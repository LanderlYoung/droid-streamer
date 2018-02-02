package io.github.landerlyoung.droidstreamer.service

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * ```
 * Author: landerlyoung@gmail.com
 * Date:   2017-06-24
 * Time:   15:45
 * Life with Passion, Code with Creativity.
 * ```
 */
class SaveToFileDataSink(private val fileName: String) : DataSink {
    var fileChannel: FileChannel? = null

    companion object {
        const val TAG = "SaveToFileDataSink"
    }

    override fun onBufferAvailable(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        Log.i(TAG, "onBufferAvailable  presentationTimeUs:${info.presentationTimeUs}")

        if (fileChannel == null) {
            val output = File(fileName)
            fileChannel = FileOutputStream(output)
                    .channel

            Log.i(TAG, "create output file $output")
        }
        Log.i(TAG, "write buffer")
        fileChannel?.write(buffer)
    }

    override fun onEnd() {
        try {
            fileChannel?.close()
        } catch (ignore: IOException) {
        }
    }

    override fun onFormatChanged(format: MediaFormat) {
        Log.i(TAG, "onFormatChanged: ")
    }

    override fun onError(e: Exception) {
        Log.i(TAG, "onError")
    }
}
