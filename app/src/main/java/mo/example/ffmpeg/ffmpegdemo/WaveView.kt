package mo.example.ffmpeg.ffmpegdemo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import fm.qingting.audioeditor.IAudioRecorder
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * WaveSurfaceView 的View版实现
 */
class WaveView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var mData = mutableListOf<Short>()//缓冲区数据
    private var mRateX = 9800//控制每隔多少个字节取一个点
    private var mRateY = 1 //  Y轴缩小的比例 默认为1
    private val mDrawInterval = 17//两次绘图间隔的时间
    private val mLineSpace: Float
    private var mLineVerticalSpace: Float = 0f
    private var mCurrentTime: Long = 0//当前时间戳
    private val mPaint: Paint = Paint()
    private val mLineWidth: Float
    private val mDensity: Float = getContext().resources.displayMetrics.density
    private val mByteBuffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
    private var mViewWidth: Int = 0
    private var mViewHeight: Int = 0
    private var mNextIndex = 0 // 需要取的下一个采样点的index
    private var mMaxLines: Int = 0
    private var mOffset: Int = 0


    init {
        mPaint.color = Color.parseColor("#FF7566")
        mPaint.strokeWidth = mDensity * 3
        mPaint.isAntiAlias = true
        mPaint.isFilterBitmap = true
        mPaint.strokeCap = Paint.Cap.ROUND
        mPaint.style = Paint.Style.FILL

        mLineWidth = mDensity * 3
        mLineSpace = mDensity * 3
        mLineVerticalSpace = mDensity * 1.5f
    }

    fun addAudioData(src: ByteArray, readSize: Int) {
        if (mNextIndex >= readSize) {
            mNextIndex -= readSize
            return
        }
        var i = mNextIndex
        while (i < readSize) {
            mByteBuffer.clear()
            mByteBuffer.put(src[i])
            mByteBuffer.put(src[i + 1])
            mData.add(mByteBuffer.getShort(0))
            i += mRateX
            if (i >= readSize) {
                mNextIndex = i - readSize
            }
        }
        val time = System.currentTimeMillis()
        if (time - mCurrentTime >= mDrawInterval) {
            if (mData.size == 0)
                return
            while (mData.size - mOffset > mMaxLines) {
                mOffset++
            }
            postInvalidate()
            mCurrentTime = System.currentTimeMillis()
        }
    }

    fun clearData() {
        mData.clear()
        mNextIndex = 0
        mOffset = 0
        postInvalidate()
    }

    fun dropData(droppedTime: Long) {
        mNextIndex = 0
        val toIndex = ((droppedTime * IAudioRecorder.DEFAULT_SAMPLE_RATE / 1000f) / (mRateX / 2)).toInt()
        if (toIndex > mData.size || 0 > toIndex) {
            mData.clear()
            mOffset = 0
        } else {
            val oldSize = mData.size
            mData = mData.subList(0, toIndex)
            val offset = mOffset - (oldSize - toIndex)
            mOffset = if (offset < 0) 0 else offset
        }
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let {canvas ->
            mRateY = 65535 / 2 / mViewHeight

            // draw wave
            var startY: Float
            val baseLine = mViewHeight / 2
            for (i in mOffset until mData.size) {
                val aShort = amplificationValue(10, mData[i])
                startY = (aShort / mRateY + baseLine).toFloat()// 调节缩小比例，调节基准线
                val x = getLineX(mData.size, i)
                var stopY = mViewHeight - startY
                if (startY < mLineVerticalSpace) {
                    startY = mLineVerticalSpace
                }
                if (startY > mViewHeight - mLineVerticalSpace) {
                    startY = (mViewHeight - mLineVerticalSpace)

                }
                if (stopY < mLineVerticalSpace) {
                    stopY = mLineVerticalSpace
                }
                if (stopY > mViewHeight - mLineVerticalSpace) {
                    stopY = (mViewHeight - mLineVerticalSpace)
                }
                if (startY == stopY) {
                    // 部分机型y坐标相同时不会画点
                    startY -= 0.5f
                    stopY += 0.5f
                }
                canvas.drawLine(x, startY, x, stopY, mPaint)
            }
        }
    }


    private fun getLineX(buffSize: Int, i: Int): Float {
        var index = i
        val x: Float
        if (mOffset > 0) {
            // 已铺满view，则正常绘制
            index -= mOffset
        } else {
            // 没铺满，因为要从右往左，所以空出MaxBuffSize - buffSize个位置再开始画
            index += mMaxLines - buffSize
        }
        x = index * mLineWidth + index * mLineSpace + mLineWidth / 2
        return x
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mViewWidth = w
        mViewHeight = h
        mMaxLines = ((mViewWidth + mLineSpace) / (mLineWidth + mLineSpace)).toInt()
    }

    private fun amplificationValue(scale: Int, value: Short): Short = when {
        value * scale < java.lang.Short.MIN_VALUE -> java.lang.Short.MIN_VALUE
        value * scale > java.lang.Short.MAX_VALUE -> java.lang.Short.MAX_VALUE
        else -> (value * scale).toShort()
    }
}
