package io.github.landerlyoung.droidstreamer.service

import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Handler
import android.view.Surface
import io.github.landerlyoung.droidstreamer.Global
import io.github.landerlyoung.droidstreamer.utils.setCallbackOnHandler
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
class ScreenMirrorManager
private constructor(projectionResultCode: Int,
                    projectionIntent: Intent,
                    width: Int, height: Int,
                    dataSink: DataSink, callbackHandler: Handler?) {

    private val virtualDisplay: VirtualDisplay
    private val h264Codec: MediaCodec
    private val inputSurface: Surface
    private val displayCallback: VirtualDisplay.Callback? = null

    init {
        try {
            h264Codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        } catch (e: IOException) {
            throw IllegalStateException("cannot create h264 encoder", e)
        }
        h264Codec.setCallbackOnHandler(dataSink, callbackHandler)
        val format =  MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
        format.setInteger(MediaFormat.KEY_BIT_RATE, 1000 * 1000)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 24)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        h264Codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        inputSurface = h264Codec.createInputSurface()

        virtualDisplay = Global.app.mediaProjectionManager
                .getMediaProjection(projectionResultCode, projectionIntent)
                .createVirtualDisplay("DroidStreamer",
                        width, height, 1,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        inputSurface,
                        displayCallback, null)

        h264Codec.start()
    }

    class Builder {
        private var projectionResultCode: Int = 0

        private var projectionIntent: Intent? = null
        private var width: Int = 0

        private var height: Int = 0
        private var dataSink: DataSink? = null

        private var callbackHandler: Handler? = null

        fun projection(resultCode: Int, resultIntent: Intent) = apply {
            projectionResultCode = resultCode
            projectionIntent = resultIntent
        }

        fun streamSize(width: Int, height: Int) = apply {
            if (width <= 0 || height <= 0) {
                throw IllegalArgumentException("width <= 0 || height <= 0")
            }
            this.width = width
            this.height = height
        }

        fun dataSink(dataSink: DataSink?, callbackHandler: Handler?) = apply {
            this.dataSink = dataSink
            this.callbackHandler = callbackHandler
        }

        fun build(): ScreenMirrorManager {
            val intent = projectionIntent ?: throw IllegalStateException()
            val sink = this.dataSink ?: throw IllegalStateException()

            if (width <= 0 || height <= 0) {
                throw IllegalArgumentException("width <= 0 || height <= 0")
            }

            return ScreenMirrorManager(projectionResultCode,
                    intent, width, height,
                    sink, callbackHandler)
        }

    }

    companion object {
        inline fun build(block: Builder.() -> Unit): ScreenMirrorManager {
            return Builder().apply(block).build()
        }

    }
}

abstract class DataSink : MediaCodec.Callback() {
    override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {}

    override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {}

    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {}

    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {}
}
