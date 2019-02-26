package fm.qingting.audioeditor

import java.io.FileNotFoundException

interface IAudioPlayer {

    fun reset()

    fun pause()

    fun start()

    fun stop()

    fun release()

    fun isPlaying(): Boolean

    fun getCurrentPosition(): Long

    fun getDuration(): Long

    fun seekTo(pos: Long)

    fun isPaused(): Boolean

    @Throws(FileNotFoundException::class, IllegalStateException::class)
    fun setDataSource(path: String)

    fun setOnCompletionListener(listener: OnCompletionListener)

    interface OnCompletionListener{
        fun onCompletion(player: IAudioPlayer)
    }

}