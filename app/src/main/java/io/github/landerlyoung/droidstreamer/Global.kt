package io.github.landerlyoung.droidstreamer

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import java.util.concurrent.atomic.AtomicReference

/**
 * <pre>
 * Author: taylorcyang@tencent.com
 * Date:   2017-06-23
 * Time:   22:54
 * Life with Passion, Code with Creativity.
 * </pre>
 */
object Global {
    private val _app: AtomicReference<DroidApplication> = AtomicReference()

    val app: DroidApplication
        get() = _app.get() ?: throw IllegalStateException("application is not set")
    val secondaryThread: HandlerThread
    val secondaryHandler: Handler
    val mainHandler = Handler(Looper.getMainLooper())

    init {
        secondaryThread = HandlerThread("SecondaryThread")
        secondaryThread.start()
        secondaryHandler = Handler(secondaryThread.looper)
    }

    fun installApplication(app: DroidApplication) {
        if (!_app.compareAndSet(null, app)) {
            throw IllegalStateException("can set application only once")
        }
    }
}
