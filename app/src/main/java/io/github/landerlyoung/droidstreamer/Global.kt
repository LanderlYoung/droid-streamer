package io.github.landerlyoung.droidstreamer

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * <pre>
 * Author: landerlyoung@gmail.com
 * Date:   2017-06-23
 * Time:   22:54
 * Life with Passion, Code with Creativity.
 * </pre>
 */
object Global {
    private val mApp: AtomicReference<DroidApplication> = AtomicReference()

    val app: DroidApplication
        get() = mApp.get() ?: throw IllegalStateException("application is not set")

    private val mSecondaryThread: HandlerThread
    val secondaryHandler: Handler

    val mainHandler = Handler(Looper.getMainLooper())

    val ioThreadPool: ExecutorService

    init {
        mSecondaryThread = HandlerThread("SecondaryThread")
        mSecondaryThread.start()
        secondaryHandler = Handler(mSecondaryThread.looper)
        ioThreadPool = ThreadPoolExecutor(2, 2, 60, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>())
                .apply { allowCoreThreadTimeOut(true) }
    }

    fun installApplication(app: DroidApplication) {
        if (!this.mApp.compareAndSet(null, app)) {
            throw IllegalStateException("can set application only once")
        }
    }
}
