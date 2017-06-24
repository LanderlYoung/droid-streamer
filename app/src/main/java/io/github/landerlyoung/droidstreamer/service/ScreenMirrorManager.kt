package io.github.landerlyoung.droidstreamer.service

import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import io.github.landerlyoung.droidstreamer.Global
import org.jetbrains.anko.mediaProjectionManager
import java.io.IOException

/**
 * <pre>
 * Author: taylorcyang@tencent.com
 * Date:   2017-06-23
 * Time:   21:40
 * Life with Passion, Code with Creativity.
 * </pre>
 */
class ScreenMirrorManager : VirtualDisplay.Callback {
    private val virtualDisplay: VirtualDisplay
    private val h264Codec: MediaCodec
    private val inputSurface: Surface

    constructor(
            projectionResultCode: Int,
            projectionIntent: Intent,
            width: Int,
            height: Int,
            dataSink: DataSink
    ) : super() {
        try {
            h264Codec = MediaCodec.createByCodecName(MediaFormat.MIMETYPE_VIDEO_AVC)
        } catch (e: IOException) {
            throw IllegalStateException("cannot create h264 encoder", e)
        }
        inputSurface = h264Codec.createInputSurface()
        h264Codec.setCallback(dataSink)

        virtualDisplay = Global.app.mediaProjectionManager
                .getMediaProjection(projectionResultCode, projectionIntent)
                .createVirtualDisplay("DroidStreamer",
                        width, height, 1,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        inputSurface,
                        this, null)
    }

    override fun onResumed() {
        super.onResumed()
    }

    override fun onStopped() {
        super.onStopped()
    }

    override fun onPaused() {
        super.onPaused()
    }
}

abstract class DataSink : MediaCodec.Callback() {

}
