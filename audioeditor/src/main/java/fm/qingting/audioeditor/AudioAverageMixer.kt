package fm.qingting.audioeditor

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 混音之线性叠加算法，直接加和并且钳位。
 */
class AudioAverageMixer : IAudioMixer {

    private val mConvertBuffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)

    override fun mix(data: Array<ByteArray>, lengthArray: IntArray, volume: FloatArray?): Int {
        // 无音轨或只有一条音轨，则直接返回，不处理
        if (data.isEmpty() || data.size == 1) {
            return lengthArray[0]
        }
        // 音轨音量， 默认都为1
        val volumes: FloatArray = volume ?: FloatArray(data.size).also {
            for (i in 0 until data.size) {
                it[i] = 1f
            }
        }
        // 数组size跟别的不匹配
        if (lengthArray.size != data.size || volumes.size != lengthArray.size || volumes.size != data.size) {
            return lengthArray[0]
        }
        // 限制音量调节范围
        trapVolume(volumes)

        // 找到最长的音轨的length
        var maxLength = 0
        for (i in lengthArray) {
            if (i > maxLength) {
                maxLength = i
            }
        }

        var short = 0
        for (i in 0 until maxLength step 2) {
            for (trackIndex in 0 until data.size) {
                // 注意相对短的音轨的边界
                if (i + 1 < lengthArray[trackIndex]) {
                    mConvertBuffer.clear()
                    mConvertBuffer.put(data[trackIndex][i])
                    mConvertBuffer.put(data[trackIndex][i + 1])
                    // 先乘以音量，再叠加
                    short = (short + mConvertBuffer.getShort(0) * volumes[trackIndex]).toInt()
                }
            }
            // 检查溢出
            if (short > Short.MAX_VALUE) {
                short = Short.MAX_VALUE.toInt()
            }
            if (short < Short.MIN_VALUE) {
                short = Short.MIN_VALUE.toInt()
            }
            // 转回byte放入第一个data数组
            data[0][i] = (short and 0xff).toByte()
            data[0][i + 1] = ((short shr 8) and 0xff).toByte()

            short = 0
        }
        return maxLength
    }

}