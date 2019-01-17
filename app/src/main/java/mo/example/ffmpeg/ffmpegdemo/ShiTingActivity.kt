package mo.example.ffmpeg.ffmpegdemo

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import fm.qingting.audioeditor.FFmpegCmd
import fm.qingting.audioeditor.FFmpegUtil
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

/*
    90分钟的音频大小达到454MB=465117KB
    每4900个sample取一个点的话，需要加载进内存的音频数据量为95KB
 */
class ShiTingActivity : AppCompatActivity() {

    private lateinit var mFile: File
    private lateinit var mWaveView: WaveformView
    private lateinit var mIndicator: WaveIndicatorView

    private var mTempFile: File? = null
    private var data = mutableListOf<Short>()
    private var mPlayer: MediaPlayer? = null
    private var mDisposable: Disposable? = null
    private var buffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shi_ting)
        mFile = intent.getSerializableExtra("file") as File
        if (!mFile.exists()) {
            finish()
            return
        }
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(mFile.absolutePath)
        val duration =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong() // ms
        mediaMetadataRetriever.release()
        if (duration < 3000) {
            Toast.makeText(this, "音频时长需要大于3秒", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        mWaveView = findViewById(R.id.wave)
        mIndicator = findViewById(R.id.indicator)
        mIndicator.listener = object : WaveIndicatorView.OnIndicatorMoveListener {
            override fun onIndicatorMoveStart(x: Float) {
                mPlayer?.pause()
                stopTimer()
            }

            override fun onIndicatorMoveUp(x: Float) {
                try {
                    mPlayer?.seekTo(mWaveView.pixelToMs(x).toInt())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onIndicatorMoved(x: Float) {
                mIndicator.setIndicatorText(Util.formatElapsedTimeMH(mWaveView.pixelToMs(x)))
                mIndicator.invalidate()
            }
        }

        mTempFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "${mFile.name.substring(0, mFile.name.lastIndexOf("."))}.raw"
        )
        mTempFile?.delete()
        FFmpegUtil.resample2Pcm(mFile, mTempFile!!, object : FFmpegCmd.OnCmdExecListener {
            override fun onSuccess() {
                readFile(mTempFile!!)

                onClickPlay(mWaveView)
            }

            override fun onFailure() {
            }

            override fun onProgress(progress: Float) {
            }
        })
    }

    fun onClickPlay(view: View) {
        if (mPlayer == null) {
            play(mFile)
            return
        }
        try {
            if (mPlayer?.isPlaying == true) {
                mPlayer?.pause()
                stopTimer()
            } else {
                mPlayer?.start()
                startTimer()
            }
        } catch (e: Exception) {
            play(mFile)
        }
    }

    private fun play(file: File) {
        if (mPlayer == null) {
            mPlayer = MediaPlayer()
        }
        mPlayer?.reset()
        mPlayer?.setDataSource(file.absolutePath)
        mPlayer?.setOnCompletionListener {
            stopTimer()
        }
        mPlayer?.setOnPreparedListener {
            it.start()
            startTimer()
        }
        mPlayer?.setOnSeekCompleteListener {
            it.start()
            startTimer()
        }
        mPlayer?.setOnErrorListener { mp, what, extra ->
            Log.e("ShiTingActivity", "OnError: $what $extra")
            stopTimer()
            false
        }
        mPlayer?.prepareAsync()
    }

    private fun startTimer() {
        stopTimer()
        mDisposable = Observable.interval(16, TimeUnit.MILLISECONDS).subscribe({
            val cur = mPlayer?.currentPosition?.toLong() ?: 0L
            val x = mWaveView.msToPixels(cur)
            Log.e("ShiTingActivity", "OnPlay: pos:$cur  x:$x")

            mIndicator.setIndicatorX(x)
            mIndicator.setIndicatorText(Util.formatElapsedTimeMH(cur))
            mIndicator.postInvalidate()

            mWaveView.setSelectedX(x)
            mWaveView.postInvalidate()

        }, { e -> e.printStackTrace() })
    }

    private fun stopTimer() {
        mDisposable?.dispose()
    }

    private fun readFile(file: File) {
        val inputStream = FileInputStream(file)
        val byteArray = ByteArray(WaveformView.SAMPLE_RATE * 2)
        var ret: Int
        while ({ ret = inputStream.read(byteArray); ret }() > 0) {
            data.add(getMaxValue(byteArray))
        }
        inputStream.close()
        mWaveView.post {
            // 可能比主线程快，此时view可能还未完成measure
            mWaveView.setAudioData(data)
            mIndicator.setEndsX(mWaveView.getWaveWidth())
            mIndicator.setIndicatorX(0f)
            mIndicator.setIndicatorText("00:00")
            mIndicator.invalidate()
        }
    }

    fun goCrop(view: View) {
        val intent = Intent(this, CropActivity::class.java).putExtra("file", mFile).putExtra("raw", mTempFile)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
        mPlayer?.reset()
        mPlayer?.release()
        mTempFile?.delete()
    }

    fun getMaxValue(arr: ByteArray): Short {
        var max: Short = 0
        var cur: Short = 0
        for (i in arr.indices step 2) {
            buffer.clear()
            buffer.put(arr[i])
            buffer.put(arr[i + 1])
            cur = buffer.getShort(0)
            if (Util.abs(cur) > Util.abs(max)) {
                max = cur
            }
        }
        return max
    }
}
