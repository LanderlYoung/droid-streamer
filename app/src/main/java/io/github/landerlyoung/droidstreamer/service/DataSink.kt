package io.github.landerlyoung.droidstreamer.service

import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 * ```
 * Author: landerlyoung@gmail.com
 * Date:   2017-06-24
 * Time:   14:37
 * Life with Passion, Code with Creativity.
 * ```
 */
interface DataSink {
    /**
     * on raw video buffer available (h264/h265/etc..)
     */
    fun onBufferAvailable(buffer: ByteBuffer, presentationTimeUs: Long, isKeyFrame: Boolean)

    fun onEnd()

    /**
     * on output format change.
     * 1. resolution
     * 2. orientation
     * 3. codec
     * 4. etc..
     */
    fun onFormatChanged(format: MediaFormat)

    fun onError(e: Exception)
}

