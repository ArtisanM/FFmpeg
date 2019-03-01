package mo.example.ffmpeg.ffmpegdemo

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.view.View
import fm.qingting.audioeditor.AudioRecorderImpl
import fm.qingting.audioeditor.IAudioRecorder
import fm.qingting.audioeditor.ITrack
import io.reactivex.android.schedulers.AndroidSchedulers
import java.io.File

class MixActivity : AppCompatActivity() {

    val recorder: IAudioRecorder = AudioRecorderImpl()
    val player = MediaPlayer()

    var track1: ITrack? = null
    var track2: ITrack? = null

    val file1 = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "track1.pcm"
    )

    val file2 = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "track2.pcm"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mix)

        track1 = recorder.addAudioTrack(file1)
        track2 = recorder.addAudioTrack(file2)
        track1!!.setVolume(0.1f)
        track2!!.setVolume(0.1f)
        track1!!.startPlay()
        track2!!.startPlay()

        player.reset()
        player.setDataSource(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "RightNow.mp3").absolutePath)
        player.prepare()
        player.start()
    }

    fun toggle(view: View) {
        recorder.toggle()
    }

    fun delTrack1(view: View) {
        recorder.removeAudioTrack(file1)
    }

    fun delTrack2(view: View) {
        recorder.removeAudioTrack(file2)
    }

    fun shiting(view: View) {
        recorder.stopRecord()
        recorder.getAudio().observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                player.reset()
                player.setDataSource(it.absolutePath)
                player.prepare()
                player.start()
            }, {it.printStackTrace()})
    }

    override fun onDestroy() {
        super.onDestroy()
        recorder.release()
        player.release()
    }
}
