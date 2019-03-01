package fm.qingting.audioeditor

import android.media.AudioFormat
import android.media.MediaRecorder
import io.reactivex.Observable
import java.io.File

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
        // 录音时每个buff时长(单位nano) = BUFFER_TIME_UNIT_MILL * 1000000 + BUFFER_TIME_UNIT_NANO
        val BUFFER_TIME_UNIT_MILL = 1000L * MIN_IN_BUFFER_SIZE / 2 / IAudioRecorder.DEFAULT_SAMPLE_RATE;
        val BUFFER_TIME_UNIT_NANO =
            ((1000 * 1000 * 1000L * MIN_IN_BUFFER_SIZE / 2 / IAudioRecorder.DEFAULT_SAMPLE_RATE) - BUFFER_TIME_UNIT_MILL * 1000 * 1000L).toInt()
    }

    fun isRecording(): Boolean

    fun pauseRecord()

    fun resumeRecord()

    fun startRecord()

    fun stopRecord()

    fun setOutputFile(file: File)

    /**
     * 根据自身状态自动 开始/暂停/继续
     */
    fun toggle()

    /**
     * release前确保已调用getAudio()完成音频合成及编码
     */
    fun release()

    /**
     * 获取音频
     */
    fun getAudio(): Observable<File>

    fun setOnAudioRecordListener(listener: OnAudioRecordListener)

    fun addAudioTrack(file: File): ITrack

    fun removeAudioTrack(file: File)
}

interface OnAudioRecordListener {
    fun onAudioFrameCaptured(audioData: ByteArray, readSize: Int)
}

/**
 * v2.0 音轨
 * 音轨格式必须为 44100Hz, pcm s16le, mono
 */
interface ITrack {

    fun stopPlay()

    fun startPlay()

    fun playAudioData(audioData: ByteArray, offsetInBytes: Int, sizeInBytes: Int)

    fun readAudioData(): AudioData

    fun isPlaying():Boolean

    fun setIsLoop(isLoop: Boolean)

    fun isLoop(): Boolean

    fun getVolume(): Float

    fun setVolume(volume: Float)

    fun release()
}

data class AudioData(var buffer: ByteArray, var len: Int)