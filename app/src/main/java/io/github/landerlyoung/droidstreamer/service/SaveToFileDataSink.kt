package io.github.landerlyoung.droidstreamer.service

import android.media.MediaFormat
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * ```
 * Author: taylorcyang@tencent.com
 * Date:   2017-06-24
 * Time:   15:45
 * Life with Passion, Code with Creativity.
 * ```
 */
class SaveToFileDataSink(private val fileName: String) : DataSink {
    var fileChannel: FileChannel? = null

    override fun onBufferAvailable(buffer: ByteBuffer, presentationTimeUs: Long, isKeyFrame: Boolean) {
        Log.i(StreamingService.TAG, "onBufferAvailable  presentationTimeUs:$presentationTimeUs isKeyFrame:$isKeyFrame")

        if (fileChannel == null) {
            val output = File(fileName)
            fileChannel = FileOutputStream(output)
                    .channel

            Log.i(StreamingService.TAG, "create output file $output")
        }
        Log.i(StreamingService.TAG, "write buffer")
        fileChannel?.write(buffer)
    }

    override fun onEnd() {
        try {
            fileChannel?.close()
        } catch (ignore: IOException) {
        }
    }

    override fun onFormatChanged(format: MediaFormat) {
        Log.i(StreamingService.TAG, "onFormatChanged: ")
    }

    override fun onError(e: Exception) {
        Log.i(StreamingService.TAG, "onError")
    }
}
