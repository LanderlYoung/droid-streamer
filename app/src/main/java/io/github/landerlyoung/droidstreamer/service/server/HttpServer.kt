package io.github.landerlyoung.droidstreamer.service.server

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import io.github.landerlyoung.droidstreamer.Global
import io.github.landerlyoung.droidstreamer.R
import io.github.landerlyoung.droidstreamer.service.DataSink
import io.github.landerlyoung.droidstreamer.service.flv.FlvMuxer
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * ```
 * Author: taylorcyang@tencent.com
 * Date:   2018-02-01
 * Time:   16:30
 * Life with Passion, Code with Creativity.
 * ```
 */
class HttpServer {
    companion object {
        const val TAG = "HttpServer"

        val DEFAULT_CONNECTION_TIME_OUT = TimeUnit.SECONDS.toMillis(10).toInt()
    }

    /**
     * HTTP & WebSocket server
     */
    private val httpServer: PageServer

    val listeningPort: Int
        get() = httpServer.listeningPort

    val h264DataSink: DataSink
        get() = dataSink.flvMuxer

    private val dataSink = StreamingFlvMuxer {
        broadcastFrame(it)
    }

    init {
        httpServer = createHttpServer()
        Log.i(TAG, "server created on port ${httpServer.listeningPort}")
    }

    private fun createHttpServer(): PageServer {
        val ports = listOf(
                8888, 8080, 8000,
                6666, 6060, 6000)
        ports.forEach {
            val server = PageServer(it)
            try {
                server.start(DEFAULT_CONNECTION_TIME_OUT)
                server.setAsyncRunner(PoolAsyncRunner)
                return server
            } catch (e: IOException) {
            }
        }
        TODO("port used")
    }

    fun stopServer() {
        httpServer.stop()
    }

    private fun broadcastFrame(frame: FrameInfo) {
        Log.i(TAG, "broadcastFrame no:${frame.sequence} length:${frame.length}")
        sok?.send(frame.buffer.copyOf(frame.length))
    }

    @Volatile
    var sok: NanoWSD.WebSocket? = null

    inner class PageServer(port: Int) : NanoWSD(port) {
        private val resourcesMap: Map<String, Int> = mapOf(
                "/" to R.raw.index,
                "/favicon.ico" to R.raw.faviocn,
                "/flv.min.js" to R.raw.flv_full
        )

        private fun getResourceStream(resId: Int): InputStream {
            return Global.app.resources.openRawResource(resId)
        }

        override fun serveHttp(session: IHTTPSession): Response {
            val path = session.uri.toString()
            Log.i(TAG, "loading resource [$path]")

            if (session.method == NanoHTTPD.Method.GET) {
                resourcesMap[path]?.let {
                    return NanoHTTPD.newChunkedResponse(
                            Response.Status.OK,
                                NanoHTTPD.MIME_HTML,
                            getResourceStream(it))
                }
            }
            return super.serveHttp(session)
        }

        override fun openWebSocket(handshake: IHTTPSession): WebSocket {
            return object : WebSocket(handshake) {

                val input = getResourceStream(R.raw.flv_mine)
                val buffer = ByteArray(1024)

                override fun send(payload: ByteArray?) {
                    try {
                        super.send(payload)
                    } catch (e: IOException) {
                        sok = null
                    }
                }

                override fun debugFrameReceived(frame: WebSocketFrame) {
                    super.debugFrameReceived(frame)
                    Log.i(TAG, "debugFrameReceived ${frame.opCode}")
                }

                override fun debugFrameSent(frame: WebSocketFrame) {
                    super.debugFrameSent(frame)
                    Log.i(TAG, "debugFrameSent: ${frame.opCode}")
                }

                override fun onOpen() {
                    Log.i(TAG, "ws onOpen")

                    var runnable: Runnable? = null
                    runnable = Runnable {
                        val len = input.read(buffer)
                        if (len == -1) {
                        } else {
                            send(buffer)
                            Global.secondaryHandler.postDelayed(runnable, 2)
                        }
                    }
//                    runnable.run()
                }

                override fun onClose(code: WebSocketFrame.CloseCode, reason: String?, initiatedByRemote: Boolean) {
                    Log.i(TAG, "ws onClose: code:$code reason:$reason")
                }

                override fun onPong(pong: WebSocketFrame?) {
                    Log.i(TAG, "ws onPong: $pong")
                }

                override fun onMessage(message: WebSocketFrame?) {
                    Log.i(TAG, "ws onMessage $message")
                }

                override fun onException(exception: IOException?) {
                    Log.i(TAG, "ws onException $exception")
                }
            }.apply {
                sok = this
            }
        }
    }

    private object PoolAsyncRunner : NanoHTTPD.AsyncRunner {
        val running = Collections.synchronizedList(mutableListOf<NanoHTTPD.ClientHandler>())

        override fun closeAll() {
            running.forEach {
                it.close()
            }
        }

        override fun closed(clientHandler: NanoHTTPD.ClientHandler) {
            running.remove(clientHandler)
            clientHandler.close()
        }

        override fun exec(code: NanoHTTPD.ClientHandler) {
            running.add(code)
            Global.ioThreadPool.submit(code)
        }
    }

    data class FrameInfo(val sequence: Int, val buffer: ByteArray, val length: Int)

    inner class StreamingFlvMuxer(private val onNewFrame: (FrameInfo) -> Unit) {

        val flvMuxer = FlvMuxer(object : OutputStream() {

            private val buffers = ByteArrayOutputStream()

            override fun write(b: ByteArray?, off: Int, len: Int) {
                if (sok != null) {
                    buffers.write(b, off, len)
                }
            }

            override fun write(b: Int) {
                if (sok != null) {
                    buffers.write(b)
                }
            }

            override fun flush() {
                if (sok == null) {
                    return
                }
                buffers.flush()
                // a whole frame is made!
                val frame = synchronized(this) {
                    if (mCurrentFrame == null) {

                    }

                    val no = (mCurrentFrame?.sequence ?: -1) + 1
                    FrameInfo(no, buffers.toByteArray(), buffers.size()).apply {
                        buffers.reset()
                        mCurrentFrame = this
                    }
                }
                onNewFrame.invoke(frame)
            }
        })

        val flvHeader = FlvMuxer.createFlvHeader()

        private var mCurrentFrame: FrameInfo? = null

        val currentFrame: FrameInfo?
            get() = mCurrentFrame
    }
}