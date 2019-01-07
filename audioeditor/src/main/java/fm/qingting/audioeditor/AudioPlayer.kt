package fm.qingting.audioeditor

import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log

class AudioPlayer {

    @Volatile
    private var mIsPlayStarted = false
    private var mAudioTrack: AudioTrack? = null

    @JvmOverloads
    fun startPlayer(
        streamType: Int = AudioManager.STREAM_MUSIC,
        sampleRateInHz: Int = IAudioRecorder.DEFAULT_SAMPLE_RATE,
        channelConfig: Int = IAudioRecorder.DEFAULT_CHANNEL_OUT_CONFIG,
        audioFormat: Int = IAudioRecorder.DEFAULT_AUDIO_FORMAT
    ): Boolean {
        if (mIsPlayStarted) {
            Log.e(TAG, "Player already started !")
            return false
        }

        Log.i(
            TAG,
            "getMinBufferSize = " + IAudioRecorder.MIN_OUT_BUFFER_SIZE + " bytes !"
        )

        mAudioTrack = AudioTrack(
            streamType, sampleRateInHz, channelConfig, audioFormat,
            IAudioRecorder.MIN_OUT_BUFFER_SIZE, DEFAULT_PLAY_MODE
        )
        if (mAudioTrack!!.state == AudioTrack.STATE_UNINITIALIZED) {
            Log.e(TAG, "AudioTrack initialize fail !")
            return false
        }

        mIsPlayStarted = true

        Log.i(TAG, "Start audio player success !")

        return true
    }

    fun stopPlayer() {
        if (!mIsPlayStarted) {
            return
        }
        mAudioTrack?.let {
            if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                it.stop()
            }
            it.release()
            mAudioTrack = null
        }
        mIsPlayStarted = false

        Log.i(TAG, "Stop audio player success !")
    }

    fun play(audioData: ByteArray, offsetInBytes: Int, sizeInBytes: Int): Boolean {
        if (!mIsPlayStarted || mAudioTrack == null) {
            Log.e(TAG, "Player not started !")
            return false
        }

        if (mAudioTrack!!.write(audioData, offsetInBytes, sizeInBytes) != sizeInBytes) {
            Log.e(TAG, "Could not write all the samples to the audio device !")
        }

        mAudioTrack!!.play()

        Log.d(TAG, "OK, Played $sizeInBytes bytes !")

        return true
    }

    companion object {

        private const val TAG = "AudioPlayer"

        private const val DEFAULT_PLAY_MODE = AudioTrack.MODE_STREAM
    }
}
