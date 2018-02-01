package io.github.landerlyoung.droidstreamer.service.server

import fi.iki.elonen.NanoWSD
import java.io.IOException

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
        override fun serveHttp(session: IHTTPSession?): Response {
            return super.serveHttp(session)
        }

        override fun isWebsocketRequested(session: IHTTPSession?): Boolean {
            return super.isWebsocketRequested(session)
        }


        override fun openWebSocket(handshake: IHTTPSession?): WebSocket {
            TODO("not implemented")
        }
    }
}