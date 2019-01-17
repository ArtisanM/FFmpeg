package mo.example.ffmpeg.ffmpegdemo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import fm.qingting.audioeditor.IAudioRecorder

class ScrollWaveformView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    interface WaveformListener {
        fun waveformTouchStart(x: Float)
        fun waveformTouchMove(x: Float)
        fun waveformTouchUp(x: Float)
        fun waveformFling(x: Float)
        fun waveformReDraw()
        fun waveformFlingEnd()
    }

    companion object {
        private const val TAG = "WaveformView"
        // 每4900个点选一个最大的画波形。波形线宽1dp间隔1dp，所以在1080P屏幕上能展示多少s的波形：最多展示1080/（2*3）=180个点 再乘以4900的波形采样率，除以44100的音频采样率，得到时间为20s
        const val SAMPLE_RATE = 4900
    }

    var listener: WaveformListener? = null

    private var mWidth = 0
    private var mHeight = 0
    private var mWaveRectHeight = 0f // 波形图区域的高度
    private var mIndicatorHeaderHeight = 0f // 指针在波形图区域外的高度
    private var mDensity = 0f
    private var mLineSpacing = 0f
    private var mOffset = 0
    private var mTouchInitialOffset = 0
    private var mOffsetGoal = 0
    private var mFlingVelocity = 0
    private var mTouchStart = 0f
    private var mTouchDragging = false
    private var mRateY = 0.1f
    private var mSelectedFrame = 0
    private var mMaxFramesInView = 180
    private var mMaxOffset = 180
    private val mGestureDetector: GestureDetector

    private val mWaveLineSelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mWaveLineUnSelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mWaveRectLinePaint = Paint(Paint.ANTI_ALIAS_FLAG) // 已选中区域画笔
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
        mLineSpacing = 1 * mDensity

        mGestureDetector = GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(e1: MotionEvent, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                    listener?.waveformFling(vx)
                    mTouchDragging = false
                    mOffsetGoal = mOffset
                    mFlingVelocity = (-vx).toInt()
                    updateDisplay()
                    return true
                }
            }
        )
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply {
            // 先画波形图边界
            drawLine(0f, mIndicatorHeaderHeight, mWidth.toFloat(), mIndicatorHeaderHeight, mWaveRectLinePaint)
            drawLine(0f, mHeight.toFloat(), mWidth.toFloat(), mHeight.toFloat(), mWaveRectLinePaint)

            drawWaveform(this)

            if (mOffsetGoal != mOffset) {
                updateDisplay()
                listener?.waveformReDraw()
            } else if (mFlingVelocity != 0) {
                updateDisplay()
                listener?.waveformReDraw()
            }
        }
    }

    private fun drawWaveform(canvas: Canvas) {
        if (mLineHeight.size > 0) {
            val lineSpace = mWaveLineSelectedPaint.strokeWidth + mLineSpacing
            var width = (mLineHeight.size * lineSpace - mOffset * lineSpace).toInt()
            var halfLineHeight: Float
            var x = 0f
            val baseLine = mWaveRectHeight / 2 + mIndicatorHeaderHeight

            if (width > mWidth) {
                width = mWidth
            }

            var i = 0
            while (x < width) {
                halfLineHeight = mLineHeight[mOffset + i] / 2f
                canvas.drawLine(
                    x,
                    baseLine - halfLineHeight,
                    x,
                    baseLine + halfLineHeight,
                    if (mOffset + i <= mSelectedFrame) mWaveLineSelectedPaint else mWaveLineUnSelectedPaint
                )
                x += lineSpace
                i++
            }
        }
    }

    fun setAudioData(arr: List<Short>) {
        mLineHeight.clear()
        var lineHeight: Short = 0
        for (sh in arr) {
            lineHeight = (Util.abs(sh) * 2 * mRateY).toShort()
            if (lineHeight < 1) {
                lineHeight = 1
            }
            mLineHeight.add(lineHeight)
        }

        // 默认选中倒数第三秒
        val last3Frame = mLineHeight.size - (IAudioRecorder.DEFAULT_SAMPLE_RATE * 3 / SAMPLE_RATE)
        mSelectedFrame = if (last3Frame >= 0) last3Frame else 0

        if (mLineHeight.size > mMaxFramesInView) {
            // 超过一屏 滚到最后
            mOffset = mLineHeight.size - mMaxFramesInView
            mOffsetGoal = mOffset

            mMaxOffset = mLineHeight.size - mMaxFramesInView
        } else {
            mMaxOffset = 0
        }

        postInvalidate()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return super.onTouchEvent(event)
        }
        if (mGestureDetector.onTouchEvent(event)) {
            return true
        }
        val x = event.x
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mTouchDragging = true
                mTouchStart = x
                mFlingVelocity = 0
                mTouchInitialOffset = mOffset
                listener?.waveformTouchStart(x)
            }
            MotionEvent.ACTION_MOVE -> {
                val offset =
                    trap((mTouchInitialOffset + (mTouchStart - x) / (mWaveLineSelectedPaint.strokeWidth + mLineSpacing)).toInt())
                if (mOffset != offset) {
                    mOffset = offset
                    updateDisplay()
                }
                listener?.waveformTouchMove(x)
            }
            MotionEvent.ACTION_UP -> {
                mTouchDragging = false
                mOffsetGoal = mOffset
                updateDisplay()
                listener?.waveformTouchUp(x)
            }
        }
        return true
    }

    private fun trap(pos: Int): Int {
        if (pos < 0)
            return 0
        return if (pos >= mMaxOffset) mMaxOffset else pos
    }

    /**
     * 设置波形绘制起始采样点
     */
    fun setOffset(offset: Int) {
        mOffset = trap(offset)
        mOffsetGoal = mOffset
    }

    /**
     * 设置已选中采样点
     */
    fun setSelectedFrame(index: Int) {
        if (mSelectedFrame == index) {
            return
        }
        mSelectedFrame = index
        postInvalidate()
    }

    private fun updateDisplay() {
        if (!mTouchDragging) {
            var offsetDelta: Int
            var offset: Int = 0

            if (mFlingVelocity != 0) {
                offsetDelta = mFlingVelocity / 30
                when {
                    mFlingVelocity > 80 -> mFlingVelocity -= 80
                    mFlingVelocity < -80 -> mFlingVelocity += 80
                    else -> {
                        mFlingVelocity = 0
                        listener?.waveformFlingEnd()
                    }
                }

                // 这里对于越界情况处理需要吧速度置为0，所以不再调用trap处理
                offset = mOffset + (offsetDelta / (mWaveLineSelectedPaint.strokeWidth + mLineSpacing)).toInt()

                if (offset >= mMaxOffset) {
                    offset = mMaxOffset
                    mFlingVelocity = 0
                    listener?.waveformFlingEnd()
                }
                if (offset < 0) {
                    offset = 0
                    mFlingVelocity = 0
                    listener?.waveformFlingEnd()
                }
                mOffsetGoal = offset
            } else {
                offsetDelta = mOffsetGoal - mOffset

                offsetDelta = when {
                    offsetDelta > 10 -> offsetDelta / 10
                    offsetDelta > 0 -> 1
                    offsetDelta < -10 -> offsetDelta / 10
                    offsetDelta < 0 -> -1
                    else -> 0
                }

                offset = trap(mOffset + (offsetDelta / (mWaveLineSelectedPaint.strokeWidth + mLineSpacing)).toInt())
            }
            if (mOffset == offset) {
                // offset未变化 不再绘制

                // 这里判断！=0是因为scroll move也可能走这里，但是move是没有flingVelocity的，fling动作是有flingVelocity的
                if (mFlingVelocity != 0) {
                    mFlingVelocity = 0
                    listener?.waveformFlingEnd()
                }
                return
            } else {
                mOffset = offset
            }
        }

        invalidate()
    }

    /**
     * 采样点 to x坐标
     */
    fun frameToPixel(frameIndex: Int): Float {
        if (frameIndex < mOffset) {
            return -1f
        }
        if (frameIndex == mOffset) {
            return 0f
        }
        val lineSpace = mWaveLineSelectedPaint.strokeWidth + mLineSpacing
        return (frameIndex - mOffset) * lineSpace
    }

    fun msToFrame(ms: Long): Int {
        return ((ms * IAudioRecorder.DEFAULT_SAMPLE_RATE / 1000f) / SAMPLE_RATE).toInt()
    }

    fun pixelToFrame(pixel: Float): Int {
        return (mOffset + pixel / (mWaveLineSelectedPaint.strokeWidth + mLineSpacing)).toInt()
    }

    /**
     * x坐标 to 时间点
     */
    fun pixelToMs(pixel: Float): Long {
        val frame = (pixel / (mWaveLineSelectedPaint.strokeWidth + mLineSpacing)) + mOffset
        return (frame * SAMPLE_RATE * 1000L / IAudioRecorder.DEFAULT_SAMPLE_RATE).toLong()
    }

    /**
     * 获取倒数第三秒波形的x坐标
     */
    fun getLast3FramePixel(): Float {
        val last3Frame = mLineHeight.size - (IAudioRecorder.DEFAULT_SAMPLE_RATE * 3 / SAMPLE_RATE)
        return frameToPixel(if (last3Frame >= 0) last3Frame else 0)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h
        mWaveRectHeight = mHeight - mIndicatorHeaderHeight
        mRateY = (mHeight - mIndicatorHeaderHeight) / 65535
        mMaxFramesInView = (w / (mWaveLineSelectedPaint.strokeWidth + mLineSpacing)).toInt()
    }
}