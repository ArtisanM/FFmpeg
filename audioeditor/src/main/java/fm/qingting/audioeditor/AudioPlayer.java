package fm.qingting.audioeditor;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class AudioPlayer {

    private static final String TAG = "AudioPlayer";

    private static final int DEFAULT_PLAY_MODE = AudioTrack.MODE_STREAM;

    private volatile boolean mIsPlayStarted = false;
    private AudioTrack mAudioTrack;

    public boolean startPlayer() {
        return startPlayer(AudioManager.STREAM_MUSIC, IAudioRecorder.DEFAULT_SAMPLE_RATE,
                IAudioRecorder.DEFAULT_CHANNEL_OUT_CONFIG, IAudioRecorder.DEFAULT_AUDIO_FORMAT);
    }

    public boolean startPlayer(int streamType, int sampleRateInHz, int channelConfig,
                               int audioFormat) {
        if (mIsPlayStarted) {
            Log.e(TAG, "Player already started !");
            return false;
        }

        Log.i(TAG,
                "getMinBufferSize = " + IAudioRecorder.Companion.getMIN_OUT_BUFFER_SIZE() + " bytes !");

        mAudioTrack = new AudioTrack(streamType, sampleRateInHz, channelConfig, audioFormat,
                IAudioRecorder.Companion.getMIN_OUT_BUFFER_SIZE(), DEFAULT_PLAY_MODE);
        if (mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            Log.e(TAG, "AudioTrack initialize fail !");
            return false;
        }

        mIsPlayStarted = true;

        Log.i(TAG, "Start audio player success !");

        return true;
    }

    public void stopPlayer() {
        if (!mIsPlayStarted) {
            return;
        }

        if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            mAudioTrack.stop();
        }

        mAudioTrack.release();
        mIsPlayStarted = false;

        Log.i(TAG, "Stop audio player success !");
    }

    public boolean play(byte[] audioData, int offsetInBytes, int sizeInBytes) {
        if (!mIsPlayStarted) {
            Log.e(TAG, "Player not started !");
            return false;
        }

        if (mAudioTrack.write(audioData, offsetInBytes, sizeInBytes) != sizeInBytes) {
            Log.e(TAG, "Could not write all the samples to the audio device !");
        }

        mAudioTrack.play();

        Log.d(TAG, "OK, Played " + sizeInBytes + " bytes !");

        return true;
    }
}
