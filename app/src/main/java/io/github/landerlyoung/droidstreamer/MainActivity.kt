package io.github.landerlyoung.droidstreamer

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.widget.Button
import io.github.landerlyoung.droidstreamer.service.StreamState
import io.github.landerlyoung.droidstreamer.service.StreamingService
import org.jetbrains.anko.mediaProjectionManager
import org.jetbrains.anko.toast

class MainActivity : Activity(), ServiceConnection {

    companion object {
        const val TAG = "MainActivity"
        const val CREATE_SCREEN_CAPTURE_REQUEST_CODE: Int = 0xbabe
    }

    private lateinit var messenger: Messenger
    private val callbackMessenger = Messenger(Handler { msg ->
        when (msg.what) {
            StreamingService.MSG_UPDATE_STREAM_STATES -> {
                val state = msg.obj as? StreamState
                updateState(state)
            }
        }

        Log.i(TAG, "callbackMessenger msg:$msg")
        true
    })

    private var ready = false
    private var isStream = false


    // view
    private lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startButton = findViewById(R.id.stream_button) as Button
        startButton.setOnClickListener {
            toggleStreaming()
        }

        bindService(Intent(this, StreamingService::class.java), this, BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()

        val regMsg = Message.obtain()
        regMsg.what = StreamingService.MSG_UNREGISTER_CALLBACK
        regMsg.replyTo = messenger
        messenger.send(regMsg)

        unbindService(this)
    }

    override fun onServiceDisconnected(name: ComponentName) {
        ready = false
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        messenger = Messenger(service)

        val regMsg = Message.obtain()
        regMsg.what = StreamingService.MSG_REGISTER_CALLBACK
        regMsg.replyTo = callbackMessenger
        messenger.send(regMsg)

        ready = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_SCREEN_CAPTURE_REQUEST_CODE) {
            if (resultCode != RESULT_OK || data == null) {
                toast("denied")
            } else {
                startStreaming(resultCode, data)
            }
        }
    }

    private fun toggleStreaming() {
        if (!isStream) {
            // start
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),
                    CREATE_SCREEN_CAPTURE_REQUEST_CODE)
        } else {
            val msg = Message.obtain()
            msg.what = StreamingService.MSG_STOP_STREAMING
            msg.replyTo = callbackMessenger
            messenger.send(msg)
        }
    }

    private fun startStreaming(resultCode: Int, data: Intent) {
        val msg = Message.obtain()
        msg.what = StreamingService.MSG_START_STREAMING
        msg.obj = data
        msg.arg1 = resultCode
        msg.replyTo = callbackMessenger
        messenger.send(msg)
    }

    private fun updateState(state: StreamState?) {
        if (ready && state != null) {
            isStream = state.isStream
            startButton.isEnabled = true
            startButton.text = if (isStream) "Streaming" else "Start"
        }else {
            startButton.isEnabled = false
        }
    }
}
