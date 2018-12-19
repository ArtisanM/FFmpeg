package mo.example.ffmpeg.ffmpegdemo

import android.Manifest
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    external fun avformatInfo(): String
    external fun avcodecInfo(): String
    external fun avfilterInfo(): String
    external fun configurationInfo(): String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),100)

        // Example of a call to a native method
        sample_text.text = avfilterInfo()

//        val duration = endTime - startTime;
//// 构建一条视频裁剪命令
//        val cmd = CmdList();
//        cmd.append("ffmpeg");
//        cmd.append("-y");
//        cmd.append("-ss").append(startTime/ 1000).append("-t").append(duration / 1000).append("-accurate_seek");
//        cmd.append("-i").append(srcFile);
//        cmd.append("-codec").append("copy").append(destFile);
//
//        FFmpegUtil.execCmd(cmd, duration)
    }

    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("FFmpegUtil")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty()) {
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "music.mp3")
                Log.e("MainActivity", "isExist:${file.exists()}  path:${file.absolutePath}")
            }
        }
    }
}
