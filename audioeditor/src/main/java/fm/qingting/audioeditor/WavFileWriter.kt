package fm.qingting.audioeditor

import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavFileWriter {
    private var mFilepath: String? = null
    private var mDataSize = 0
    private var mDataOutputStream: DataOutputStream? = null

    @Throws(IOException::class)
    fun openFile(filepath: String, sampleRateInHz: Int, channels: Int, bitsPerSample: Int): Boolean {
        if (mDataOutputStream != null) {
            closeFile()
        }
        mFilepath = filepath
        mDataSize = 0
        mDataOutputStream = DataOutputStream(FileOutputStream(filepath))
        return writeHeader(sampleRateInHz, bitsPerSample, channels)
    }

    @Throws(IOException::class)
    fun closeFile(): Boolean {
        var ret = true
        if (mDataOutputStream != null) {
            ret = writeDataSize()
            mDataOutputStream!!.close()
            mDataOutputStream = null
        }
        return ret
    }

    fun writeData(buffer: ByteArray, offset: Int, count: Int): Boolean {
        if (mDataOutputStream == null) {
            return false
        }

        try {
            mDataOutputStream!!.write(buffer, offset, count)
            mDataSize += count
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        return true
    }

    private fun writeHeader(sampleRateInHz: Int, channels: Int, bitsPerSample: Int): Boolean {
        if (mDataOutputStream == null) {
            return false
        }

        val header = WavFileHeader(sampleRateInHz, channels, bitsPerSample)

        try {
            mDataOutputStream!!.writeBytes(header.mChunkID)
            mDataOutputStream!!.write(intToByteArray(header.mChunkSize), 0, 4)
            mDataOutputStream!!.writeBytes(header.mFormat)
            mDataOutputStream!!.writeBytes(header.mSubChunk1ID)
            mDataOutputStream!!.write(intToByteArray(header.mSubChunk1Size), 0, 4)
            mDataOutputStream!!.write(shortToByteArray(header.mAudioFormat), 0, 2)
            mDataOutputStream!!.write(shortToByteArray(header.mNumChannel), 0, 2)
            mDataOutputStream!!.write(intToByteArray(header.mSampleRate), 0, 4)
            mDataOutputStream!!.write(intToByteArray(header.mByteRate), 0, 4)
            mDataOutputStream!!.write(shortToByteArray(header.mBlockAlign), 0, 2)
            mDataOutputStream!!.write(shortToByteArray(header.mBitsPerSample), 0, 2)
            mDataOutputStream!!.writeBytes(header.mSubChunk2ID)
            mDataOutputStream!!.write(intToByteArray(header.mSubChunk2Size), 0, 4)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        return true
    }

    private fun writeDataSize(): Boolean {
        if (mDataOutputStream == null) {
            return false
        }

        try {
            val wavFile = RandomAccessFile(mFilepath, "rw")
            wavFile.seek(WavFileHeader.WAV_CHUNKSIZE_OFFSET.toLong())
            wavFile.write(intToByteArray(mDataSize + WavFileHeader.WAV_CHUNKSIZE_EXCLUDE_DATA), 0, 4)
            wavFile.seek(WavFileHeader.WAV_SUB_CHUNKSIZE2_OFFSET.toLong())
            wavFile.write(intToByteArray(mDataSize), 0, 4)
            wavFile.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return false
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }

        return true
    }

    private fun intToByteArray(data: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(data).array()
    }

    private fun shortToByteArray(data: Short): ByteArray {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(data).array()
    }
}
