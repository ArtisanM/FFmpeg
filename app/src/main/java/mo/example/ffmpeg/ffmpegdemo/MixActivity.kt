package mo.example.ffmpeg.ffmpegdemo

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.view.View
import fm.qingting.audioeditor.AudioRecorderImpl
import fm.qingting.audioeditor.IAudioRecorder
import fm.qingting.audioeditor.ITrack
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import java.io.File

class MixActivity : AppCompatActivity() {

    val recorder: IAudioRecorder = AudioRecorderImpl()
    val player = MediaPlayer()

    var track1: ITrack? = null
    var track2: ITrack? = null
    val mHeadSetSubject = BehaviorSubject.create<Boolean>()

    val file1 = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "track1.pcm"
    )

    val file2 = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "track2.pcm"
    )

    private val mReceiver = Receiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mix)

        registerReceiver(mReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))
        registerReceiver(mReceiver, IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED))

        track1 = recorder.addAudioTrack(file1)
        track2 = recorder.addAudioTrack(file2)
        track1!!.setVolume(0.1f)
        track2!!.setVolume(0.1f)
        track1!!.startRecord()
        track2!!.startRecord()

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
        unregisterReceiver(mReceiver)
    }


    private inner class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.apply {
                when (action) {
                    BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                        if (BluetoothAdapter.getDefaultAdapter().getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_DISCONNECTED) {
                            mHeadSetSubject.onNext(false)
                        } else if (BluetoothAdapter.getDefaultAdapter().getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED) {
                            mHeadSetSubject.onNext(true)
                        }
                    }
                    Intent.ACTION_HEADSET_PLUG -> {
                        when (getIntExtra("state", -1)) {
                            0 -> {
                                // unplugged
                                mHeadSetSubject.onNext(false)
                            }
                            1 -> {
                                // plugged
                                mHeadSetSubject.onNext(true)
                            }
                            else -> {
                                // unknown
                            }
                        }
                    }
                }
            }
        }

    }
}
