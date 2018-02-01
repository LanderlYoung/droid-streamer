package io.github.landerlyoung.droidstreamer.service.server

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import io.github.landerlyoung.droidstreamer.Global
import io.github.landerlyoung.droidstreamer.R
import java.io.IOException
import java.io.InputStream
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

    fun broadcastThroughWebsocket() {
        TODO()
    }


    class PageServer(port: Int) : NanoWSD(port) {
        private val resourcesMap: Map<String, Int> = mapOf(
                "/" to R.raw.index,
                "/favicon.ico" to R.raw.faviocn,
                "/flv.min.js" to R.raw.flv_min
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
                val input = getResourceStream(R.raw.flv)
                val buffer = ByteArray(1024)

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
                    runnable.run()
                }

                override fun onClose(code: WebSocketFrame.CloseCode, reason: String?, initiatedByRemote: Boolean) {
                    Log.i(TAG, "ws onClose: code:$code reason:$reason")
                }

                override fun onPong(pong: WebSocketFrame?) {
//                    input.read(buffer)
//                    send(buffer)

                    Log.i(TAG, "ws onPong: $pong")
                }

                override fun onMessage(message: WebSocketFrame?) {
                    Log.i(TAG, "ws onMessage $message")

//                    input.read(buffer)
//                    send(buffer)
                }

                override fun onException(exception: IOException?) {
                    Log.i(TAG, "ws onException $exception")
                }
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
}