package io.github.landerlyoung.droidstreamer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import io.github.landerlyoung.droidstreamer.Global
import io.github.landerlyoung.droidstreamer.R
import org.jetbrains.anko.notificationManager

/**
 * ```
 * Author: landerlyoung@gmail.com
 * Date:   2017-06-13
 * Time:   23:23
 * Life with Passion, Code with Creativity.
 * ```
 */
class StreamingService : Service(), Handler.Callback {
    private lateinit var messenger: Messenger
    private var streamManager: ScreenMirrorManager? = null
    private val callbackList: MutableList<Messenger> = mutableListOf()
    private val currentState: StreamState
        get() = StreamState(streamManager != null)

    companion object {
        const val TAG = "StreamingService"
        const val MSG_GET_STREAMING_UTL = 0
        const val MSG_GET_CURRENT_STATUS = 1
        const val MSG_START_STREAMING = 2
        const val MSG_STOP_STREAMING = 3

        const val MSG_REGISTER_CALLBACK = 4
        const val MSG_UNREGISTER_CALLBACK = 5

        // callback
        const val MSG_UPDATE_STREAM_STATES = 6

        const val NOTIFICATION_ID = 100
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        messenger = Messenger(Handler(this))
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.i(TAG, "onBind: $intent")
        return messenger.binder!!
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "onUnbind: $intent")
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand: $intent")
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")

        streamManager?.release()
    }

    private fun startForeground() {
        notificationManager.createNotificationChannel(
                NotificationChannel("FOR", "FOR", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val noti = Notification.Builder(this, "FOR")
                .setContentTitle("Streaming")
                .setContentText("Click to see more details")
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", null)
                .setSmallIcon(R.drawable.ic_cast_connected)
                .setLargeIcon((resources.getDrawable(R.mipmap.ic_launcher_round) as BitmapDrawable).bitmap)
                .build()
        startForeground(NOTIFICATION_ID, noti)
    }

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            MSG_GET_STREAMING_UTL -> {
            }
            MSG_GET_CURRENT_STATUS -> {
            }
            MSG_START_STREAMING -> {
                msg.obj?.run {
                    startStream(this as Intent, msg.arg1)
                }
                notifyState(currentState, msg.replyTo)
            }
            MSG_STOP_STREAMING -> {
                stopStream()
                notifyState(currentState, msg.replyTo)
            }
            MSG_REGISTER_CALLBACK -> {
                Log.i(TAG, "MSG_REGISTER_CALLBACK: ${msg.replyTo} ")

                msg.replyTo?.let {
                    callbackList.add(msg.replyTo)
                    notifyState(currentState, msg.replyTo)
                }
            }
            MSG_UNREGISTER_CALLBACK -> {
                msg.replyTo?.let {
                    callbackList.remove(msg.replyTo)
                }
            }
            else -> return false
        }

        Log.i(TAG, "handle msg $msg")
        return true
    }

    private fun startStream(intent: Intent, resultCode: Int) {
        Log.i(TAG, "startStream")
        startService(Intent(this, StreamingService::class.java))
        startForeground()

        streamManager = ScreenMirrorManager.build {
            projection(resultCode, intent)
//            dataSink(SaveToFileDataSink("${Global.app.externalCacheDir}${File.separator}cap_${System.currentTimeMillis()}.h264"),
//                    Global.secondaryHandler)
            dataSink(TcpDataSink(), Global.secondaryHandler)

            streamStopListener {
                stopStream()
            }

            streamSize(720, 1280)
        }
    }

    private fun stopStream() {
        streamManager?.release()
        streamManager = null

        broadcastState()

        stopForeground(true)
        stopSelf()
    }

    private fun notifyState(state: StreamState, replyMessenger: Messenger?): Boolean {
        Log.i(TAG, "notifyState: ")
        
        replyMessenger?.let {
            try {
                val msg = Message.obtain()
                msg.what = MSG_UPDATE_STREAM_STATES
                msg.obj = state
                replyMessenger.send(msg)
                return true
            } catch (ignore: RemoteException) {
            }
        }
        return false
    }

    private fun broadcastState() {
        val state = currentState
        val it = callbackList.listIterator()
        while (it.hasNext()) {
            if (!notifyState(state, it.next())) {
                it.remove()
            }
        }
    }
}
