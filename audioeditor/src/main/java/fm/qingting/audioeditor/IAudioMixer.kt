package fm.qingting.audioeditor

interface IAudioMixer {

    /**
     * 音轨合成
     * 合成后的数据将复写到第一条音轨所在数组
     *
     * @param data 音轨数据数组
     * @param lengthArray 记录每个音轨数据length的数组
     * @param volume 调节每条音轨音量大小，默认都为1，范围(0,10]
     *
     * @return  the total number of bytes mixed and written into the data[0]
     */
    fun mix(data: Array<ByteArray>, lengthArray: IntArray, volume: FloatArray? = null): Int

    fun trapVolume(volumes: FloatArray) {
        for (i in 0 until volumes.size) {
            if (volumes[i] <= 0) {
                volumes[i] = 0f
            }
            if (volumes[i] > 10) {
                volumes[i] = 10f
            }
        }
    }
}