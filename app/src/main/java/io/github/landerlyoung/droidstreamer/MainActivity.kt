package io.github.landerlyoung.droidstreamer

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.IBinder
import android.view.SurfaceView
import org.jetbrains.anko.mediaProjectionManager
import org.jetbrains.anko.toast


class MainActivity : Activity(), ServiceConnection {

    companion object {
        const val CREATE_SCREEN_CAPTURE_REQUEST_CODE: Int = 0xbabe
    }

    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var mediaProjection: MediaProjection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById(R.id.stream_button)?.setOnClickListener { toggleStreaming() }

        projectionManager = mediaProjectionManager

        bindService(Intent(this, StreamingService::class.java), this, BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(this)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        TODO("not implemented")
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        TODO("not implemented")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_SCREEN_CAPTURE_REQUEST_CODE) {
            if (resultCode != RESULT_OK || data == null) {
                toast("denied")
            } else {
                startStreaming(resultCode, data)
            }
        }
    }

    private fun toggleStreaming() {
        // start
        startActivityForResult(projectionManager.createScreenCaptureIntent(), CREATE_SCREEN_CAPTURE_REQUEST_CODE)
    }

    private fun startStreaming(resultCode: Int, data: Intent) {
        val proj: MediaProjection? = projectionManager.getMediaProjection(resultCode, data)

        proj?.let {
            mediaProjection = proj
            createVirtualDisplay()
            return
        }
    }

    private fun createVirtualDisplay(): VirtualDisplay {
        val surface = findViewById(R.id.surface) as SurfaceView

        return mediaProjection.createVirtualDisplay(
                "ScreenSharingDemo",
                720, 1080, 1,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface.holder.surface,
                null /*Handler*/, null) /*Callbacks*/
    }

}
