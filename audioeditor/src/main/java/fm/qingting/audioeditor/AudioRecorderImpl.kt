package fm.qingting.audioeditor

import android.media.AudioRecord
import android.os.Environment
import android.util.Log
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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
    private var mOutputFile: File = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "qt_record_${System.currentTimeMillis()}.m4a")
    private var mOutputTempFile: File = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "qt_record_${System.currentTimeMillis()}_temp.pcm")

    override fun isRecording(): Boolean = mIsCaptureStarted

    override fun pauseRecord() {
        mAudioRecord?.let {
            if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                it.stop()
                mIsCaptureStarted = false
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
        mAudioRecord = AudioRecord(IAudioRecorder.DEFAULT_SOURCE, IAudioRecorder.DEFAULT_SAMPLE_RATE, IAudioRecorder.DEFAULT_CHANNEL_IN_CONFIG, IAudioRecorder.DEFAULT_AUDIO_FORMAT, IAudioRecorder.MIN_IN_BUFFER_SIZE)
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

        override fun run() {
            val buffer = ByteArray(IAudioRecorder.MIN_IN_BUFFER_SIZE)
            while (mIsCaptureStarted) {
                mAudioRecord?.let {
                    val ret = it.read(buffer, 0, buffer.size)
                    Log.d(TAG, "Captured $ret bytes !")
                    when (ret) {
                        AudioRecord.ERROR_INVALID_OPERATION -> Log.e(TAG, "Error ERROR_INVALID_OPERATION")
                        AudioRecord.ERROR_BAD_VALUE -> Log.e(TAG, "Error ERROR_BAD_VALUE")
                        else -> {
                            if (ret > 0) {
                                mOnAudioRecordListener?.onAudioFrameCaptured(buffer, ret)
                                mOutputStream?.write(buffer, 0, ret)
                            }
                        }
                    }
                    return@let
                }
            }
        }
    }
}