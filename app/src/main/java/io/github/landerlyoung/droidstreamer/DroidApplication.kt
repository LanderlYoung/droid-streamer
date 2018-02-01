package io.github.landerlyoung.droidstreamer

import android.app.Application

/**
 * <pre>
 * Author: landerlyoung@gmail.com
 * Date:   2017-06-23
 * Time:   22:54
 * Life with Passion, Code with Creativity.
 * </pre>
 */
class DroidApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Global.installApplication(this)
    }
}