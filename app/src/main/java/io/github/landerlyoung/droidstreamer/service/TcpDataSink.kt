package io.github.landerlyoung.droidstreamer.service

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import java.io.OutputStream
import java.net.Inet4Address
import java.net.ServerSocket
import java.nio.ByteBuffer


/**
 * ```
 * Author: landerlyoung@gmail.com
 * Date:   2017-06-24
 * Time:   16:08
 * Life with Passion, Code with Creativity.
 * ```
 */
class TcpDataSink : DataSink {
    companion object {
        const val TAG = "TcpDataSink"
        private const val port = 8888
    }

    init {
    }

    var output: OutputStream? = null

    override fun onBufferAvailable(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (output == null) {
            Log.i(TAG, "listen port:$port ip:${getNetworkInterfaceIpAddress().filter { it is Inet4Address }}")
            val socket = ServerSocket(port).accept()
            output = socket.getOutputStream()
            Log.i(TAG, "get client $output")
        }

        val out = output!!

        Log.i(TAG, "onBufferAvailable  ${buffer.remaining()}@${info.presentationTimeUs}")

        while (buffer.remaining() > 0) {
            out.write(buffer.get().toInt())
        }
        out.flush()
    }

    override fun onEnd() {
        output?.use {
            // close finally
        }
    }

    override fun onFormatChanged(format: MediaFormat) {
    }

    override fun onError(e: Exception) {
    }
}