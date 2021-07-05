package com.soywiz.korio.net.http

import com.soywiz.klock.seconds
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import com.soywiz.korio.net.ws.WsCloseInfo
import com.soywiz.korio.net.ws.WsFrame
import com.soywiz.korio.net.ws.WsOpcode
import com.soywiz.korio.stream.*
import com.soywiz.krypto.sha1
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.math.*

open class HttpPortable(
    private val factory: AsyncSocketFactory = asyncSocketFactory
) {
    companion object : HttpPortable()

    internal fun computeHeader(method: Http.Method, url: URL, rheaders2: Http.Headers): String = buildString {
        val EOL = "\r\n"
        append("$method ${url.pathWithQuery} HTTP/1.1$EOL")
        for (header in rheaders2) {
            append("${header.first}: ${header.second}$EOL")
        }
        append(EOL)
    }

    fun createClient(): HttpClient {
        return object : HttpClient() {
            override suspend fun requestInternal(method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?): Response {
                val url = URL(url)
                val secure = url.scheme == "https"
                //println("HTTP CLIENT: host=${url.host}, port=${url.port}, secure=$secure")
                val client = factory.createClient(url.host!!, url.port, secure)

                val rheaders = combineHeadersForHost(headers, url.host)
                val rheaders2 = if (content != null)
                    rheaders.withReplaceHeaders(Http.Headers(Http.Headers.ContentLength to content.getLength().toString()))
                else
                    rheaders
                client.writeString(computeHeader(method, url, rheaders2))
                content?.copyTo(client)

                //println("SENT RESPONSE")

                val firstLine = client.readLine()
                val responseInfo = Regex("HTTP/1.\\d+\\s+(\\d+)\\s+(.*)").find(firstLine)
                    ?: error("Invalid HTTP response $firstLine")

                //println("FIRST LINE: ${firstLine.trim()}")

                val responseCode = responseInfo.groupValues[1].toInt()
                val responseMessage = responseInfo.groupValues[2]

                val headers = arrayListOf<String>()
                while (true) {
                    val line = client.readLine().trim()
                    //println("line: $line")
                    if (line.isEmpty()) break
                    headers += line
                }

                val responseHeaders = Http.Headers(headers.map {
                    val parts = it.split(':', limit = 2)
                    parts.getOrElse(0) { "" } to parts.getOrElse(1) { "" }.trimStart()
                })

                return Response(responseCode, responseMessage, responseHeaders, client)
            }
        }
    }

    fun createServer(): HttpServer {
        val HeaderRegex = Regex("^(\\w+)\\s+(.*)\\s+(HTTP/1.[01])$")

        return object : HttpServer() {
            val BodyChunkSize = 1024
            val LimitRequestFieldSize = 8190
            val LimitRequestFields = 100

            var wshandler: suspend (WsRequest) -> Unit = {}
            var handler: suspend (Request) -> Unit = {}
            val onClose = Signal<Unit>()
            override var actualPort: Int = -1; private set

            override suspend fun websocketHandlerInternal(handler: suspend (WsRequest) -> Unit) {
                this.wshandler = handler
            }

            override suspend fun httpHandlerInternal(handler: suspend (Request) -> Unit) {
                this.handler = handler
            }

            override suspend fun listenInternal(port: Int, host: String) {
                val context = coroutineContext
                val socket = factory.createServer(port, host)
                actualPort = socket.port

                val close = socket.listen { client ->
                    while (true) {
                        //println("Connected! : $client : ${KorioNative.currentThreadId}")
                        val cb = client.bufferedInput()
                        //val cb = client

                        //val header = cb.readBufferedLine().trim()
                        //val fline = cb.readBufferedUntil('\n'.toByte()).toString(UTF8).trim()
                        val fline = cb.readUntil('\n'.toByte(), limit = LimitRequestFieldSize).toString(UTF8).trim()
                        //println("fline: $fline")
                        val match = HeaderRegex.matchEntire(fline)
                            ?: throw IllegalStateException("Not a valid request '$fline'")
                        val method = match.groupValues[1]
                        val url = match.groupValues[2]
                        val httpVersion = match.groupValues[3]
                        val headerList = arrayListOf<Pair<String, String>>()
                        for (n in 0 until LimitRequestFields) { // up to 1024 headers
                            val line = cb.readUntil('\n'.toByte(), limit = LimitRequestFieldSize).toString(UTF8).trim()
                            if (line.isEmpty()) break
                            val parts = line.split(':', limit = 2)
                            headerList += parts.getOrElse(0) { "" }.trim() to parts.getOrElse(1) { "" }.trim()
                        }
                        val headers = Http.Headers(headerList)
                        val keepAlive = headers["connection"]?.toLowerCase() == "keep-alive"
                        val upgradeWebsocket = headers["upgrade"]?.toLowerCase() == "websocket"
                        val contentLength = headers[Http.Headers.ContentLength]?.toLongOrNull()

                        //println("REQ: $method, $url, $headerList")

                        val requestCompleted = CompletableDeferred<Unit>(Job())

                        var bodyHandler: (ByteArray) -> Unit = {}
                        var endHandler: () -> Unit = {}

                        if (upgradeWebsocket) {
                            val websocketVersion = (headers["Sec-WebSocket-Version"]?.trim()?.toInt() ?: -1)
                            val websocketProtocol = headers["Sec-WebSocket-Protocol"]?.split(",")?.map { it.trim() }
                                ?: listOf()
                            val websocketKey = headers["Sec-WebSocket-Key"]
                            val websocketAcceptKey = "${websocketKey}258EAFA5-E914-47DA-95CA-C5AB0DC85B11".toByteArray().sha1().base64
                            val origin = headers["origin"]

                            try {
                                coroutineScope {
                                    val websocketScope = this

                                    val onStringMessageSignal = AsyncSignal<String>()
                                    val onByteArrayMessageSignal = AsyncSignal<ByteArray>()
                                    val onCloseSignal = AsyncSignal<WsCloseInfo>()

                                    val queue = AsyncQueue()

                                    fun send(msg: WsFrame) {
                                        queue(websocketScope.coroutineContext) {
                                            client.write(msg.toByteArray())
                                        }
                                    }

                                    fun send(msg: ByteArray, opcode: WsOpcode) {
                                        send(WsFrame(msg, opcode, true, masked = false))
                                    }

                                    var acceptHeaders = Http.Headers {
                                        put("Upgrade", "websocket")
                                        put("Connection", "Upgrade")
                                        put("Sec-WebSocket-Accept", websocketAcceptKey)
                                        val protocol = websocketProtocol.firstOrNull()
                                        if (protocol != null) {
                                            put("Sec-WebSocket-Protocol", protocol)
                                        }
                                        if (origin != null) {
                                            put("Origin", origin)
                                        }
                                    }

                                    wshandler(object : WsRequest(url, headers, websocketScope) {
                                        override fun reject() {
                                            throw CancellationException("Rejected")
                                        }

                                        override fun accept(headers: Http.Headers) {
                                            acceptHeaders += headers
                                        }

                                        override fun close() {
                                            launchImmediately { client.close() }
                                        }

                                        override fun onStringMessage(handler: suspend (String) -> Unit) {
                                            onStringMessageSignal(handler)
                                        }

                                        override fun onBinaryMessage(handler: suspend (ByteArray) -> Unit) {
                                            onByteArrayMessageSignal(handler)
                                        }

                                        override fun onClose(handler: suspend (WsCloseInfo) -> Unit) {
                                            onCloseSignal(handler)
                                        }

                                        override fun send(msg: String) {
                                            send(msg.toByteArray(), WsOpcode.Text)
                                        }

                                        override fun send(msg: ByteArray) {
                                            send(msg, WsOpcode.Binary)
                                        }
                                    })

                                    client.writeString(
                                        "$httpVersion 101 Switching Protocols\r\n" +
                                            acceptHeaders.toHttpHeaderString()
                                    )

                                    var receivedPong = false

                                    val pingJob = launchImmediately {
                                        while (true) {
                                            receivedPong = false
                                            send(WsFrame(byteArrayOf(1, 2, 3), WsOpcode.Ping, masked = false))
                                            delay(10.seconds)
                                            if (!receivedPong) {
                                                throw CancellationException("Disconnecting client because of timeout")
                                            }
                                        }
                                    }

                                    val readPacketJob = launchImmediately {
                                        while (true) {
                                            val frame = WsFrame.readWsFrameOrNull(client) ?: break
                                            //println("frame: $frame")
                                            if (!frame.isFinal) {
                                                val reason = "Unsupported non-final websocket frames"
                                                onCloseSignal(WsCloseInfo.MessageTooBig.copy(reason = reason))
                                                error(reason)
                                            }
                                            when (frame.type) {
                                                WsOpcode.Continuation -> {
                                                    val reason = "Unsupported websocket frame CONTINUATION"
                                                    onCloseSignal(WsCloseInfo.MessageTooBig.copy(reason = reason))
                                                    error(reason)
                                                }
                                                WsOpcode.Text -> onStringMessageSignal(frame.data.toString(Charsets.UTF8))
                                                WsOpcode.Binary -> onByteArrayMessageSignal(frame.data)
                                                WsOpcode.Pong -> receivedPong = true
                                                WsOpcode.Close -> {
                                                    val reason = "Normal close"
                                                    onCloseSignal(WsCloseInfo.fromBytes(frame.data).copy(reason = reason))
                                                    throw CancellationException(reason)
                                                }
                                                else -> {
                                                    val message = "Invalid frame ${frame.type}"
                                                    onCloseSignal(WsCloseInfo.InvalidFramePayloadData)
                                                    error(message)
                                                }
                                            }
                                        }
                                        val reason = "Dirty close"
                                        onCloseSignal(WsCloseInfo.AbnormalClosure.copy(reason = reason))
                                        throw CancellationException(reason)
                                    }

                                    //listOf(handlerJob, pingJob, readPacketJob).joinAll()
                                    listOf(pingJob, readPacketJob).joinAll()
                                }
                            } catch (e: CancellationException) {
                                //println("COMPLETED!")
                                //e.printStackTrace()
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        } else {
                            launchImmediately(coroutineContext) {
                                handler(object : Request(Http.Method(method), url, headers) {
                                    override suspend fun _handler(handler: (ByteArray) -> Unit) =
                                        run { bodyHandler = handler }

                                    override suspend fun _endHandler(handler: () -> Unit) = run { endHandler = handler }

                                    override suspend fun _sendHeader(code: Int, message: String, headers: Http.Headers) {
                                        client.writeString(buildString {
                                            append("$httpVersion $code $message\r\n")
                                            for (header in headers) append("${header.first}: ${header.second}\r\n")
                                            append("\r\n")
                                        })
                                    }

                                    override suspend fun _write(data: ByteArray, offset: Int, size: Int) {
                                        client.write(data, offset, size)
                                    }

                                    override suspend fun _end() {
                                        requestCompleted.complete(Unit)
                                    }
                                })
                            }

                            //println("Content-Length: '${headers["content-length"]}'")
                            //println("Content-Length: $contentLength")
                            if (contentLength != null) {
                                var remaining = contentLength
                                while (remaining > 0) {
                                    val toRead = min(BodyChunkSize.toLong(), remaining).toInt()
                                    val read = cb.readBytesUpToFirst(toRead)
                                    bodyHandler(read)
                                    remaining -= read.size
                                }
                            }
                            endHandler()

                            requestCompleted.await()

                            if (keepAlive) continue

                            client.close()
                            break
                        }
                    }
                }

                onClose {
                    close.close()
                }
            }

            override suspend fun closeInternal() {
                onClose()
            }
        }
    }
}
