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
    private val _app: AtomicReference<Application> = AtomicReference()
    val secondaryThread = HandlerThread("SecondaryThread")
    val secondaryThreadHandler = Handler(secondaryThread.looper)

    val app: Application
        get() = _app.get() ?: throw IllegalStateException("application is not set")


    fun installApplication(app: Application) {
        if (!_app.compareAndSet(null, app)) {
            throw IllegalStateException("can set application only once")
        }
    }
}
