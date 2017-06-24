package io.github.landerlyoung.droidstreamer

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import io.github.landerlyoung.droidstreamer.service.StreamingService
import org.jetbrains.anko.mediaProjectionManager
import org.jetbrains.anko.toast


class MainActivity : Activity(), ServiceConnection {

    companion object {
        const val TAG = "MainActivity"
        const val CREATE_SCREEN_CAPTURE_REQUEST_CODE: Int = 0xbabe
    }

    private var projectionIntent: Intent? = null
    private var projectionResultCode = 0

    private var messenger: Messenger? = null
    private val callbackMessenger = Messenger(Handler { msg ->

        Log.i(TAG, "callbackMessenger msg:$msg")
        true
    })


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById(R.id.stream_button)?.setOnClickListener { toggleStreaming() }

        bindService(Intent(this, StreamingService::class.java), this, BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(this)
    }

    override fun onServiceDisconnected(name: ComponentName) {
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        messenger = Messenger(service)

        projectionIntent?.let { intent ->
            startStreaming(projectionResultCode, intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_SCREEN_CAPTURE_REQUEST_CODE) {
            if (resultCode != RESULT_OK || data == null) {
                toast("denied")
            } else {
                if (!startStreaming(resultCode, data)) {
                    projectionIntent = data
                    projectionResultCode = resultCode
                }
            }
        }
    }

    private fun toggleStreaming() {
        // start
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), CREATE_SCREEN_CAPTURE_REQUEST_CODE)
    }

    private fun startStreaming(resultCode: Int, data: Intent): Boolean = messenger?.run { ->
        val msg = Message.obtain()
        msg.what = StreamingService.MSG_START_STREAMING
        msg.obj = data
        msg.arg1 = resultCode
        msg.replyTo = callbackMessenger
        send(msg)
        true
    } ?: false
}
