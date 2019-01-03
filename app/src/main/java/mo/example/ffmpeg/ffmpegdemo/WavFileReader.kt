package mo.example.ffmpeg.ffmpegdemo

import android.util.Log

import java.io.DataInputStream
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavFileReader {

    private var mDataInputStream: DataInputStream? = null
    private var mWavFileHeader: WavFileHeader? = null

    @Throws(IOException::class)
    fun openFile(filepath: String): Boolean {
        if (mDataInputStream != null) {
            closeFile()
        }
        mDataInputStream = DataInputStream(FileInputStream(filepath))
        return readHeader()
    }

    @Throws(IOException::class)
    fun closeFile() {
        if (mDataInputStream != null) {
            mDataInputStream!!.close()
            mDataInputStream = null
        }
    }

    fun getmWavFileHeader(): WavFileHeader? {
        return mWavFileHeader
    }

    fun readData(buffer: ByteArray, offset: Int, count: Int): Int {
        if (mDataInputStream == null || mWavFileHeader == null) {
            return -1
        }

        try {
            val nbytes = mDataInputStream!!.read(buffer, offset, count)
            Log.e("audiorecorder", "read buffer $nbytes")
            return if (nbytes == -1) {
                0
            } else nbytes
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return -1
    }

    private fun readHeader(): Boolean {
        if (mDataInputStream == null) {
            return false
        }

        val header = WavFileHeader()

        val intValue = ByteArray(4)
        val shortValue = ByteArray(2)

        try {
            header.mChunkID = "" + mDataInputStream!!.readByte().toChar() + mDataInputStream!!.readByte().toChar() +
                    mDataInputStream!!.readByte().toChar() + mDataInputStream!!.readByte().toChar()
            Log.d(TAG, "Read file chunkID:" + header.mChunkID)

            mDataInputStream!!.read(intValue)
            header.mChunkSize = byteArrayToInt(intValue)
            Log.d(TAG, "Read file chunkSize:" + header.mChunkSize)

            header.mFormat = "" + mDataInputStream!!.readByte().toChar() + mDataInputStream!!.readByte().toChar() +
                    mDataInputStream!!.readByte().toChar() + mDataInputStream!!.readByte().toChar()
            Log.d(TAG, "Read file format:" + header.mFormat)

            header.mSubChunk1ID = "" + mDataInputStream!!.readByte().toChar() + mDataInputStream!!.readByte().toChar() +
                    mDataInputStream!!.readByte().toChar() + mDataInputStream!!.readByte().toChar()
            Log.d(TAG, "Read fmt chunkID:" + header.mSubChunk1ID)

            mDataInputStream!!.read(intValue)
            header.mSubChunk1Size = byteArrayToInt(intValue)
            Log.d(TAG, "Read fmt chunkSize:" + header.mSubChunk1Size)

            mDataInputStream!!.read(shortValue)
            header.mAudioFormat = byteArrayToShort(shortValue)
            Log.d(TAG, "Read audioFormat:" + header.mAudioFormat)

            mDataInputStream!!.read(shortValue)
            header.mNumChannel = byteArrayToShort(shortValue)
            Log.d(TAG, "Read channel number:" + header.mNumChannel)

            mDataInputStream!!.read(intValue)
            header.mSampleRate = byteArrayToInt(intValue)
            Log.d(TAG, "Read samplerate:" + header.mSampleRate)

            mDataInputStream!!.read(intValue)
            header.mByteRate = byteArrayToInt(intValue)
            Log.d(TAG, "Read byterate:" + header.mByteRate)

            mDataInputStream!!.read(shortValue)
            header.mBlockAlign = byteArrayToShort(shortValue)
            Log.d(TAG, "Read blockalign:" + header.mBlockAlign)

            mDataInputStream!!.read(shortValue)
            header.mBitsPerSample = byteArrayToShort(shortValue)
            Log.d(TAG, "Read bitspersample:" + header.mBitsPerSample)

            header.mSubChunk2ID = "" + mDataInputStream!!.readByte().toChar() + mDataInputStream!!.readByte().toChar() +
                    mDataInputStream!!.readByte().toChar() + mDataInputStream!!.readByte().toChar()
            Log.d(TAG, "Read data chunkID:" + header.mSubChunk2ID)

            mDataInputStream!!.read(intValue)
            header.mSubChunk2Size = byteArrayToInt(intValue)
            Log.d(TAG, "Read data chunkSize:" + header.mSubChunk2Size)

            Log.d(TAG, "Read wav file success !")
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        mWavFileHeader = header

        return true
    }

    companion object {
        private val TAG = WavFileReader::class.java.simpleName

        private fun byteArrayToShort(b: ByteArray): Short {
            return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).short
        }

        private fun byteArrayToInt(b: ByteArray): Int {
            return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).int
        }
    }
}
