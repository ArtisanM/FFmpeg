package mo.example.ffmpeg.ffmpegdemo

import java.util.*

object Util {

    fun abs(a: Short): Short {
        return if (a > 0) a else (-a).toShort()
    }

    fun formatElapsedTimeMH(time: Long): String {
        var elapsedSeconds = time / 1000
        var minutes: Long = 0
        var seconds: Long = 0
        if (elapsedSeconds >= 60) {
            minutes = elapsedSeconds / 60
            elapsedSeconds -= minutes * 60
        }
        seconds = elapsedSeconds
        val f = Formatter(StringBuilder(8), Locale.getDefault())
        return f.format("%02d:%02d", minutes, seconds).toString()
    }
}