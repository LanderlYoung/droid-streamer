package io.github.landerlyoung.droidstreamer.service

import android.app.Service
import android.content.Intent
import android.media.MediaFormat
import android.os.Handler
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import io.github.landerlyoung.droidstreamer.Global
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * <pre>
 * Author: taylorcyang@tencent.com
 * Date:   2017-06-13
 * Time:   23:23
 * Life with Passion, Code with Creativity.
 * </pre>
 */
class StreamingService : Service(), Handler.Callback {
    private lateinit var messenger: Messenger
    private var streamManager: ScreenMirrorManager? = null
    private val callbackList: MutableList<Messenger> = mutableListOf()
    private val currentState: StreamState
        get() = StreamState(StreamingState.IDEL)


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
    }

    enum class StreamingState {
        IDEL,
        STREAMING;

        fun fromName(name: String) = try {
            StreamingState.valueOf(name)
        } catch (e: IllegalArgumentException) {
            IDEL
        }
    }

    override fun onCreate() {
        super.onCreate()
        messenger = Messenger(Handler(this))
    }

    override fun onBind(intent: Intent?) = messenger.binder!!

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
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
                msg.replyTo?.let {
                    callbackList.add(msg.replyTo)
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

    fun startStream(intent: Intent, resultCode: Int) {
        streamManager = ScreenMirrorManager.build {
            projection(resultCode, intent)
            dataSink(object : DataSink {
                var fileChannel: FileChannel? = null

                override fun onBufferAvailable(buffer: ByteBuffer, presentationTimeUs: Long, isKeyFrame: Boolean) {
                    Log.i(TAG, "onBufferAvailable  presentationTimeUs:$presentationTimeUs isKeyFrame:$isKeyFrame")

                    if (fileChannel == null) {
                        val output = File(Global.app.externalCacheDir, "cap_${System.currentTimeMillis()}.h264")
                        fileChannel = FileOutputStream(output)
                                .channel

                        Log.i(TAG, "create output file $output")
                    }
                    Log.i(TAG, "write buffer")
                    fileChannel?.write(buffer)
                }

                override fun onFormatChanged(format: MediaFormat) {
                    Log.i(TAG, "onFormatChanged: ")
                }

                override fun onError(e: Exception) {
                    Log.i(TAG, "onError")
                }
            }, Global.secondaryHandler)

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
    }

    private fun notifyState(state: StreamState, replyMessenger: Messenger?): Boolean {
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
