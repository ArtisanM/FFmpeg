package fm.qingting.audioeditor

import android.media.AudioFormat
import android.media.MediaRecorder
import fm.qingting.audioeditor.IAudioRecorder.Companion.MIN_OUT_BUFFER_SIZE
import java.io.File
import java.io.IOException
import java.util.*

interface IAudioRecorder {

    companion object {
        const val TAG = "IAudioRecorder"
        const val DEFAULT_SOURCE = MediaRecorder.AudioSource.MIC
        const val DEFAULT_SAMPLE_RATE = 44100 // 兼容性最好
        const val DEFAULT_CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val DEFAULT_CHANNEL_OUT_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        const val DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        val MIN_IN_BUFFER_SIZE = android.media.AudioTrack.getMinBufferSize(
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

    fun addAudioTrack(file: File): Track
}

data class Track(val file: File) {

    private var mAudioPlayer: AudioPlayer? = null
    private var mWavFileReader: WavFileReader? = null
    @Volatile
    private var mIsPlaying = false
    private var mVolume: Float = 1f

    fun play() {
        mAudioPlayer?.let {
            it.stopPlayer()
            mAudioPlayer = null
        }
        mAudioPlayer = AudioPlayer()
        mAudioPlayer?.startPlayer()

        mWavFileReader = WavFileReader()
        mWavFileReader?.openFile(file.absolutePath)

        mIsPlaying = true

        Thread(Runnable {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            val buffer = ByteArray(MIN_OUT_BUFFER_SIZE)
            while (mIsPlaying && mWavFileReader?.readData(buffer, 0, buffer.size)!! > 0) {
                AudioUtils.adjustVolume(buffer, mVolume)
                mAudioPlayer?.play(buffer, 0, buffer.size)
                Arrays.fill(buffer, 0)
            }
            mAudioPlayer?.stopPlayer()
            mAudioPlayer = null
            mIsPlaying = false
            try {
                mWavFileReader?.closeFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            mWavFileReader = null
        }).start()
    }

    fun stop() {
        mIsPlaying = false
        mAudioPlayer?.stopPlayer()
        mAudioPlayer = null
        try {
            mWavFileReader?.closeFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        mWavFileReader = null
    }

    fun setVolume(volume: Float) {
        mVolume = if (volume > 2) 2f else (if (volume < 0) 0f else volume)
    }
}


enum class RecordState {
    INITIALIZED, RECORDING, PAUSED
}