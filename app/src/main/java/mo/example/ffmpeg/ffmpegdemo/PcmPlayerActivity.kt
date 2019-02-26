package mo.example.ffmpeg.ffmpegdemo

import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import fm.qingting.audioeditor.IAudioPlayer
import fm.qingting.audioeditor.PcmPlayer
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.io.File
import java.util.concurrent.TimeUnit

class PcmPlayerActivity : AppCompatActivity() {


    private lateinit var mSeekBar: SeekBar
    private lateinit var mCurrent: TextView
    private lateinit var mTotal: TextView

    private val player = PcmPlayer()

    private var mTimer: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pcm_player)
        mSeekBar = findViewById(R.id.seekbar)
        mCurrent = findViewById(R.id.cur)
        mTotal = findViewById(R.id.total)

        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RightNow.raw")
        player.setDataSource(file.absolutePath)
        player.setOnCompletionListener(object : IAudioPlayer.OnCompletionListener {
            override fun onCompletion(player: IAudioPlayer) {
                runOnUiThread {
                    mSeekBar.progress = 0
                    mCurrent.text = "00:00"
                }
            }
        })
        mSeekBar.progress = 0
        mSeekBar.max = player.getDuration().toInt()
        mTotal.text = Util.formatElapsedTimeMH(mSeekBar.max.toLong())
        mCurrent.text = "00:00"
        mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    player.seekTo(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
    }


    private fun startTimer() {
        stopTimer()
        mTimer = Observable.interval(16, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe({
            mSeekBar.progress = player.getCurrentPosition().toInt()
            mCurrent.text = Util.formatElapsedTimeMH(mSeekBar.progress.toLong())
        }, { e -> e.printStackTrace() })
    }

    private fun stopTimer() {
        mTimer?.dispose()
    }

    fun onClickPlay(view: View) {
        player.start()
        startTimer()
    }

    fun onClickPause(view: View) {
        player.pause()
        stopTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
        player.reset()
        player.release()
    }
}
