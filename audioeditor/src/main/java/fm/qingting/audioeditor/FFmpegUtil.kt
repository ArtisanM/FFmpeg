package fm.qingting.audioeditor

import android.media.MediaMetadataRetriever
import java.io.File
import java.util.*

object FFmpegUtil {

    /**
     * startTime: ms
     * duration: s
     */
    fun cropAudio(
        srcFile: File,
        outputFile: File,
        startTime: Long,
        duration: Long,
        listener: FFmpegCmd.OnCmdExecListener
    ) {
        if (outputFile.exists()) {
            outputFile.delete()
        }
        if (!srcFile.exists()) {
            listener.onFailure()
            return
        }
        FFmpegCmd.exec(
            "ffmpeg -i ${srcFile.absolutePath} -ss ${formatElapsedTime(
                startTime
            )} -t $duration -vn -vsync 2 ${outputFile.absolutePath}".split(" ").toTypedArray(),
            duration * 1000,
            listener
        )
    }

    /**
     * 裁剪文件，要求输入文件与输出文件格式相同
     * 加入-c copy参数，省略了codec过程，速度更快
     * startTime: ms
     * duration: s
     */
    fun cropAudioWithSameFormat(
        srcFile: File,
        outputFile: File,
        startTime: Long,
        duration: Long,
        listener: FFmpegCmd.OnCmdExecListener
    ) {
        if (outputFile.exists()) {
            outputFile.delete()
        }
        if (!srcFile.exists()) {
            listener.onFailure()
            return
        }
        FFmpegCmd.exec(
            "ffmpeg -i ${srcFile.absolutePath} -ss ${formatElapsedTime(
                startTime
            )} -t $duration -vn -vsync 2 -c copy ${outputFile.absolutePath}".split(" ").toTypedArray(),
            duration * 1000,
            listener
        )
    }

    /**
     * resample file to 44100 kHz, mono, PCM_16BIT
     */
    fun resample2wav(srcFile: File, outputFile: File, listener: FFmpegCmd.OnCmdExecListener) {
        if (outputFile.exists()) {
            outputFile.delete()
        }
        if (!srcFile.exists()) {
            listener.onFailure()
            return
        }
        //get duration for calc ffmpeg progress
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(srcFile.absolutePath)
        val duration =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong() // ms
        mediaMetadataRetriever.release()

        FFmpegCmd.exec(
            "ffmpeg -i ${srcFile.absolutePath} -ar 44100 -ac 1 -acodec pcm_s16le -vn -vsync 2 ${outputFile.absolutePath}".split(
                " "
            ).toTypedArray(), duration, listener
        )
    }


    fun mixAudio(srcFile: File, srcFile1: File, outputFile: File, listener: FFmpegCmd.OnCmdExecListener) {
        if (outputFile.exists()) {
            outputFile.delete()
        }
        if (!srcFile.exists() || !srcFile1.exists()) {
            listener.onFailure()
            return
        }
        /*
         other choice:
         ffmpeg -i ${file.absolutePath} -i ${file2.absolutePath} -vsync 2 -map 0:0 -map 1:0 -filter_complex amix=inputs=2:duration=longest -strict -2 ${out.absolutePath}

            # -map 0:0 选择第一个input的第一个流输出到output的第一个流  -map 1:0 选择第二个input的第一个流输出到output的第一个流
          */
        // -vn drop video stream（include music cover）
        FFmpegCmd.exec(
            "ffmpeg -i ${srcFile.absolutePath} -i ${srcFile1.absolutePath} -vsync 2 -vn -filter_complex amix=inputs=2:duration=longest -strict -2 ${outputFile.absolutePath}".split(
                " "
            ).toTypedArray(), 0, listener
        )
    }


    fun concatAudio(outputFile: File, listener: FFmpegCmd.OnCmdExecListener, vararg srcFile: File) {
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val mediaMetadataRetriever = MediaMetadataRetriever()
        var outputFileDuration = 0L

        val sb = java.lang.StringBuilder()
        val sb2 = java.lang.StringBuilder()
        for ((index, file) in srcFile.withIndex()) {
            if (!file.exists()) {
                listener.onFailure()
                return
            }
            sb.append(" -i ${file.absolutePath}")
            sb2.append("[$index:a]")
            mediaMetadataRetriever.setDataSource(file.absolutePath)
            outputFileDuration += mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                .toLong()
        }
        mediaMetadataRetriever.release()
        FFmpegCmd.exec(
            ("ffmpeg$sb -vn -vsync 2 -filter_complex $sb2" + "concat=n=${srcFile.size}:v=0:a=1[outa] -map [outa] ${outputFile.absolutePath}").split(
                " "
            ).toTypedArray(), outputFileDuration, listener
        )
    }

    private fun formatElapsedTime(time: Long): String {
        var elapsedSeconds = time / 1000
        var hours: Long = 0
        var minutes: Long = 0
        var seconds: Long = 0
        if (elapsedSeconds >= 3600) {
            hours = elapsedSeconds / 3600
            elapsedSeconds -= hours * 3600
        }
        if (elapsedSeconds >= 60) {
            minutes = elapsedSeconds / 60
            elapsedSeconds -= minutes * 60
        }
        seconds = elapsedSeconds
        val f = Formatter(StringBuilder(8), Locale.getDefault())
        return f.format("%02d:%02d:%02d", hours, minutes, seconds).toString()
    }
}