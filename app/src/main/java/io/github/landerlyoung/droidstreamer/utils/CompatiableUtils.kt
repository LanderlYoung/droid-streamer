@file:Suppress("NOTHING_TO_INLINE")

package io.github.landerlyoung.droidstreamer.utils

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.os.Handler

/**
 * <pre>
 * Author: taylorcyang@tencent.com
 * Date:   2017-06-24
 * Time:   08:57
 * Life with Passion, Code with Creativity.
 * </pre>
 */
inline fun MediaCodec.setCallbackOnHandler(callback: MediaCodec.Callback?, handler: Handler?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        setCallback(callback, handler)
    } else if (handler == null || callback == null) {
        this.setCallback(callback)
    } else {
        setCallback(object : MediaCodec.Callback() {
            val MSG_ON_OUT_AVAILABLE = 0
            val MSG_ON_IN_AVAILABLE = 1
            val MSG_ON_OUT_FORMAT_CHANGE = 2
            val MSG_ON_ERROR = 3

            val callbackHandler = Handler(handler.looper) { msg ->
                when (msg.what) {
                    MSG_ON_OUT_AVAILABLE -> {
                        callback.onOutputBufferAvailable(this@setCallbackOnHandler, msg.arg1, msg.obj as MediaCodec.BufferInfo?)
                        true
                    }
                    MSG_ON_IN_AVAILABLE -> {
                        callback.onInputBufferAvailable(this@setCallbackOnHandler, msg.arg1)
                        true
                    }
                    MSG_ON_OUT_FORMAT_CHANGE -> {
                        callback.onOutputFormatChanged(this@setCallbackOnHandler, msg.obj as MediaFormat?)
                        true
                    }

                    MSG_ON_ERROR -> {
                        callback.onError(this@setCallbackOnHandler, msg.obj as MediaCodec.CodecException?)
                        true
                    }
                    else -> false
                }
            }

            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                callbackHandler.obtainMessage(MSG_ON_OUT_AVAILABLE, index, 0, info)
                        .sendToTarget()
            }

            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                callbackHandler.obtainMessage(MSG_ON_IN_AVAILABLE, index, 0)
                        .sendToTarget()
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                callbackHandler.obtainMessage(MSG_ON_OUT_FORMAT_CHANGE, format)
                        .sendToTarget()
            }

            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                callbackHandler.obtainMessage(MSG_ON_ERROR, e)
                        .sendToTarget()
            }
        })
    }
}
