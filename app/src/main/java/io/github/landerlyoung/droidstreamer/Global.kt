package io.github.landerlyoung.droidstreamer

import android.os.Handler
import android.os.HandlerThread
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
    val secondaryThread: HandlerThread
    val secondaryThreadHandler: Handler

    init {
        secondaryThread = HandlerThread("SecondaryThread")
        secondaryThread.start()
        secondaryThreadHandler = Handler(secondaryThread.looper)
    }

    val app: DroidApplication
        get() = _app.get() ?: throw IllegalStateException("application is not set")


    fun installApplication(app: DroidApplication) {
        if (!_app.compareAndSet(null, app)) {
            throw IllegalStateException("can set application only once")
        }
    }
}
