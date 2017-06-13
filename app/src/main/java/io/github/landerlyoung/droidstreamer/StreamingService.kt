package io.github.landerlyoung.droidstreamer

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.Messenger
import android.os.RemoteException
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

    companion object X{
        const val TAG = "StreamingService"
        const val MSG_GET_STREAMING_UTL = 0
        const val MSG_GET_CURRENT_STATUS = 1
    }

    override fun onCreate() {
        super.onCreate()
        messenger = Messenger(Handler { msg ->
            if (msg.replyTo != null) {
                try {
                    when (msg.what) {
                        MSG_GET_STREAMING_UTL -> {
                        }
                        MSG_GET_CURRENT_STATUS -> {
                        }
                        else -> return@Handler false
                    }
                    return@Handler true
                } catch (e: RemoteException) {
                    Log.e(TAG, "error in handle message", e)
                }
            }
            return@Handler false
        })
    }

    override fun onBind(intent: Intent?) = messenger.binder!!

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

}
