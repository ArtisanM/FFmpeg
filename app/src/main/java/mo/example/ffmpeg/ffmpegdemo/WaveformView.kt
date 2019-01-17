package mo.example.ffmpeg.ffmpegdemo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import fm.qingting.audioeditor.IAudioRecorder

class WaveformView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    companion object {
        private const val TAG = "WaveformView"
        const val SAMPLE_RATE = 4900
    }

    private var mWidth = 0
    private var mHeight = 0
    private var mWaveRectHeight = 0f // 波形图区域的高度
    private var mIndicatorHeaderHeight = 0f // 指针在波形图区域外的高度
    private var mDensity = 0f
    private var mLineSpacing = 0f
    private var mSelectedX = 0f
    private var mRateY = 0.1f

    private val mWaveLineSelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mWaveLineUnSelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mWaveRectLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mLineHeight = mutableListOf<Short>()

    init {
        mDensity = context.resources.displayMetrics.density

        mWaveLineSelectedPaint.color = Color.parseColor("#FFBD66")
        mWaveLineSelectedPaint.strokeWidth = mDensity * 1

        mWaveLineUnSelectedPaint.color = Color.parseColor("#666666")
        mWaveLineUnSelectedPaint.strokeWidth = mDensity * 1

        mWaveRectLinePaint.color = Color.parseColor("#CCCCCC")
        mWaveRectLinePaint.strokeWidth = mDensity * 1

        mIndicatorHeaderHeight = mDensity * 35

        mLineSpacing = mWaveLineSelectedPaint.strokeWidth / 2
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let {
            // 先画波形图边界
            it.drawLine(0f, mIndicatorHeaderHeight, mWidth.toFloat(), mIndicatorHeaderHeight, mWaveRectLinePaint)
            it.drawLine(0f, mHeight.toFloat(), mWidth.toFloat(), mHeight.toFloat(), mWaveRectLinePaint)

            drawWaveform(it)

        }
    }

    private fun drawWaveform(canvas: Canvas) {
        var halfLineHeight: Float
        var x: Float
        val baseLine = mWaveRectHeight / 2 + mIndicatorHeaderHeight
        for (index in mLineHeight.indices) {
            x = index * mWaveLineSelectedPaint.strokeWidth + index * mLineSpacing
            halfLineHeight = mLineHeight[index] / 2f
            canvas.drawLine(
                x,
                baseLine - halfLineHeight,
                x,
                baseLine + halfLineHeight,
                if (x <= mSelectedX) mWaveLineSelectedPaint else mWaveLineUnSelectedPaint
            )
        }
    }

    fun setSelectedX(x: Float) {
        mSelectedX = x
    }

    fun setAudioData(arr: List<Short>) {
        mLineHeight.clear()
        var lineHeight:Short = 0
        for (sh in arr) {
            lineHeight = (Util.abs(sh) * 2 * mRateY).toShort()
            if (lineHeight < 1) {
                lineHeight = 1
            }
            mLineHeight.add(lineHeight)
        }
        if (mWidth > 0 && mLineHeight.size * 1 * mDensity > mWidth) {
            // 缩小波形线宽
            val lineWidth = mWidth * 1.0f / mLineHeight.size
            mWaveLineSelectedPaint.strokeWidth = lineWidth / 2
            mWaveLineUnSelectedPaint.strokeWidth = lineWidth / 2
            mLineSpacing = lineWidth / 2
        }

        postInvalidate()
    }

    fun getWaveWidth(): Float {
        return mLineHeight.size * (mWaveLineSelectedPaint.strokeWidth + mLineSpacing)
    }

    fun pixelToMs(pixel: Float):Long {
        // 计算原始数据长度(bytes)
        val totalRawDataLength = mLineHeight.size * 2 * SAMPLE_RATE
        // 每秒数据长度(bytes) = 采样率*通道数*16/8
        val bytesPerSeconds = IAudioRecorder.DEFAULT_SAMPLE_RATE * 1 * 16 / 8
        // 计算总时长 = 原始数据长度 / 每秒数据长度
        val duration = 1000L * totalRawDataLength / bytesPerSeconds
        return ((pixel / (mLineHeight.size * (mWaveLineSelectedPaint.strokeWidth + mLineSpacing))) * duration).toLong()
    }

    fun msToPixels(ms: Long): Float {
        val totalRawDataLength = mLineHeight.size * 2 * SAMPLE_RATE
        val bytesPerSeconds = IAudioRecorder.DEFAULT_SAMPLE_RATE * 1 * 16 / 8
        val duration = 1000L * totalRawDataLength / bytesPerSeconds
        return (ms * 1.0f / duration) * mLineHeight.size * (mWaveLineSelectedPaint.strokeWidth + mLineSpacing)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h
        mWaveRectHeight = mHeight - mIndicatorHeaderHeight
        mRateY = (mHeight - mIndicatorHeaderHeight) / 65535
        if (mLineHeight.size > 0 && mLineHeight.size * 1 * mDensity > mWidth) {
            // 缩小波形线宽
            val lineWidth = mWidth * 1.0f / mLineHeight.size
            mWaveLineSelectedPaint.strokeWidth = lineWidth / 2
            mWaveLineUnSelectedPaint.strokeWidth = lineWidth / 2
            mLineSpacing = lineWidth / 2
        }
    }
}