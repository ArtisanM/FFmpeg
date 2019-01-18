package fm.qingting.audioeditor

import android.media.MediaMetadataRetriever
import java.io.File

object FFmpegUtil {

    /**
     * 转码
     */
    fun convert(srcFile: File, outputFile: File, listener: FFmpegCmd.OnCmdExecListener) {
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

        val cmd = CmdList()
        cmd.add("-i")
        cmd.add(srcFile.absolutePath)
        cmd.add("-vn")
        cmd.add("-vsync")
        cmd.add("2")
        cmd.add(outputFile.absolutePath)

        FFmpegCmd.exec(cmd.toTypedArray(), duration, listener)
    }

    /**
     * startTime: ms
     * duration: ms
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
        val cmd = CmdList()
        cmd.add("-i")
        cmd.add(srcFile.absolutePath)
        cmd.add("-ss")
        cmd.add(formatElapsedTime(startTime))
        cmd.add("-t")
        cmd.add(formatElapsedTime(duration))
        cmd.add("-vn")
        cmd.add("-vsync")
        cmd.add("2")
        cmd.add(outputFile.absolutePath)

        FFmpegCmd.exec(cmd.toTypedArray(), duration, listener)
    }

    /**
     * 裁剪文件，要求输入文件与输出文件格式相同
     * 加入-c copy参数，省略了codec过程，速度更快
     * startTime: ms
     * duration: ms
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

        val cmd = CmdList()
        cmd.add("-i")
        cmd.add(srcFile.absolutePath)
        cmd.add("-ss")
        cmd.add(formatElapsedTime(startTime))
        cmd.add("-t")
        cmd.add(formatElapsedTime(duration))
        cmd.add("-vn")
        cmd.add("-vsync")
        cmd.add("2")
        cmd.add("-c")
        cmd.add("copy")
        cmd.add(outputFile.absolutePath)

        FFmpegCmd.exec(cmd.toTypedArray(), duration, listener)
    }

    /**
     * 裁剪文件from 0 to endTime，要求输入文件与输出文件格式相同
     * 加入-c copy参数，省略了codec过程，速度更快
     * startTime: ms
     */
    fun cutAudioWithSameFormat(
        srcFile: File,
        outputFile: File,
        endTime: Long,
        listener: FFmpegCmd.OnCmdExecListener
    ) {
        if (outputFile.exists()) {
            outputFile.delete()
        }
        if (!srcFile.exists()) {
            listener.onFailure()
            return
        }

        val cmd = CmdList()
        cmd.add("-i")
        cmd.add(srcFile.absolutePath)
        cmd.add("-to")
        cmd.add(formatElapsedTime(endTime))
        cmd.add("-vn")
        cmd.add("-vsync")
        cmd.add("2")
        cmd.add("-c")
        cmd.add("copy")
        cmd.add(outputFile.absolutePath)

        FFmpegCmd.exec(cmd.toTypedArray(), endTime, listener)
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

        val cmd = CmdList()
        cmd.add("-i")
        cmd.add(srcFile.absolutePath)
        cmd.add("-ar")
        cmd.add("44100")
        cmd.add("-ac")
        cmd.add("1")
        cmd.add("-acodec")
        cmd.add("pcm_s16le")
        cmd.add("-vn")
        cmd.add("-vsync")
        cmd.add("2")
        cmd.add(outputFile.absolutePath)

        FFmpegCmd.exec(cmd.toTypedArray(), duration, listener)
    }

    /**
     * resample file to 44100 kHz, mono, PCM_16BIT
     */
    fun resample2Pcm(srcFile: File, outputFile: File, listener: FFmpegCmd.OnCmdExecListener) {
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

        val cmd = CmdList()
        cmd.add("-i")
        cmd.add(srcFile.absolutePath)
        cmd.add("-f")
        cmd.add("s16le")
        cmd.add("-ar")
        cmd.add("44100")
        cmd.add("-ac")
        cmd.add("1")
        cmd.add("-acodec")
        cmd.add("pcm_s16le")
        cmd.add("-vn")
        cmd.add("-vsync")
        cmd.add("2")
        cmd.add(outputFile.absolutePath)

        FFmpegCmd.exec(cmd.toTypedArray(), duration, listener)
    }

    fun pcm2other(srcFile: File, outputFile: File, listener: FFmpegCmd.OnCmdExecListener) {
        if (outputFile.exists()) {
            outputFile.delete()
        }
        if (!srcFile.exists()) {
            listener.onFailure()
            return
        }
        val cmd = CmdList()
        cmd.add("-f")
        cmd.add("s16le")
        cmd.add("-ar")
        cmd.add("44100")
        cmd.add("-ac")
        cmd.add("1")
        cmd.add("-i")
        cmd.add(srcFile.absolutePath)
        cmd.add(outputFile.absolutePath)

        FFmpegCmd.exec(cmd.toTypedArray(), 1000 * srcFile.length() / (IAudioRecorder.DEFAULT_SAMPLE_RATE * 1 * 16 / 8), listener)
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
        val cmd = CmdList()
        cmd.add("-i")
        cmd.add(srcFile.absolutePath)
        cmd.add("-i")
        cmd.add(srcFile1.absolutePath)
        cmd.add("-vsync")
        cmd.add("2")
        cmd.add("-vn")
        cmd.add("-filter_complex")
        cmd.add("amix=inputs=2:duration=longest")
        cmd.add("-strict")
        cmd.add("-2")
        cmd.add(outputFile.absolutePath)

        FFmpegCmd.exec(cmd.toTypedArray(), 0, listener)
    }


    fun concatAudio(outputFile: File, listener: FFmpegCmd.OnCmdExecListener, vararg srcFile: File) {
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val mediaMetadataRetriever = MediaMetadataRetriever()
        var outputFileDuration = 0L

        val cmd = CmdList()
        val sb = java.lang.StringBuilder()
        for ((index, file) in srcFile.withIndex()) {
            if (!file.exists()) {
                listener.onFailure()
                return
            }
            if (file.absolutePath.endsWith(".pcm")) {
                cmd.add("-f")
                cmd.add("s16le")
                cmd.add("-ar")
                cmd.add("44100")
                cmd.add("-ac")
                cmd.add("1")
                cmd.add("-i")
                cmd.add(file.absolutePath)
                outputFileDuration += 1000 * file.length() / (IAudioRecorder.DEFAULT_SAMPLE_RATE * 1 * 16 / 8)
            } else {
                cmd.add("-i")
                cmd.add(file.absolutePath)
                mediaMetadataRetriever.setDataSource(file.absolutePath)
                outputFileDuration += mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    .toLong()
            }
            sb.append("[$index:a]")
        }
        mediaMetadataRetriever.release()

        cmd.add("-vn")
        cmd.add("-vsync")
        cmd.add("2")
        cmd.add("-filter_complex")
        cmd.add("${sb}concat=n=${srcFile.size}:v=0:a=1[outa]")
        cmd.add("-map")
        cmd.add("[outa]")
        cmd.add(outputFile.absolutePath)

        FFmpegCmd.exec(cmd.toTypedArray(), outputFileDuration, listener)
    }

    /**
     * @param time: ms
     */
    private fun formatElapsedTime(time: Long): String {
        return String.format("%.3f", time / 1000f)
    }
}