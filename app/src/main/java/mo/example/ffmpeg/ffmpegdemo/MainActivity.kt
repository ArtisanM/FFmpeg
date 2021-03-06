package mo.example.ffmpeg.ffmpegdemo

import android.Manifest
import android.annotation.SuppressLint
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
import android.widget.Toast
import fm.qingting.audioeditor.AudioRecorderImpl
import fm.qingting.audioeditor.FFmpegUtil
import fm.qingting.audioeditor.IAudioRecorder
import fm.qingting.audioeditor.OnAudioRecordListener
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
    private lateinit var wavesfv: WaveView

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
        wavesfv = findViewById(R.id.wavesfv)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
        audioRecord.setOnAudioRecordListener(object : OnAudioRecordListener {
            override fun onAudioFrameCaptured(audioData: ByteArray, readSize: Int) {
                wavesfv.addAudioData(audioData, readSize)
            }
        })

//        FFmpegUtil.resample2Pcm(file, File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RightNow.raw"), listener)
    }

    fun crop(view: View) {
        FFmpegUtil.cutAudioWithSameFormat(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "m_1547691621606.m4a"), out, 30500, listener)
    }

    fun mix(view: View) {
        FFmpegUtil.mixAudio(file, file2, out, listener)
    }

    fun concat(view: View) {
        FFmpegUtil.concatAudio(out, listener, file, file2)
    }

    fun goPcmPlayer(view: View) {
        startActivity(Intent(this, PcmPlayerActivity::class.java))
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
        audioRecord.toggle()
    }

    @SuppressLint("CheckResult")
    fun goPlay(view: View) {
//        audioRecord.getAudio().observeOn(AndroidSchedulers.mainThread()).subscribe({
//            startActivity(Intent(this, ShiTingActivity::class.java).putExtra("file", it))
//        }, { t ->
//            t.printStackTrace()
//            Toast.makeText(this, "获取音频失败", Toast.LENGTH_SHORT).show()
//        })

//        startActivity(Intent(this, ShiTingActivity::class.java).putExtra("file", File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "m_1547626485779.m4a")))
        startActivity(Intent(this, ShiTingActivity::class.java).putExtra("file", File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "m_1547691621606.m4a")))
    }

    fun goCrop(view: View) {
        audioRecord.getAudio().observeOn(AndroidSchedulers.mainThread()).subscribe({
            val intent = Intent(this, CropActivity::class.java).putExtra("file", it)
            startActivity(intent)
        }, { t ->
            t.printStackTrace()
            Toast.makeText(this, "获取音频失败", Toast.LENGTH_SHORT).show()
        })

    }

    fun goCropExist(view: View) {
        startActivity(Intent(this, CropActivity::class.java).putExtra("file", File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "m_1547691621606.m4a")))
    }

    @SuppressLint("CheckResult")
    fun saveAudio(view: View) {
        audioRecord.getAudio().observeOn(AndroidSchedulers.mainThread()).subscribe({
            Toast.makeText(this, "保存成功，文件地址：${it.absolutePath}", Toast.LENGTH_SHORT).show()
        }, { t ->
            t.printStackTrace()
            Toast.makeText(this, "保存失败}", Toast.LENGTH_SHORT).show()
        })
    }

    fun onClickCached(view: View) {
        for (file in Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).listFiles()) {
            if (file.isFile && (file.name.startsWith("m_") || file.name.startsWith("qt_record_"))) {
                file.delete()
            }
        }
    }


    @SuppressLint("CheckResult")
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

                disposable = Observable.interval(0, 1, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        seekBar.progress = mediaPlayer.currentPosition
                        tvCurrent.text = getDurationString(seekBar.progress)
                    }, { e -> e.printStackTrace() })
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
                audioRecord.setOutputFile(
                    File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "m_${System.currentTimeMillis()}.m4a"
                    )
                )
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
        wavesfv.clearData()
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
