package io.github.landerlyoung.droidstreamer

import android.app.Application

/**
 * <pre>
 * Author: taylorcyang@tencent.com
 * Date:   2017-06-23
 * Time:   22:54
 * Life with Passion, Code with Creativity.
 * </pre>
 */
class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        Global.installApplication(this)
    }
}