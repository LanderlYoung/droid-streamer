package io.github.landerlyoung.droidstreamer.service

import android.app.Service
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Handler
import android.os.Messenger
import android.util.Log
import io.github.landerlyoung.droidstreamer.Global
import java.io.File
import java.io.FileOutputStream
import java.nio.channels.FileChannel

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
                    msg.obj.run {
                        startStreaming(this as Intent, msg.arg1)
                    }
                }
                else -> return@Handler false
            }

            Log.i(TAG, "handle msg $msg")
            return@Handler true
        })
    }

    var mgr: Any? = null

    fun startStreaming(intent: Intent, resultCode: Int) {
        mgr = ScreenMirrorManager.build {
            projection(resultCode, intent)
            dataSink(object : DataSink() {
                var fileChannel: FileChannel? = null
                override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                    Log.i(TAG, "onInputBufferAvailable: ")
                }

                override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                    Log.i(TAG, "onOutputBufferAvailable index:$index " +
                            "flag:${info.flags} " +
                            "isKey:${info.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME}")

                    if (fileChannel == null) {
                        val output = File(Global.app.externalCacheDir, "cap_${System.currentTimeMillis()}.h264")
                        fileChannel = FileOutputStream(output)
                                .channel

                        Log.i(TAG, "create output file $output")
                    }
                    val buffer = codec.getOutputBuffer(index)
                    Log.i(TAG, "write buffer")
                    fileChannel?.write(buffer)
                    codec.releaseOutputBuffer(index, false)
                }

                override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                    Log.i(TAG, "onOutputFormatChanged: ")
                }

                override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                    Log.i(TAG, "onError")
                }

            }, Global.secondaryHandler)

            streamSize(720, 1280)
        }
    }

    override fun onBind(intent: Intent?) = messenger.binder!!

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }
}
