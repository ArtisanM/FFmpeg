package fm.qingting.audioeditor

class WavFileHeader {

    var mChunkID = "RIFF"
    var mChunkSize = 0
    var mFormat = "WAVE"

    var mSubChunk1ID = "fmt "
    var mSubChunk1Size = 16
    var mAudioFormat: Short = 1
    var mNumChannel: Short = 1
    var mSampleRate = 8000
    var mByteRate = 0
    var mBlockAlign: Short = 0
    var mBitsPerSample: Short = 8

    var mSubChunk2ID = "data"
    var mSubChunk2Size = 0

    constructor()

    constructor(sampleRateInHz: Int, channels: Int, bitsPerSample: Int) {
        mSampleRate = sampleRateInHz
        mBitsPerSample = bitsPerSample.toShort()
        mNumChannel = channels.toShort()
        mByteRate = mSampleRate * mNumChannel.toInt() * mBitsPerSample.toInt() / 8
        mBlockAlign = (mNumChannel * mBitsPerSample / 8).toShort()
    }

    companion object {
        val WAV_FILE_HEADER_SIZE = 44
        val WAV_CHUNKSIZE_EXCLUDE_DATA = 36

        val WAV_CHUNKSIZE_OFFSET = 4
        val WAV_SUB_CHUNKSIZE1_OFFSET = 16
        val WAV_SUB_CHUNKSIZE2_OFFSET = 40
    }
}
