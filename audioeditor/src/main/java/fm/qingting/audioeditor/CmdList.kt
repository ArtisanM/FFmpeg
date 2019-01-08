package fm.qingting.audioeditor

class CmdList: ArrayList<String>() {
    init {
        add("ffmpeg")
    }

    override fun toString(): String {
        val sb = StringBuilder()
        val iterator = iterator()
        while (iterator.hasNext()) {
            sb.append(" ${iterator.next()}")
        }
        return sb.toString()
    }
}