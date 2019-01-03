package mo.example.ffmpeg.ffmpegdemo

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import java.io.*
import java.util.*


class AudioRecorder {

    companion object {
        private const val TAG = "AudioRecorder"

        private const val DEFAULT_SOURCE = MediaRecorder.AudioSource.MIC
        private const val DEFAULT_SAMPLE_RATE = 44100 // 兼容性最好
        private const val DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO  // CHANNEL_IN_STEREO录出来的全是噪音
        private const val DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var mAudioRecord: AudioRecord? = null
    private var mMinBufferSize = 0

    private var mCaptureThread: Thread? = null
    private var mIsCaptureStarted = false
    //    private var mWavFileWriter: WavFileWriter? = null
//    private var mWavFileReader: WavFileReader? = null
    private var output: DataOutputStream? = null
    private var input: DataInputStream? = null
    private var mAudioPlayer: AudioPlayer? = null

    @Volatile
    private var mIsLoopExit = false

    private var mAudioFrameCapturedListener: OnAudioFrameCapturedListener? = null

    interface OnAudioFrameCapturedListener {
        fun onAudioFrameCaptured(audioData: ByteArray)
    }

    fun isCaptureStarted(): Boolean {
        return mIsCaptureStarted
    }

    fun setOnAudioFrameCapturedListener(listener: OnAudioFrameCapturedListener) {
        mAudioFrameCapturedListener = listener
    }

    fun startCapture(): Boolean {
        return startCapture(
            DEFAULT_SOURCE, DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_CONFIG,
            DEFAULT_AUDIO_FORMAT
        )
    }

    fun startCapture(audioSource: Int, sampleRateInHz: Int, channelConfig: Int, audioFormat: Int): Boolean {

        if (mIsCaptureStarted) {
            Log.e(TAG, "Capture already started !")
            return false
        }

        mMinBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)
        if (mMinBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "Invalid parameter !")
            return false
        }
        Log.d(TAG, "getMinBufferSize = $mMinBufferSize bytes !")

        mAudioRecord = AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, mMinBufferSize)
        if (mAudioRecord!!.state == AudioRecord.STATE_UNINITIALIZED) {
            Log.e(TAG, "AudioRecord initialize fail !")
            return false
        }

        // 录音保存为wav
//        val out = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "audio.wav")
//        out.delete()
//        mWavFileWriter = WavFileWriter()
//        mWavFileWriter?.openFile(out.absolutePath, DEFAULT_SAMPLE_RATE,1,16)

        // 录音存为pcm
        output?.close()
        output = null
        val out = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "audio.pcm")
        out.delete()
        output = DataOutputStream(FileOutputStream(out))

        mAudioRecord!!.startRecording()

        mIsLoopExit = false
        mCaptureThread = Thread(AudioCaptureRunnable())
        mCaptureThread!!.start()

        mIsCaptureStarted = true

        Log.d(TAG, "Start audio capture success !")

        return true
    }

    fun stopCapture() {

        if (!mIsCaptureStarted) {
            return
        }

        mIsLoopExit = true
        try {
            mCaptureThread!!.interrupt()
            mCaptureThread!!.join(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        if (mAudioRecord!!.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            mAudioRecord!!.stop()
        }

        mAudioRecord!!.release()

        mIsCaptureStarted = false
        mAudioFrameCapturedListener = null

//        mWavFileWriter?.closeFile()
        output?.flush()
        output?.close()
        output = null

        Log.d(TAG, "Stop audio capture success !")


        Log.d(TAG, "Start play captured audio !")
//        mWavFileReader = WavFileReader()
//        mWavFileReader?.openFile(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "audio.wav").absolutePath)
        input = DataInputStream(FileInputStream(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "audio.pcm").absolutePath))

        mAudioPlayer = AudioPlayer()
        mAudioPlayer?.startPlayer()
        Thread(Runnable {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            val buffer = ByteArray(mMinBufferSize)
//            while (mWavFileReader?.readData(buffer, 0, buffer.size)!! > 0) {
//                mAudioPlayer?.play(buffer, 0, buffer.size)
//                Arrays.fill(buffer, 0)
//            }
//            mAudioPlayer?.stopPlayer()
//            try {
//                mWavFileReader?.closeFile()
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
            while (input?.read(buffer, 0, buffer.size)!! > 0) {
                mAudioPlayer?.play(buffer, 0, buffer.size)
                Arrays.fill(buffer, 0)
            }
            mAudioPlayer?.stopPlayer()
            try {
                input?.close()
                input = null
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }).start()
    }


    private inner class AudioCaptureRunnable : Runnable {

        override fun run() {
            val buffer = ByteArray(mMinBufferSize)
            while (!mIsLoopExit) {
                val ret = mAudioRecord!!.read(buffer, 0, mMinBufferSize)
                when (ret) {
                    AudioRecord.ERROR_INVALID_OPERATION -> Log.e(TAG, "Error ERROR_INVALID_OPERATION")
                    AudioRecord.ERROR_BAD_VALUE -> Log.e(TAG, "Error ERROR_BAD_VALUE")
                    else -> {
                        if (mAudioFrameCapturedListener != null) {
                            mAudioFrameCapturedListener!!.onAudioFrameCaptured(buffer)
                        }
//                        //写入wav
//                        mWavFileWriter?.writeData(buffer, 0, buffer.size)
                        output?.write(buffer, 0, buffer.size)
                        Log.d(TAG, "OK, Captured $ret bytes !")
                    }
                }
                Arrays.fill(buffer, 0)
            }
        }
    }
}