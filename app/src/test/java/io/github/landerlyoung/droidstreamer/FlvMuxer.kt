package io.github.landerlyoung.droidstreamer

import android.media.MediaCodec
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * ```
 * Author: taylorcyang@tencent.com
 * Date:   2018-02-02
 * Time:   11:09
 * Life with Passion, Code with Creativity.
 * ```
 */
class FlvMuxer(output: OutputStream) {
    private val sink = DataOutputStream(output)
    private var headerWritten = false
    private lateinit var bytes: ByteArray

    @Volatile
    var isWriting = false

    fun onBufferAvailable(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        synchronized(this) {
            try {
                isWriting = true
                if (!headerWritten) {
                    writeHeader(sink)
                    headerWritten = true
                    initialTime = info.presentationTimeUs
                }
                val length = buffer.remaining()
                writeVideoTag(copyByteBuffer(buffer), length, info)
            } finally {
                isWriting = false
            }
        }
    }

    var initialTime = 0.toLong()

    private fun writeVideoTag(avcPacket: ByteArray, length: Int, info: MediaCodec.BufferInfo) {
        val presentationTimeUs = info.presentationTimeUs - initialTime
        val isKeyFrame = info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0
        val isCsd = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
        val isEndOfStream = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0

        if (isCsd) {
            // don't write csd
            return
        }

        val flagSize = 5 // for AVC VIDEO
        sink.writeByte(9) // tagType
        sink.write24(length + flagSize) // dataSize
        val timeMillis = (presentationTimeUs / 1000).toInt()
        sink.write24(timeMillis and 0xFF_FF_FF) // timestamp
        sink.writeByte((timeMillis shr 24) and 0x7F) // timestampExtended
        sink.write24(0) // streamId

        // VIDEO DATA
        val frameType = if (isKeyFrame) 1 else 2
        val codecId = 7 // AVC
        val flag = (frameType shl 4) or codecId
        sink.writeByte(flag)
        // packet
        sink.writeByte(when { // packetType |
            isEndOfStream -> 2
            isCsd -> 0
            else -> 1 // AVC NALU
        })
        sink.write24(if (isCsd) 0 else timeMillis)
        sink.write(avcPacket, 0, length)

        // previous tag size
        sink.writeInt(length + flagSize + 11)
        sink.flush() // end of tag
    }

    private fun copyByteBuffer(byteBuffer: ByteBuffer): ByteArray {
        if (!::bytes.isInitialized || bytes.size < byteBuffer.remaining()) {
            bytes = ByteArray(byteBuffer.remaining() + 1024)
        }
        byteBuffer.get(bytes, 0, byteBuffer.remaining())
        return bytes
    }

    companion object {
        /**
         * write a 24 bit int in Big Endianness
         */
        private fun DataOutputStream.write24(i: Int) {
            writeByte((i shr 16) and 0xFF)
            writeByte((i shr 8) and 0xFF)
            writeByte((i shr 0) and 0xFF)
        }

        fun writeHeader(sink: DataOutputStream) {
            sink.writeByte('F'.toInt())
            sink.writeByte('L'.toInt())
            sink.writeByte('V'.toInt())
            sink.writeByte(1)
            val flag = 1
            sink.writeByte(flag)
            sink.writeInt(9)

            // previous header size
            sink.writeInt(0)
            sink.flush()
        }

        fun createFlvHeader() = ByteArrayOutputStream().apply {
            DataOutputStream(this).use { writeHeader(it) }
        }.toByteArray()!!
    }
}