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
import android.widget.SeekBar
import android.widget.TextView
import fm.qingting.audioeditor.AudioRecorderImpl
import fm.qingting.audioeditor.FFmpegUtil
import fm.qingting.audioeditor.IAudioRecorder
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.io.File
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private var granted = false

    private val mediaPlayer = MediaPlayer()
    private var audioRecord: IAudioRecorder = AudioRecorderImpl()
    private val file =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RightNow.mp3")
    private val file2 =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "少一点天分.mp3")
    private val out = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "mix.m4a")
    private val listener = object : fm.qingting.audioeditor.FFmpegCmd.OnCmdExecListener {
        override fun onSuccess() {
            Log.e("FFmpeg_Editor", "ffmpeg cmd exec success ${out.exists()}")
        }

        override fun onFailure() {
            Log.e("FFmpeg_Editor", "ffmpeg cmd exec failed")
        }

        override fun onProgress(progress: Float) {
            Log.e("FFmpeg_Editor", "ffmpeg cmd exec onProgress $progress")
        }

    }

    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrent: TextView
    private lateinit var tvTotal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO),
            100
        )
        seekBar = findViewById(R.id.seekbar)
        tvCurrent = findViewById(R.id.current)
        tvTotal = findViewById(R.id.total)

//        val audioTrack = Track(wav)
//        audioTrack.play()
//
//        val seekBar: SeekBar = findViewById(R.id.seekbar)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.seekTo(progress)
                    }
                }
            }

        })
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
        if (audioRecord.isRecording()) {
            audioRecord.stopRecord()
        } else {
            audioRecord.setOutputFile(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "m_${System.currentTimeMillis()}.m4a"))
            audioRecord.startRecord()
        }
    }

    fun pauseRecord(view: View) {
        if (audioRecord.isRecording()) {
            audioRecord.pauseRecord()
        } else {
            audioRecord.resumeRecord()
        }
    }

    fun playRecord(view: View) {
        disposable?.dispose()
        audioRecord.getAudio().subscribe({
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.reset()
            mediaPlayer.setDataSource(it.absolutePath)
            mediaPlayer.setOnPreparedListener {
                val duration = it.duration
                seekBar.max = duration
                seekBar.progress = it.currentPosition
                tvCurrent.text = "00:00"
                tvTotal.text = getDurationString(duration)

                disposable = Observable.interval(0,1, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe({
                    seekBar.progress = mediaPlayer.currentPosition
                    tvCurrent.text = getDurationString(seekBar.progress)
                }, {e -> e.printStackTrace()})
                it.start()
            }
            mediaPlayer.setOnCompletionListener {
                disposable?.dispose()
                seekBar.progress = mediaPlayer.currentPosition
                tvCurrent.text = getDurationString(seekBar.progress)
            }
            mediaPlayer.setOnErrorListener { mp, what, extra ->
                Log.e("FFmpeg_Editor", "mediaPlayer onErrorr  what:$what  extra:$extra")
                disposable?.dispose()
                true
            }
            mediaPlayer.prepareAsync()
        }, { t -> t.printStackTrace() })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults.size > 1) {
                granted = true
//                val mediaMetadataRetriever = MediaMetadataRetriever()
//                mediaMetadataRetriever.setDataSource(file.absolutePath)
//                val duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong() // ms
//                mediaMetadataRetriever.release()
//                Log.e("FFmpeg_Editor", "duration $duration")

            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123 && resultCode == Activity.RESULT_OK) {
            data?.data?.let { startPlay(it) }
        }
    }

    private var disposable: Disposable? = null

    private fun startPlay(uri: Uri) {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.reset()
        mediaPlayer.setDataSource(this, uri)
        mediaPlayer.setOnPreparedListener {
            it.start()
        }
        mediaPlayer.setOnErrorListener { mp, what, extra ->
            Log.e("FFmpeg_Editor", "mediaPlayer onErrorr  what:$what  extra:$extra")
            true
        }
        mediaPlayer.prepareAsync()
    }


    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        audioRecord.release()
    }

    private fun getDurationString(seconds: Int): String {
        var seconds = seconds / 1000

        val hours = seconds / 3600
        val minutes = seconds % 3600 / 60
        seconds = seconds % 60

        return twoDigitString(minutes) + ":" + twoDigitString(seconds)
    }

    private fun twoDigitString(number: Int): String {

        if (number == 0) {
            return "00"
        }

        return if (number / 10 == 0) {
            "0$number"
        } else number.toString()

    }
}
