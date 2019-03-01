package fm.qingting.audioeditor

import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

class AudioRecorderImpl : IAudioRecorder {


    companion object {
        private const val TAG = "AudioRecorder"
    }

    private var mAudioRecord: AudioRecord? = null
    private var mWorkThread = Executors.newSingleThreadExecutor()
    @Volatile
    private var mIsCaptureStarted = false
    private var mOutputStream: FileOutputStream? = null
    private var mOnAudioRecordListener: OnAudioRecordListener? = null
    private var mOutputFile: File = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "qt_record_${System.currentTimeMillis()}.m4a"
    )
    private var mOutputTempFile: File = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "qt_record_${System.currentTimeMillis()}_temp.pcm"
    )
    private val mTracks = CopyOnWriteArrayList<Track>()
    private var mBuffer = ByteArray(IAudioRecorder.MIN_IN_BUFFER_SIZE)

    override fun isRecording(): Boolean = mIsCaptureStarted

    override fun pauseRecord() {
        mAudioRecord?.let {
            if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                it.stop()
//                mIsCaptureStarted = false
            }
        }
    }

    override fun resumeRecord() {
        mAudioRecord?.let {
            if (it.recordingState == AudioRecord.RECORDSTATE_STOPPED) {
                it.startRecording()
                mIsCaptureStarted = true

                if (mOutputStream == null) {
                    mOutputStream = FileOutputStream(mOutputTempFile)
                }

                mWorkThread.execute(AudioCaptureRunnable())
            }
        }
    }

    override fun startRecord() {
        mAudioRecord = AudioRecord(
            IAudioRecorder.DEFAULT_SOURCE,
            IAudioRecorder.DEFAULT_SAMPLE_RATE,
            IAudioRecorder.DEFAULT_CHANNEL_IN_CONFIG,
            IAudioRecorder.DEFAULT_AUDIO_FORMAT,
            IAudioRecorder.MIN_IN_BUFFER_SIZE
        )
        if (mAudioRecord!!.state == AudioRecord.STATE_UNINITIALIZED) {
            Log.e(TAG, "AudioRecord initialize fail !")
            return
        }
        if (mOutputFile.exists()) {
            mOutputFile.delete()
        }
        if (mOutputTempFile.exists()) {
            mOutputTempFile.delete()
        }

        mOutputStream?.close()
        mOutputStream = FileOutputStream(mOutputTempFile)
        mAudioRecord!!.startRecording()

        mIsCaptureStarted = true

        mWorkThread.execute(AudioCaptureRunnable())

        Log.d(TAG, "Start audio capture success !")
    }

    override fun stopRecord() {
        if (!mIsCaptureStarted) {
            return
        }

        mIsCaptureStarted = false

        mAudioRecord?.let {
            if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                it.stop()
            }
            it.release()
            mAudioRecord = null
        }

        mOutputStream?.close()
        mOutputStream = null

        Log.d(TAG, "Stop audio capture success !")

    }

    override fun toggle() {
        if (mAudioRecord != null) {
            if (mIsCaptureStarted) {
                pauseRecord()
            } else {
                resumeRecord()
            }
        } else {
            startRecord()
        }
    }

    override fun addAudioTrack(file: File): ITrack {
        return Track(file).also {
            mTracks.add(it)
        }
    }

    override fun removeAudioTrack(file: File) {
        for (i in mTracks.indices) {
            if (mTracks[i].file == file) {
                mTracks[i].apply {
                    stopPlay()
                    release()
                }
                mTracks.removeAt(i)
            }
        }
    }

    override fun getAudio(): Observable<File> {
        return Observable.create<File> {
            if (mAudioRecord != null && mAudioRecord!!.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                throw IllegalStateException("can not get audio while recording !")
            }
            mOutputStream?.close()
            mOutputStream = null

            if (!mOutputTempFile.exists()) {
                // 已经合成过，暂无未合成数据
                it.onNext(mOutputFile)
                it.onComplete()
                return@create
            }

            val output = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "qt_record_${System.currentTimeMillis()}_temp.m4a"
            )
            if (mOutputFile.exists()) {
                FFmpegUtil.concatAudio(output, object : FFmpegCmd.OnCmdExecListener {
                    override fun onSuccess() {
                        mOutputFile.delete()
                        output.renameTo(mOutputFile)
                        mOutputTempFile.delete()
                        it.onNext(mOutputFile)
                        it.onComplete()
                    }

                    override fun onFailure() {
                        it.onError(IOException("concat audio failed"))
                    }

                    override fun onProgress(progress: Float) {
                    }

                }, mOutputFile, mOutputTempFile)
            } else {
                FFmpegUtil.pcm2other(mOutputTempFile, output, object : FFmpegCmd.OnCmdExecListener {
                    override fun onSuccess() {
                        mOutputFile.delete()
                        output.renameTo(mOutputFile)
                        mOutputTempFile.delete()
                        it.onNext(mOutputFile)
                        it.onComplete()
                    }

                    override fun onFailure() {
                        it.onError(IOException("concat audio failed"))
                    }

                    override fun onProgress(progress: Float) {
                    }

                })
            }
        }.subscribeOn(Schedulers.io())
    }

    override fun setOutputFile(file: File) {
        mOutputFile = file
        if (mOutputFile.exists()) {
            mOutputFile.delete()
        }
    }

    override fun setOnAudioRecordListener(listener: OnAudioRecordListener) {
        mOnAudioRecordListener = listener
    }

    override fun release() {
        mIsCaptureStarted = false
        mOnAudioRecordListener = null
        mWorkThread.shutdown()
        mOutputStream?.close()
        mOutputStream = null
        mAudioRecord?.let {
            if (it.state != AudioRecord.STATE_UNINITIALIZED) {
                it.release()
            }
        }
        mAudioRecord = null
        mOutputTempFile.delete()
    }


    private inner class AudioCaptureRunnable : Runnable {

        private var buffers = Array(1) { mBuffer }
        private var ret = IntArray(1)
        private var volumes = FloatArray(1){ 1f}
        private var tracks = mutableListOf<Track>()

        override fun run() {

            var finalRet: Int
            val mixer = AudioAverageMixer()
            var readed: AudioData

            while (mIsCaptureStarted) {
                var capturedData = false

                // 后续只用tracks不用mTracks，避免多线程的问题
                tracks.clear()
                mTracks.forEach { tracks.add(it) }

                ensureArraysWithSameSize()

                // read from recorder
                mAudioRecord?.let {
                    ret[0] = it.read(buffers[0], 0, buffers[0].size)
                    if (ret[0] > 0) {
                        capturedData = true
                    }
                }

                // read from tracks
                for (index in tracks.indices) {
                    readed = tracks[index].readAudioData()
                    buffers[index + 1] = readed.buffer
                    ret[index + 1] = readed.len
                    volumes[index + 1] = tracks[index].getVolume()

                    if (readed.len > 0) {
                        capturedData = true
                        //todo 判断是否需要play
                        tracks[index].playAudioData(readed.buffer, 0, readed.len)
                    }
                }

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Captured ${ret.toList()} bytes !")
                }

                if (capturedData) {
                    finalRet = mixer.mix(buffers, ret, volumes)
                    mOnAudioRecordListener?.onAudioFrameCaptured(buffers[0], finalRet)
                    mOutputStream?.write(buffers[0], 0, finalRet)
                } else {
                    //todo 音轨 麦克风 都没拿到音频数据，则暂停录音（是否需要停止？待定）
                    mIsCaptureStarted = false
                }

                if (ret[0] <= 0) {
                    // 录音关闭 处于只录音轨的状态，所以要block；录音开启的时候recorder会block所以不需要sleep
                    Thread.sleep(IAudioRecorder.BUFFER_TIME_UNIT_MILL, IAudioRecorder.BUFFER_TIME_UNIT_NANO)
                }
            }
        }

        /**
         * 确保buffer/ret/volumes长度与tracks一致
         */
        private fun ensureArraysWithSameSize() {
            if (buffers.size != tracks.size + 1) {
                buffers = Array(tracks.size + 1) { mBuffer }
                buffers[0] = mBuffer
            }

            if (ret.size != tracks.size + 1) {
                ret = IntArray(tracks.size + 1)
            }

            if (volumes.size != tracks.size + 1) {
                volumes = FloatArray(tracks.size + 1){ 1f}
            }
        }
    }


    private inner class Track(val file: File) : HandlerThread("QtTrack:${file.name}"), ITrack {

        @Volatile
        private var mIsPlaying = false
        @Volatile
        private var mIsLoop = false
        private var started = false
        @Volatile
        private var mVolume = 1f
        private val mAudioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            IAudioRecorder.DEFAULT_SAMPLE_RATE,
            IAudioRecorder.DEFAULT_CHANNEL_OUT_CONFIG,
            IAudioRecorder.DEFAULT_AUDIO_FORMAT,
            IAudioRecorder.MIN_OUT_BUFFER_SIZE,
            AudioTrack.MODE_STREAM
        )
        private val mBuffer = ByteArray(IAudioRecorder.MIN_IN_BUFFER_SIZE)
        private val mStream = RandomAccessFile(file, "r")
        private val mReadBuffer = AudioData(mBuffer, 0)
        private var mHandler: Handler? = null
        private val mPlayBufferPool = ConcurrentLinkedQueue<ByteArray>()

        override fun onLooperPrepared() {
            super.onLooperPrepared()
            mHandler = Handler(looper, Handler.Callback { msg ->
                val buffer = msg.obj as ByteArray
                val len = msg.arg1
                mAudioTrack.write(buffer, 0, len)

                mPlayBufferPool.offer(buffer)
                return@Callback true
            })
        }

        override fun stopPlay() {
            mIsPlaying = false
            mAudioTrack.pause()
            mAudioTrack.stop()
            mAudioTrack.flush()

            //todo 若录音已暂停，并且无其他音轨在录，就设置recording=false
        }

        override fun startPlay() {
            if (!started) {
                start()
                started = true
            }
            mAudioTrack.flush()
            mAudioTrack.play()
            mIsPlaying = true

            //todo 若还未开始录音，则启动录音
            if (!isRecording()) {

            }
        }

        override fun playAudioData(audioData: ByteArray, offsetInBytes: Int, sizeInBytes: Int) {
            // 因为audioTrack的write是阻塞的（非阻塞要求API23），而read又在别的线程执行，所以play与read不能用同一个buffer
            val buffer = getBuffer()
            System.arraycopy(audioData, offsetInBytes, buffer, offsetInBytes, sizeInBytes)
            mHandler?.sendMessage(Message.obtain().also { msg ->
                msg.obj = buffer
                msg.arg1 = sizeInBytes
            })
        }

        override fun readAudioData(): AudioData {
            if (isPlaying()) {
                mReadBuffer.len = mStream.read(mBuffer, 0, mBuffer.size)
            } else {
                mReadBuffer.len = 0
            }
            mReadBuffer.buffer = mBuffer
            return mReadBuffer
        }

        override fun isPlaying(): Boolean = mIsPlaying

        override fun setIsLoop(isLoop: Boolean) {
            mIsLoop = isLoop
        }

        override fun isLoop(): Boolean = mIsLoop

        override fun getVolume(): Float = mVolume

        override fun setVolume(volume: Float) {
            mVolume = if (volume < 0) 0f else (if (volume > 10) 10f else volume)
        }

        private fun getBuffer(): ByteArray {
            var buffer = mPlayBufferPool.poll()
            if (buffer == null) {
                buffer = ByteArray(IAudioRecorder.MIN_IN_BUFFER_SIZE)
            }
            return buffer
        }

        override fun release() {
            mAudioTrack.pause()
            mAudioTrack.stop()
            mAudioTrack.flush()
            mAudioTrack.release()
            quit()
        }
    }
}
