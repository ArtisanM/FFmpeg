package fm.qingting.audioeditor

import android.media.AudioManager
import android.media.AudioTrack
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.util.concurrent.Executors

class PcmPlayer : IAudioPlayer {

    private inner class PlayerRunnable : Runnable {

        override fun run() {
            var ret = 0
            while (mKeepPlaying && { ret = mInputStream?.read(mBuffer) ?: -1 ; ret}() != -1) {
                mInputStream?.apply {
                    mAudioTrack.write(mBuffer, 0, ret)
                    mPosition += ret
                }
            }
            if (ret == -1) {
                stop()
                mOnCompletionListener?.onCompletion(this@PcmPlayer)
            }
        }
    }

    private val mAudioTrack: AudioTrack = AudioTrack(
        AudioManager.STREAM_MUSIC, IAudioRecorder.DEFAULT_SAMPLE_RATE, IAudioRecorder.DEFAULT_CHANNEL_OUT_CONFIG, IAudioRecorder.DEFAULT_AUDIO_FORMAT,
        IAudioRecorder.MIN_OUT_BUFFER_SIZE,  AudioTrack.MODE_STREAM
    )

    @Volatile
    private var mKeepPlaying = false
    @Volatile
    private var mPosition = 0L

    private var mWorkThread = Executors.newSingleThreadExecutor()
    private var mFile: File? = null
    private val mBuffer = ByteArray(IAudioRecorder.MIN_OUT_BUFFER_SIZE)
    private var mInputStream: RandomAccessFile? = null
    private var mOnCompletionListener: IAudioPlayer.OnCompletionListener? = null


    override fun reset() {
        if (isPlaying()) {
            stop()
        }
        mFile = null
        mInputStream?.close()
        mInputStream = null
    }

    override fun pause() {
        if (isPlaying()) {
            mKeepPlaying = false
            mAudioTrack.pause()
        }
    }

    override fun start() {
        if (isPlaying()) {
            return
        }
        if (mAudioTrack.state == AudioTrack.STATE_UNINITIALIZED) {
            throw IllegalStateException("AudioTrack initialize fail !")
        }
        if (mFile == null) {
            throw IllegalStateException("dataSource not set !")
        }
        if (mInputStream == null) {
            mInputStream = RandomAccessFile(mFile!!, "r")
        }
        mKeepPlaying = true
        mAudioTrack.flush()
        mAudioTrack.play()
        mWorkThread.execute(PlayerRunnable())
    }

    override fun stop() {
        if (isPlaying() || isPaused()) {
            mKeepPlaying = false
            mPosition = 0L
            mAudioTrack.pause()
            mAudioTrack.stop()
            mAudioTrack.flush()
            mInputStream?.seek(0)
        }
    }

    override fun release() {
        stop()
        mFile = null
        mAudioTrack.release()
        mWorkThread.shutdownNow()
    }

    override fun isPlaying(): Boolean = mAudioTrack.playState == AudioTrack.PLAYSTATE_PLAYING

    override fun isPaused(): Boolean = mAudioTrack.playState == AudioTrack.PLAYSTATE_PAUSED

    override fun getCurrentPosition(): Long = (1000 * mPosition / (IAudioRecorder.DEFAULT_SAMPLE_RATE * 1f * 16 / 8)).toLong()

    override fun getDuration(): Long {
        return if (mFile != null) {
            (1000 * mFile!!.length() / (IAudioRecorder.DEFAULT_SAMPLE_RATE * 1f * 16 / 8)).toLong()
        } else {
            0L
        }
    }

    override fun seekTo(pos: Long) {
        if (mAudioTrack.state == AudioTrack.STATE_UNINITIALIZED) {
            throw IllegalStateException("AudioTrack initialize fail !")
        }
        if (mFile == null) {
            throw IllegalStateException("dataSource not set !")
        }
        val wasPlaying = isPlaying()
        stop()
        mPosition = (pos * (IAudioRecorder.DEFAULT_SAMPLE_RATE * 1f * 16 / 8) / 1000f).toLong()
        if (mPosition >= mFile!!.length()) {
            mPosition = 0L
            return
        }
        // 2 bytes = 1 sample, 这里必须注意！否则当postion为单数，会导致播放器解析的音频数据错乱，播放出来的声音不对
        if (mPosition % 2 != 0L) {
            mPosition++
        }
        if (mInputStream == null) {
            mInputStream = RandomAccessFile(mFile!!, "r")
        }
        mInputStream?.seek(mPosition)
        if (wasPlaying) {
            start()
        }
    }

    @Throws(FileNotFoundException::class, IllegalStateException::class)
    override fun setDataSource(path: String) {
        if (mAudioTrack.state == AudioTrack.STATE_UNINITIALIZED) {
            throw IllegalStateException("AudioTrack initialize fail !")
        }
        val file = File(path)
        if (!file.exists()) {
            throw FileNotFoundException()
        }
        mFile = file
    }

    override fun setOnCompletionListener(listener: IAudioPlayer.OnCompletionListener) {
        mOnCompletionListener = listener
    }
}