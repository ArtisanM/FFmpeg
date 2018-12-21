package mo.example.ffmpeg.ffmpegdemo

import android.Manifest
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import java.io.File

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),100)

        // Example of a call to a native method
//        sample_text.text = avfilterInfo()

    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty()) {
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "forhim.m4a")
                val file2 = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "music.mp3")

                val out = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "mix.m4a")
                out.delete()

                val mediaMetadataRetriever = MediaMetadataRetriever()
                mediaMetadataRetriever.setDataSource(file.absolutePath)
                val duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
                mediaMetadataRetriever.release()
                Log.e("FFmpeg_VideoEditor", "duration $duration")

                val listener = object : FFmpegCmd.OnCmdExecListener {
                    override fun onSuccess() {
                        Log.e("FFmpeg_VideoEditor", "ffmpeg cmd exec success ${out.exists()}")
                    }

                    override fun onFailure() {
                        Log.e("FFmpeg_VideoEditor", "ffmpeg cmd exec failed")
                    }

                    override fun onProgress(progress: Float) {
                        Log.e("FFmpeg_VideoEditor", "ffmpeg cmd exec onProgress $progress")
                    }

                }

                // 裁剪
//                FFmpegUtil.cropAudio(file, out, 30, 10, listener)

                // 混音
//                FFmpegUtil.mixAudio(file, file2, out, listener)

                // 拼接
                FFmpegUtil.concatAudio(out, listener, file, file2)
            }
        }
    }
}
