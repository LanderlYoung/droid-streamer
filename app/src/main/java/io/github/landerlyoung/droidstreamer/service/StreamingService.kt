package io.github.landerlyoung.droidstreamer.service

import android.app.Service
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Handler
import android.os.Messenger
import android.util.Log

/**
 * <pre>
 * Author: taylorcyang@tencent.com
 * Date:   2017-06-13
 * Time:   23:23
 * Life with Passion, Code with Creativity.
 * </pre>
 */
class StreamingService : Service() {
    lateinit var messenger: Messenger

    companion object {
        const val TAG = "StreamingService"
        const val MSG_GET_STREAMING_UTL = 0
        const val MSG_GET_CURRENT_STATUS = 1
        const val MSG_START_STREAMING = 2
    }

    enum class StreamingState {
        IDEL,
        STARTING,
        STREAMING,
    }

    override fun onCreate() {
        super.onCreate()
        messenger = Messenger(Handler { msg ->
            when (msg.what) {
                MSG_GET_STREAMING_UTL -> {
                }
                MSG_GET_CURRENT_STATUS -> {
                }
                MSG_START_STREAMING -> {
                    ScreenMirrorManager(msg.arg1, msg.obj as Intent, 720, 1080, object : DataSink() {
                        override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                            Log.i(TAG, "StreamingService")
                        }

                        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                        }

                        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                        }

                        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                        }
                    })
                }
                else -> return@Handler false
            }

            Log.i(TAG, "handle msg $msg")
            return@Handler true
        })
    }

    override fun onBind(intent: Intent?) = messenger.binder!!

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }
}
