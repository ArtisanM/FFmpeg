package mo.example.ffmpeg.ffmpegdemo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View

class WaveIndicatorView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    companion object {
        private const val TAG = "WaveformView"
    }

    var showIndicatorHeader = true
    var showSelectedRect = false
    var listener: OnIndicatorMoveListener? = null

    private var mWidth = 0
    private var mHeight = 0
    private var mIndicatorCircleRadius = 0f
    private var mIndicatorCircleToWaveTop = 0f // 指针小圆点距离波形图上边界距离
    private var mIndicatorHeaderHeight = 0f // 指针在波形图区域外的高度
    private var mIndicatorX = 0f
    private var mDensity = 0f
    private var mText = ""
    private var mEndsX = 0F
    private var mMinX = 0F

    private val mIndicatorLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mSelectedRectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mIndicatorTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    init {
        mDensity = context.resources.displayMetrics.density
        mIndicatorLinePaint.color = Color.parseColor("#FF7566")
        mIndicatorLinePaint.strokeWidth = mDensity * 2
        mIndicatorLinePaint.strokeCap = Paint.Cap.ROUND

        mSelectedRectPaint.color = Color.parseColor("#1AFF7566")

        mIndicatorCircleRadius = mDensity * 6
        mIndicatorHeaderHeight = mDensity * 35
        mIndicatorCircleToWaveTop = mDensity * 6

        mIndicatorTextPaint.color = mIndicatorLinePaint.color
        mIndicatorTextPaint.textSize = context.resources.displayMetrics.scaledDensity * 11
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let {
            drawIndicator(it)
        }
    }

    // touch事件x坐标在指针左右各14dp范围内，视为点击指针
    private fun isInIndicatorRange(x: Float): Boolean =
        x in mMinX..mEndsX && x <= mIndicatorX + mDensity * 14 && x >= mIndicatorX - mDensity * 14

    private fun drawIndicator(canvas: Canvas) {
        if (showSelectedRect) {
            canvas.drawRect(mIndicatorX, mIndicatorHeaderHeight, mWidth.toFloat(), mHeight.toFloat(), mSelectedRectPaint)
        }
        canvas.drawLine(
            mIndicatorX,
            if (showIndicatorHeader) mIndicatorHeaderHeight - mIndicatorCircleToWaveTop else mIndicatorHeaderHeight,
            mIndicatorX,
            mHeight.toFloat(),
            mIndicatorLinePaint
        )
        if (showIndicatorHeader) {
            canvas.drawCircle(
                mIndicatorX,
                mIndicatorHeaderHeight - mIndicatorCircleToWaveTop - mIndicatorCircleRadius,
                mIndicatorCircleRadius,
                mIndicatorLinePaint
            )
            val textWidth = mIndicatorTextPaint.measureText(mText)
            canvas.drawText(mText, mIndicatorX - textWidth / 2,  mIndicatorHeaderHeight - mDensity * 21, mIndicatorTextPaint)
        }
    }

    fun setIndicatorText(txt: String) {
        if (mText != txt) {
            mText = txt
        }
    }

    fun setEndsX(pixel: Float) {
        mEndsX = pixel
    }

    fun setMinX(pixel: Float) {
        mMinX = pixel
    }

    fun setIndicatorX(pixel: Float) {
        mIndicatorX = pixel
    }

    fun getIndicatorX():Float = mIndicatorX

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return super.onTouchEvent(event)
        }
        val x = event.x
        Log.e(TAG, "indicator touch x: $x")
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isInIndicatorRange(x)) {
                    mIndicatorX = x
                    invalidate()
                    listener?.onIndicatorMoveStart(x)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isInIndicatorRange(x)) {
                    mIndicatorX = x
                    invalidate()
                    listener?.onIndicatorMoved(x)
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isInIndicatorRange(x)) {
                    mIndicatorX = x
                    invalidate()
                    listener?.onIndicatorMoveUp(x)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h
        mEndsX = w.toFloat()
        mIndicatorX = w * 0.85f // 指针起始位置
    }

    interface OnIndicatorMoveListener {
        fun onIndicatorMoveStart(x: Float)
        fun onIndicatorMoved(x: Float)
        fun onIndicatorMoveUp(x: Float)
    }
}