package fm.qingting.audioeditor

import android.media.AudioFormat
import android.media.MediaRecorder
import io.reactivex.Observable
import java.io.File

/**
 * v1.0功能：
 * 录音（可分段）
 * 裁剪
 * 录音片段拼接（pause时与上一个录音wav拼接并存为一个wav）
 */
interface IAudioRecorder {

    companion object {
        const val DEFAULT_SOURCE = MediaRecorder.AudioSource.MIC
        const val DEFAULT_SAMPLE_RATE = 44100 // 兼容性最好
        const val DEFAULT_CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val DEFAULT_CHANNEL_OUT_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        const val DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        val MIN_IN_BUFFER_SIZE = android.media.AudioRecord.getMinBufferSize(
            DEFAULT_SAMPLE_RATE,
            DEFAULT_CHANNEL_IN_CONFIG,
            DEFAULT_AUDIO_FORMAT
        )
        val MIN_OUT_BUFFER_SIZE = android.media.AudioTrack.getMinBufferSize(
            DEFAULT_SAMPLE_RATE,
            DEFAULT_CHANNEL_OUT_CONFIG,
            DEFAULT_AUDIO_FORMAT
        )
    }

    fun isRecording(): Boolean

    fun pauseRecord()

    fun resumeRecord()

    fun startRecord()

    fun stopRecord()

    fun setOutputFile(file: File)

    /**
     * release前确保已调用getAudio()完成音频合成及编码
     */
    fun release()

    /**
     * 获取音频
     */
    fun getAudio(): Observable<File>

    fun setOnAudioRecordListener(listener: OnAudioRecordListener)

//    fun addAudioTrack(file: File): Track
}

interface OnAudioRecordListener {
    fun onAudioFrameCaptured(audioData: ByteArray)
}

/**
 * v2.0 背景音乐功能 未完成
 */
//data class Track(val file: File) {
//
//    private var mAudioPlayer: AudioPlayer? = null
//    private var mWavFileReader: WavFileReader? = null
//    @Volatile
//    private var mIsPlaying = false
//    private var mVolume: Float = 1f
//
//    fun play() {
//        mAudioPlayer?.let {
//            it.stopPlayer()
//            mAudioPlayer = null
//        }
//        mAudioPlayer = AudioPlayer()
//        mAudioPlayer?.startPlayer()
//
//        mWavFileReader = WavFileReader()
//        mWavFileReader?.openFile(file.absolutePath)
//
//        mIsPlaying = true
//
//        Thread(Runnable {
//            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
//            val buffer = ByteArray(MIN_OUT_BUFFER_SIZE)
//            while (mIsPlaying && mWavFileReader?.readData(buffer, 0, buffer.size)!! > 0) {
//                AudioUtils.adjustVolume(buffer, mVolume)
//                mAudioPlayer?.play(buffer, 0, buffer.size)
//                Arrays.fill(buffer, 0)
//            }
//            mAudioPlayer?.stopPlayer()
//            mAudioPlayer = null
//            mIsPlaying = false
//            try {
//                mWavFileReader?.closeFile()
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//            mWavFileReader = null
//        }).start()
//    }
//
//    fun stop() {
//        mIsPlaying = false
//        mAudioPlayer?.stopPlayer()
//        mAudioPlayer = null
//        try {
//            mWavFileReader?.closeFile()
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//        mWavFileReader = null
//    }
//
//    fun setVolume(volume: Float) {
//        mVolume = if (volume > 2) 2f else (if (volume < 0) 0f else volume)
//    }
//}