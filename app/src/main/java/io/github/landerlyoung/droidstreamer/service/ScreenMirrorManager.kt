package io.github.landerlyoung.droidstreamer.service

import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.os.Bundle
import android.os.Handler
import android.support.annotation.IntRange
import android.view.Surface
import io.github.landerlyoung.droidstreamer.Global
import io.github.landerlyoung.droidstreamer.utils.setCallbackOnHandler
import org.jetbrains.anko.mediaProjectionManager
import java.io.IOException

/**
 * ```
 * Author: landerlyoung@gmail.com
 * Date:   2017-06-23
 * Time:   21:40
 * Life with Passion, Code with Creativity.
 * ```
 */
class ScreenMirrorManager
private constructor(projectionResultCode: Int,
                    projectionIntent: Intent,
                    width: Int, height: Int,
                    val dataSink: DataSink,
                    val callbackHandler: Handler?,
                    val onStreamStopListener: OnStreamStopListener?) {

    private val virtualDisplay: VirtualDisplay
    private val videoEncoder: MediaCodec
    private val mediaProjection: MediaProjection
    private val inputSurface: Surface
    private val displayCallback: VirtualDisplay.Callback? = null
    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            onStreamStopListener?.invoke()
        }
    }

    init {
        mediaProjection = Global.app.mediaProjectionManager
                .getMediaProjection(projectionResultCode, projectionIntent)

        if (mediaProjection == null) {
            throw IllegalArgumentException("cannot obtain MediaProjection")
        }
        mediaProjection.registerCallback(projectionCallback, Global.mainHandler)

        try {
            videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        } catch (e: IOException) {
            throw IllegalStateException("cannot create h264 encoder", e)
        }
        videoEncoder.setCallbackOnHandler(DataSinkAdapter(), callbackHandler)
        val format =  MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
        format.setInteger(MediaFormat.KEY_BIT_RATE, 4 * 1000 * 1000)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 24)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        videoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        inputSurface = videoEncoder.createInputSurface()

        virtualDisplay = mediaProjection
                .createVirtualDisplay("DroidStreamer",
                        width, height, 1,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        inputSurface,
                        displayCallback, Global.mainHandler)

        videoEncoder.start()
    }

    fun yieldKeyFrame(): Boolean {
        val param = Bundle()
        param.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
        return setParam(param)
    }

    fun changeBitRate(
            @IntRange(from = 1, to = Integer.MAX_VALUE.toLong())
            newBitRage: Int): Boolean {
        val param = Bundle()
        param.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, newBitRage)
        return setParam(param)
    }

    fun pauseEncoder(): Boolean {
        val param = Bundle()
        param.putInt(MediaCodec.PARAMETER_KEY_SUSPEND, 1)
        return setParam(param)
    }

    fun resumeEncoder(): Boolean {
        val param = Bundle()
        param.putInt(MediaCodec.PARAMETER_KEY_SUSPEND, 0)
        return setParam(param)
    }

    private fun setParam(param: Bundle): Boolean {
        try {
            videoEncoder.setParameters(param)
            return true
        } catch (e: IllegalStateException) {
            return false
        }
    }

    fun release() {
        callbackHandler?.let {
            callbackHandler.post {
                dataSink.onEnd()
            }
        } ?: dataSink.onEnd()

        mediaProjection.stop()
        virtualDisplay.release()
        videoEncoder.stop()
        videoEncoder.release()
    }

    private inner class DataSinkAdapter : MediaCodec.Callback() {
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            // ignore
        }

        override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
            val byteBuffer = codec.getOutputBuffer(index)
            dataSink.onBufferAvailable(byteBuffer,
                    info.presentationTimeUs,
                    (info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0)
            codec.releaseOutputBuffer(index, false)
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            dataSink.onFormatChanged(format)
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            dataSink.onError(e)

            onStreamStopListener?.invoke()
        }
    }

    class Builder {
        private var projectionResultCode: Int = 0

        private var projectionIntent: Intent? = null
        private var width: Int = 0

        private var height: Int = 0
        private var dataSink: DataSink? = null

        private var callbackHandler: Handler? = null
        private var streamStopListener: OnStreamStopListener? = null

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

        fun streamStopListener(streamStopListener: OnStreamStopListener) = apply {
            this.streamStopListener = streamStopListener
        }

        fun build(): ScreenMirrorManager {
            val intent = projectionIntent ?: throw IllegalStateException()
            val sink = this.dataSink ?: throw IllegalStateException()

            if (width <= 0 || height <= 0) {
                throw IllegalArgumentException("width <= 0 || height <= 0")
            }

            return ScreenMirrorManager(projectionResultCode,
                    intent, width, height,
                    sink,
                    callbackHandler,
                    streamStopListener)
        }
    }

    companion object {
        inline fun build(block: Builder.() -> Unit): ScreenMirrorManager {
            return Builder().apply(block).build()
        }
    }
}

typealias OnStreamStopListener = () -> Unit
