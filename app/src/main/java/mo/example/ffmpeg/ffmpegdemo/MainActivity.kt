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
                Log.e("FFmpeg_VideoEditor", "isExist:${file.exists()}  path:${file.absolutePath}")

                val out = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "forhim_cut.m4a")
                out.delete()

                val mediaMetadataRetriever = MediaMetadataRetriever()
                mediaMetadataRetriever.setDataSource(file.absolutePath)
                val duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
                mediaMetadataRetriever.release()
                Log.e("FFmpeg_VideoEditor", "duration $duration")

                FFmpegCmd.exec("ffmpeg -i ${file.absolutePath} -ss 00:00:00 -t 40 -vsync 2 -c copy ${out.absolutePath}".split(" ").toTypedArray() , duration, object : FFmpegCmd.OnCmdExecListener{
                    override fun onSuccess() {
                        Log.e("FFmpeg_VideoEditor", "ffmpeg cmd exec success ${out.exists()}")
                    }

                    override fun onFailure() {
                        Log.e("FFmpeg_VideoEditor", "ffmpeg cmd exec failed")
                    }

                    override fun onProgress(progress: Float) {
                        Log.e("FFmpeg_VideoEditor", "ffmpeg cmd exec onProgress $progress")
                    }

                })
            }
        }
    }
}
