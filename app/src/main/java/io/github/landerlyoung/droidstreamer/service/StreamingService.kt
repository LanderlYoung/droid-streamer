package io.github.landerlyoung.droidstreamer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.*
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import io.github.landerlyoung.droidstreamer.Global
import io.github.landerlyoung.droidstreamer.R
import io.github.landerlyoung.droidstreamer.service.server.HttpServer
import org.jetbrains.anko.notificationManager
import java.io.File
import java.net.Inet4Address
import java.util.concurrent.atomic.AtomicInteger

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
    private var httpServer: HttpServer? = null
    private val callbackList: MutableList<Messenger> = mutableListOf()
    private val currentState: StreamState
        get() = StreamState(streamManager != null)

    private val startStreamCommandId = AtomicInteger()

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

        private const val KEY_PROJECTION_INTENT = "projection_intent"
        private const val KEY_PROJECTION_RESULT_CODE = "projection_intent_result_code"
        private const val KEY_START_STREAM = "start_stream"
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
        if (intent != null && intent.getBooleanExtra(KEY_START_STREAM, false)) {
            if (startStreamCommandId.compareAndSet(0, startId)) {
                startForeground()
                performStartStream(intent)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")

        streamManager?.release()
    }

    private fun startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(NotificationChannel("FOR", "FOR", NotificationManager.IMPORTANCE_LOW))
        }

        NotificationCompat.Builder(this, "FOR")
                .setContentTitle("Streaming")
                .setContentText("Click to see more details")
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", null)
                .setSmallIcon(R.drawable.ic_cast_connected)
                .setLargeIcon((ContextCompat.getDrawable(this, R.mipmap.ic_launcher_round) as BitmapDrawable).bitmap)
                .build()
                .also {
                    startForeground(NOTIFICATION_ID, it)
                }
    }

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            MSG_GET_STREAMING_UTL -> {
            }
            MSG_GET_CURRENT_STATUS -> {
            }
            MSG_START_STREAMING -> {
                msg.obj?.run {
                    requestStartStream(this as Intent, msg.arg1)
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

    private fun requestStartStream(projectionIntent: Intent, resultCode: Int) {
        Log.i(TAG, "requestStartStream")

        ContextCompat.startForegroundService(this, Intent(this, StreamingService::class.java).also {
            it.putExtra(KEY_PROJECTION_INTENT, projectionIntent)
            it.putExtra(KEY_PROJECTION_RESULT_CODE, resultCode)
            it.putExtra(KEY_START_STREAM, true)
        })

    }

    private fun performStartStream(startIntent: Intent) {
        val projectionIntent: Intent? = startIntent.getParcelableExtra(KEY_PROJECTION_INTENT)
        val resultCode: Int = startIntent.getIntExtra(KEY_PROJECTION_RESULT_CODE, 0)

        Global.secondaryHandler.post {
            stopStream()
            if (projectionIntent != null) {
                stopStream()
                val server = startHttpServer()
                val stream = startScreenStream(projectionIntent, resultCode, server)

                notifyStreaming(server, stream)
            }
        }
    }

    private fun startHttpServer() = HttpServer().also {
        httpServer = it
    }

    private fun startScreenStream(projectionIntent: Intent, resultCode: Int, server: HttpServer) =
            ScreenMirrorManager.build {
                projection(resultCode, projectionIntent)
                dataSink(SaveToFileDataSink(File(getExternalFilesDir("flv_video"), "h264.raw").absolutePath),
                        Global.secondaryHandler)
                //            dataSink(TcpDataSink(), Global.secondaryHandler)
                // dataSink to HttpServer
//
//                dataSink(FlvMuxer(FileOutputStream(
//                        File(getExternalFilesDir("flv_video"), "flv_mine.flv")
//                )), Global.secondaryHandler)

//                dataSink(server.h264DataSink, Global.secondaryHandler)

                streamStopListener {
                    stopStream()
                }

                streamSize(720, 1280)
            }.also {
                streamManager = it
            }

    private fun stopStream() {
        val startCommandId = startStreamCommandId.get()
        if (startCommandId != 0) {
            startStreamCommandId.set(0)
        }
        httpServer?.stopServer()
        streamManager?.release()
        streamManager = null

        broadcastState()

        stopForeground(true)
        stopSelf(startCommandId)
    }

    private fun notifyStreaming(server: HttpServer, stream: ScreenMirrorManager) {
        Log.i(TAG, "notifyStreaming httpServer port:${server.listeningPort} localIp:${getNetworkInterfaceIpAddress().filter { it is Inet4Address }}")
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
