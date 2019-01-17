package mo.example.ffmpeg.ffmpegdemo

import java.nio.ByteBuffer
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

    fun getMaxValue(arr: ByteArray, buffer: ByteBuffer): Short {
        var max: Short = 0
        var cur: Short = 0
        for (i in arr.indices step 2) {
            buffer.clear()
            buffer.put(arr[i])
            buffer.put(arr[i + 1])
            cur = buffer.getShort(0)
            if (abs(cur) > abs(max)) {
                max = cur
            }
        }
        return max
    }
}