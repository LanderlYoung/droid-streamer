package io.github.landerlyoung.droidstreamer.service.server

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import io.github.landerlyoung.droidstreamer.Global
import io.github.landerlyoung.droidstreamer.R
import java.io.IOException
import java.io.InputStream

/**
 * ```
 * Author: taylorcyang@tencent.com
 * Date:   2018-02-01
 * Time:   16:30
 * Life with Passion, Code with Creativity.
 * ```
 */

class HttpServer {
    /**
     * HTTP & WebSocket server
     */
    private val httpServer: PageServer

    val listeningPort: Int
        get() = httpServer.listeningPort

    init {
        httpServer = createHttpServer()
    }

    private fun createHttpServer(): PageServer {
        val ports = listOf(
                8888, 8080, 8000,
                6666, 6060, 6000)
        ports.forEach {
            val server = PageServer(it)
            try {
                server.start()
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

        private fun getResourceStream(resId: Int): InputStream {
            return Global.app.resources.openRawResource(resId)
        }

        override fun serveHttp(session: IHTTPSession): Response {
            if (session.method == NanoHTTPD.Method.GET) {
                when (session.uri.toString()) {
                    "/" -> // index.html
                        return NanoHTTPD.newChunkedResponse(
                                Response.Status.ACCEPTED,
                                NanoHTTPD.MIME_HTML,
                                getResourceStream(R.raw.index))
                }
            }
            return super.serveHttp(session)
        }

        override fun isWebsocketRequested(session: IHTTPSession): Boolean {
            return super.isWebsocketRequested(session)
        }


        override fun openWebSocket(handshake: IHTTPSession): WebSocket {
            TODO("not implemented")
        }
    }
}