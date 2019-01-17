package mo.example.ffmpeg.ffmpegdemo

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
import mo.example.ffmpeg.ffmpegdemo.Util.getMaxValue
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

class CropActivity : AppCompatActivity() {

    private lateinit var mFile: File
    private lateinit var mRawAudioFile: File
    private lateinit var mWaveView: ScrollWaveformView
    private lateinit var mIndicator: WaveIndicatorView

    private var data = mutableListOf<Short>()
    private var buffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
    private var mDuration = 0L
    private var mPlayer: MediaPlayer? = null
    private var mDisposable: Disposable? = null

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

        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(mFile.absolutePath)
        mDuration =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong() // ms
        mediaMetadataRetriever.release()
        if (mDuration < 3000) {
            Toast.makeText(this, "音频时长需要大于3秒", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        mWaveView = findViewById(R.id.wave)
        mIndicator = findViewById(R.id.indicator)
        mIndicator.showSelectedRect = true
        mIndicator.listener = object :WaveIndicatorView.OnIndicatorMoveListener {

            override fun onIndicatorMoveStart(x: Float) {
                mPlayer?.pause()
                stopTimer()
            }

            override fun onIndicatorMoved(x: Float) {
                mIndicator.setIndicatorText(Util.formatElapsedTimeMH(mWaveView.pixelToMs(x)))
                mIndicator.invalidate()
                mWaveView.setSelectedFrame(mWaveView.pixelToFrame(x))
            }

            override fun onIndicatorMoveUp(x: Float) {
                seekAndStart(x)
            }

        }
        mWaveView.listener = object : ScrollWaveformView.WaveformListener{
            override fun waveformFlingEnd() {
                seekAndStart(mIndicator.getIndicatorX())
            }

            override fun waveformTouchStart(x: Float) {
                mPlayer?.pause()
                stopTimer()
            }

            override fun waveformTouchMove(x: Float) {
                mIndicator.setIndicatorText(Util.formatElapsedTimeMH(mWaveView.pixelToMs(mIndicator.getIndicatorX())))
                mIndicator.invalidate()
                mWaveView.setSelectedFrame(mWaveView.pixelToFrame(mIndicator.getIndicatorX()))
            }

            override fun waveformTouchUp(x: Float)  {
                seekAndStart(mIndicator.getIndicatorX())
            }

            override fun waveformFling(x: Float) {
            }

            override fun waveformReDraw() {
                mIndicator.setIndicatorText(Util.formatElapsedTimeMH(mWaveView.pixelToMs(mIndicator.getIndicatorX())))
                mIndicator.invalidate()
            }

        }

        if (raw == null) {
            mRawAudioFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "${mFile.name.substring(0, mFile.name.lastIndexOf("."))}.raw"
            )
            mRawAudioFile.delete()
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

    fun onClickPlay(view: View) {
        if (mPlayer == null) {
            mPlayer = MediaPlayer()
        }
        mPlayer?.reset()
        mPlayer?.setDataSource(mFile.absolutePath)
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
            Log.e("CropActivity", "OnError: $what $extra")
            stopTimer()
            false
        }
        mPlayer?.prepareAsync()
    }

    private fun startTimer() {
        stopTimer()
        mDisposable = Observable.interval(16, TimeUnit.MILLISECONDS).subscribe({
            val cur = mPlayer?.currentPosition?.toLong() ?: 0L
            mWaveView.setSelectedFrame(mWaveView.msToFrame(cur))
        }, { e -> e.printStackTrace() })
    }

    private fun stopTimer() {
        mDisposable?.dispose()
    }

    private fun seekAndStart(x: Float) {
        try {
            if (mPlayer == null) {
                mPlayer = MediaPlayer()
                mPlayer?.setDataSource(mFile.absolutePath)
                mPlayer?.setOnCompletionListener {
                    stopTimer()
                }
                mPlayer?.setOnPreparedListener {
                    mPlayer?.seekTo(mWaveView.pixelToMs(x).toInt())
                }
                mPlayer?.setOnSeekCompleteListener {
                    it.start()
                    startTimer()
                }
                mPlayer?.setOnErrorListener { mp, what, extra ->
                    Log.e("CropActivity", "OnError: $what $extra")
                    stopTimer()
                    false
                }
                mPlayer?.prepareAsync()
            } else {
                mPlayer?.seekTo(mWaveView.pixelToMs(x).toInt())
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun readRawFile() {
        val inputStream = FileInputStream(mRawAudioFile)
        val byteArray = ByteArray(ScrollWaveformView.SAMPLE_RATE * 2)
        var ret: Int
        while ({ ret = inputStream.read(byteArray); ret }() > 0) {
            data.add(getMaxValue(byteArray, buffer))
        }
        inputStream.close()
        mWaveView.post {
            mWaveView.setAudioData(data)
            mIndicator.setEndsX(mWaveView.frameToPixel(data.size) - resources.displayMetrics.density * 9)
            mIndicator.setIndicatorX(mWaveView.getLast3FramePixel())
            mIndicator.setMinX(resources.displayMetrics.density * 9)
            mIndicator.setIndicatorText(Util.formatElapsedTimeMH(mWaveView.pixelToMs(mIndicator.getIndicatorX())))
            mIndicator.invalidate()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
        mPlayer?.reset()
        mPlayer?.release()
    }
}
