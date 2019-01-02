package mo.example.ffmpeg.ffmpegdemo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import java.io.File



class MainActivity : AppCompatActivity() {

    private var granted = false

    private val mediaPlayer = MediaPlayer()
    private val audioRecord = AudioRecorder()
    private val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RightNow.mp3")
    private val file2 = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "少一点天分.mp3")
    private val out = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "mix.m4a")
    private val listener = object : FFmpegCmd.OnCmdExecListener {
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO),100)
    }

    fun crop(view: View) {
        FFmpegUtil.cropAudio(file, out, 30 * 1000, 10, listener)
    }

    fun mix(view: View) {
        FFmpegUtil.mixAudio(file, file2, out, listener)
    }

    fun concat(view: View) {
        FFmpegUtil.concatAudio(out, listener, file, file2)
    }

    fun play(view: View) {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.reset()
        } else {
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(Intent.createChooser(intent, "Select a file"), 123)
        }
    }

    fun startRecord(view: View) {
        if (audioRecord.isCaptureStarted()) {
            audioRecord.stopCapture()
        } else {
            audioRecord.startCapture()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults.size > 1) {
                granted = true
//                val mediaMetadataRetriever = MediaMetadataRetriever()
//                mediaMetadataRetriever.setDataSource(file.absolutePath)
//                val duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
//                mediaMetadataRetriever.release()
//                Log.e("FFmpeg_VideoEditor", "duration $duration")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123 && resultCode == Activity.RESULT_OK) {
            data?.data?.let { startPlay(it) }
        }
    }

    private fun startPlay(uri: Uri) {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.reset()
        }
        mediaPlayer.setDataSource(this, uri)
        mediaPlayer.setOnPreparedListener {
            it.start()
        }
        mediaPlayer.setOnErrorListener { mp, what, extra ->
            Log.e("FFmpeg_VideoEditor", "mediaPlayer onErrorr  what:$what  extra:$extra")
            true
        }
        mediaPlayer.prepareAsync()
    }


    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}
