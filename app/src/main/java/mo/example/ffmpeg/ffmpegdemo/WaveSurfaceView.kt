package mo.example.ffmpeg.ffmpegdemo

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Rect
import android.util.AttributeSet
import android.view.SurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class WaveSurfaceView(context: Context, attrs: AttributeSet) : SurfaceView(context, attrs) {

    private val mData = ArrayList<Short>()//缓冲区数据
    private var mRateX = 10000//控制多少帧取一帧
    private var mRateY = 1 //  Y轴缩小的比例 默认为1
    private val mDrawInterval = 17//两次绘图间隔的时间
    private val mLineSpace: Float
    private var mLineVerticalSpace: Float = 0f
    private var mCurrentTime: Long = 0//当前时间戳
    private val mPaint: Paint
    private val mLineWidth: Float
    private val mDensity: Float
    private val mByteBuffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
    private var mViewWidth: Int = 0
    private var mViewHeight: Int = 0
    private var mNextIndex = 0 // 需要取的下一个采样点的index
    private var mMaxBuffSize: Int = 0


    init {
        setZOrderOnTop(true)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        mDensity = getContext().resources.displayMetrics.density

        mPaint = Paint()
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
            while (mData.size > mMaxBuffSize) {
                mData.removeAt(0)
            }
            drawData(mData.clone() as ArrayList<Short>, mViewHeight / 2)// 把缓冲区数据画出来
            mCurrentTime = System.currentTimeMillis()
        }
    }

    fun clearData() {
        mData.clear()
        mNextIndex = 0

        val canvas = holder.lockCanvas(Rect(0, 0, mViewWidth, mViewHeight))
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        holder.unlockCanvasAndPost(canvas)
    }

    /**
     * 绘制指定区域
     *
     * @param buf
     * 缓冲区
     * @param baseLine
     * Y轴基线
     */
    private fun drawData(buf: ArrayList<Short>, baseLine: Int) {
        mRateY = 65535 / 2 / mViewHeight

        val canvas = holder.lockCanvas(Rect(0, 0, mViewWidth, mViewHeight)) ?: return
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        // draw wave
        var startY: Float
        for (i in buf.indices) {
            val aShort = amplificationValue(10, buf[i])
            startY = (aShort / mRateY + baseLine).toFloat()// 调节缩小比例，调节基准线
            val x = getLineX(buf.size, i)
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
            canvas.drawLine(x, startY, x, stopY, mPaint)
        }
        holder.unlockCanvasAndPost(canvas)// 解锁画布，提交画好的图像
    }

    private fun getLineX(buffSize: Int, i: Int): Float {
        var index = i
        val x: Float
        if (buffSize >= mMaxBuffSize) {
            // 已铺满view，则正常绘制
        } else {
            // 没铺满，因为要从右往左，所以空出MaxBuffSize - buffSize个位置再开始画
            index += mMaxBuffSize - buffSize
        }
        x = index * mLineWidth + index * mLineSpace + mLineWidth / 2
        return x
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mViewWidth = w
        mViewHeight = h
        mMaxBuffSize = ((mViewWidth + mLineSpace) / (mLineWidth + mLineSpace)).toInt()
    }

    private fun amplificationValue(scale: Int, value: Short): Short = when {
        value * scale < java.lang.Short.MIN_VALUE -> java.lang.Short.MIN_VALUE
        value * scale > java.lang.Short.MAX_VALUE -> java.lang.Short.MAX_VALUE
        else -> (value * scale).toShort()
    }
}
