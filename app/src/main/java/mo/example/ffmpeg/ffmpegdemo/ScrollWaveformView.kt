package mo.example.ffmpeg.ffmpegdemo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import fm.qingting.audioeditor.IAudioRecorder

class ScrollWaveformView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    companion object {
        private const val TAG = "WaveformView"
        const val SAMPLE_RATE = 1024 // 每1024个点选一个最大的画波形
    }

    var isScrollable = false

    private var mWidth = 0
    private var mHeight = 0
    private var mWaveRectHeight = 0f // 波形图区域的高度
    private var mIndicatorHeaderHeight = 0f // 指针在波形图区域外的高度
    private var mDensity = 0f
    private var mLineSpacing = 0f

    private val mWaveLineSelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mWaveLineUnSelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mWaveRectLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mShortData = mutableListOf<Short>()

    init {
        mDensity = context.resources.displayMetrics.density

        mWaveLineSelectedPaint.color = Color.parseColor("#FFBD66")
        mWaveLineSelectedPaint.strokeWidth = mDensity * 1

        mWaveLineUnSelectedPaint.color = Color.parseColor("#666666")
        mWaveLineUnSelectedPaint.strokeWidth = mDensity * 1

        mWaveRectLinePaint.color = Color.parseColor("#CCCCCC")
        mWaveRectLinePaint.strokeWidth = mDensity * 1

        mIndicatorHeaderHeight = mDensity * 35
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
        if (isScrollable) {

        } else {
            val rateY = mWaveRectHeight / 65535
            var lineHeight: Float
            var x: Float
            val baseLine = mWaveRectHeight / 2 + mIndicatorHeaderHeight
            for (index in mShortData.indices) {
                lineHeight = Util.abs(mShortData[index]) * 2 * rateY
                if (lineHeight < 1) {
                    lineHeight = 1f
                }
                x = index * mWaveLineSelectedPaint.strokeWidth + index * mLineSpacing
                canvas.drawLine(
                    x,
                    baseLine - lineHeight / 2,
                    x,
                    baseLine + lineHeight / 2,
                    if (true) mWaveLineSelectedPaint else mWaveLineUnSelectedPaint
                )
                Log.e(TAG, "draw line: x:$x startY:${baseLine - lineHeight / 2} stopY:${baseLine + lineHeight / 2} value: ${mShortData[index]}")
            }
        }
    }

    fun setAudioData(arr: List<Short>) {
        mShortData.clear()
        mShortData.addAll(arr)
        if (!isScrollable && mWidth > 0 && mShortData.size * 1 * mDensity > mWidth) {
            // 缩小波形线宽
            val lineWidth = mWidth * 1.0f / mShortData.size
            Log.e(TAG, "set line width: $lineWidth")
            mWaveLineSelectedPaint.strokeWidth = lineWidth / 2
            mWaveLineUnSelectedPaint.strokeWidth = lineWidth / 2
            mLineSpacing = lineWidth / 2
        }
        postInvalidate()
    }

    fun getWaveWidth(): Float {
        return mShortData.size * (mWaveLineSelectedPaint.strokeWidth + mLineSpacing)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return super.onTouchEvent(event)
        }
        val x = event.x
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isScrollable) {

                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isScrollable) {

                }
            }
            MotionEvent.ACTION_UP -> {
                if (isScrollable) {

                }
            }
        }
        return super.onTouchEvent(event)
    }

    fun pixelsToMs(pixel: Float):Long {
        // 计算原始数据长度(bytes)
        val totalRawDataLength = mShortData.size * 2 * SAMPLE_RATE
        // 每秒数据长度(bytes) = 采样率*通道数*16/8
        val bytesPerSeconds = IAudioRecorder.DEFAULT_SAMPLE_RATE * 1 * 16 / 8
        // 计算总时长 = 原始数据长度 / 每秒数据长度
        val duration = 1000 * totalRawDataLength / bytesPerSeconds
        return ((pixel / (mShortData.size * (mWaveLineSelectedPaint.strokeWidth + mLineSpacing))) * duration).toLong()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h
        mWaveRectHeight = mHeight - mIndicatorHeaderHeight
        if (!isScrollable && mShortData.size > 0 && mShortData.size * 1 * mDensity > mWidth) {
            // 缩小波形线宽
            val lineWidth = mWidth * 1.0f / mShortData.size
            mWaveLineSelectedPaint.strokeWidth = lineWidth / 2
            mWaveLineUnSelectedPaint.strokeWidth = lineWidth / 2
            mLineSpacing = lineWidth / 2
        }
    }
}