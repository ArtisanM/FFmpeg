package mo.example.ffmpeg.ffmpegdemo

import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import fm.qingting.audioeditor.FFmpegCmd
import fm.qingting.audioeditor.FFmpegUtil
import java.io.File

class CropActivity : AppCompatActivity() {

    private lateinit var mFile: File
    private lateinit var mRawAudioFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop)
        val file = intent.getSerializableExtra("file") as? File
        val raw = intent.getSerializableExtra("raw") as? File
        if (file == null) {
            finish()
            return
        }
        mFile = file
        if (raw == null) {
            mRawAudioFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "${mFile.name.substring(0, mFile.name.lastIndexOf("."))}.raw"
            )
            FFmpegUtil.resample2Pcm(mFile, mRawAudioFile, object : FFmpegCmd.OnCmdExecListener {
                override fun onSuccess() {
                    readRawFile()
                }

                override fun onFailure() {
                }

                override fun onProgress(progress: Float) {
                }
            })
        } else {
            mRawAudioFile = raw
            readRawFile()
        }
    }

    private fun readRawFile() {

    }
}
