package io.github.landerlyoung.droidstreamer.service

import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 * ```
 * Author: taylorcyang@tencent.com
 * Date:   2017-06-24
 * Time:   14:37
 * Life with Passion, Code with Creativity.
 * ```
 */
interface DataSink {
    fun onBufferAvailable(buffer: ByteBuffer, presentationTimeUs: Long, isKeyFrame: Boolean)

    fun onEnd()

    fun onFormatChanged(format: MediaFormat)

    fun onError(e: Exception)
}

