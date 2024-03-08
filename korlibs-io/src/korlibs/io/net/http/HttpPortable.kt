package korlibs.io.net.http

import korlibs.crypto.*
import korlibs.io.async.*
import korlibs.io.lang.*
import korlibs.io.net.*
import korlibs.io.net.ws.*
import korlibs.io.stream.*
import korlibs.logger.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.math.*

open class HttpPortable(
    private val factory: AsyncSocketFactory = asyncSocketFactory
) {
    companion object : HttpPortable() {
        internal fun computeHeader(method: Http.Method, url: URL, rheaders2: Http.Headers): String =
            HttpPortableClient.computeHeader(method, url, rheaders2)
    }

    fun createClient(): HttpClient = HttpPortableClient(factory)

    fun createServer(): HttpServer = HttpPortableServer(factory)
}

internal class HttpPortableClient(val factory: AsyncSocketFactory) : HttpClient() {
    //withContext(if (OS.isNative) coroutineContext else Dispatchers.Default) { // @TODO: We should try to execute pending events from the eventloop while waiting for vsync
    override suspend fun requestInternal(
        method: Http.Method,
        url: String,
        headers: Http.Headers,
        content: AsyncInputStreamWithLength?
    ): Response = withContext(coroutineContext) {
        val url = URL(url)
        val secure = url.scheme == "https"
        //println("HTTP CLIENT: host=${url.host}, port=${url.port}, secure=$secure")
        val client = factory.createClient(url.host!!, url.port, secure)

        //println("[1]")

        val rheaders = combineHeadersForHost(headers, url.host)
        val rheaders2 = if (content != null)
            rheaders.withReplaceHeaders(
                Http.Headers(
                    Http.Headers.ContentLength to content.getLength().toString()
                )
            )
        else
            rheaders

        //println("[2]")

        client.writeString(computeHeader(method, url, rheaders2))

        //println("[3]")

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

        Response(responseCode, responseMessage, responseHeaders, client)
    }

    companion object {
        internal fun computeHeader(method: Http.Method, url: URL, rheaders2: Http.Headers): String = buildString {
            val EOL = "\r\n"
            append("$method ${url.pathWithQuery} HTTP/1.1$EOL")
            for (header in rheaders2) {
                append("${header.first}: ${header.second}$EOL")
            }
            append(EOL)
        }
    }
}

internal class HttpPortableServer(val factory: AsyncSocketFactory) : HttpServer() {
    companion object {
        private val logger = Logger("HttpPortableServer")
        val HeaderRegex = Regex("^(\\w+)\\s+(.*)\\s+(HTTP/1.[01])$")
    }

    val BodyChunkSize = 1024
    val LimitRequestFieldSize = 8190
    val LimitRequestFields = 100

    var wshandler: suspend (WsRequest) -> Unit = {}
    var handler: suspend (Request) -> Unit = {}
    var errorHandler: suspend (Throwable) -> Unit = {}
    val onClose = Signal<Unit>()
    override var actualPort: Int = -1; private set
    override var actualHost: String = "127.0.0.1"; private set

    override suspend fun errorHandlerInternal(handler: suspend (Throwable) -> Unit) {
        this.errorHandler = handler
    }

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
        actualHost = socket.host

        val close = socket.listen { client -> handleClient(client) }

        onClose {
            close.close()
        }
    }

    class RequestInfo(
        val client: AsyncClient,
        val cb: AsyncBufferedInputStream,
        val url: String,
        val headers: Http.Headers,
        val httpVersion: String,
        val method: String,
    ) {
        val keepAlive = headers["connection"].equals("keep-alive", ignoreCase = true)
        val upgradeWebsocket = headers["upgrade"].equals("websocket", ignoreCase = true)
        val contentLength = headers[Http.Headers.ContentLength]?.toLongOrNull()
    }

    suspend fun handleClient(client: AsyncClient) {
        try {
            while (true) {
                //println("Connected! : $client : ${KorioNative.currentThreadId}")
                val cb = client.bufferedInput()
                //val cb = client

                //val header = cb.readBufferedLine().trim()
                //val fline = cb.readBufferedUntil('\n'.toByte()).toString(UTF8).trim()
                val fline =
                    cb.readUntil('\n'.code.toByte(), limit = LimitRequestFieldSize).toString(UTF8).trim()
                if (fline.isEmpty()) break // Do not fail, just skip

                //println("fline: $fline")
                val match = HeaderRegex.matchEntire(fline)
                    ?: throw Http.InvalidRequestException("Not a valid http request '$fline'")
                val method = match.groupValues[1]
                val url = match.groupValues[2]
                val httpVersion = match.groupValues[3]
                val headerList = arrayListOf<Pair<String, String>>()
                for (n in 0 until LimitRequestFields) { // up to 1024 headers
                    val line =
                        cb.readUntil('\n'.code.toByte(), limit = LimitRequestFieldSize).toString(UTF8)
                            .trim()
                    if (line.isEmpty()) break
                    val parts = line.split(':', limit = 2)
                    headerList += parts.getOrElse(0) { "" }.trim() to parts.getOrElse(1) { "" }.trim()
                }
                val headers = Http.Headers(headerList)

                //println("REQ: $method, $url, $headerList")

                val info = RequestInfo(client, cb, url, headers, httpVersion, method)

                if (info.upgradeWebsocket) {
                    handleWebsocket(info)
                } else {
                    handleNormalRequest(info)

                    if (info.keepAlive) {
                        continue
                    } else {
                        client.close()
                        break
                    }
                }
            }
        } catch (e: Throwable) {
            // Do nothing
            errorHandler(e)
            if (e is CancellationException) throw e
        }
    }

    suspend fun handleNormalRequest(info: RequestInfo) {
        val client = info.client
        val cb = info.cb
        val headers = info.headers
        val url = info.url
        val method = info.method
        val httpVersion = info.httpVersion

        val requestCompleted = CompletableDeferred<Unit>(Job())
        var bodyHandler: (ByteArray) -> Unit = {}
        var endHandler: () -> Unit = {}

        launchImmediately(coroutineContext) {
            handler(object : Request(Http.Method(method), url, headers) {
                override suspend fun _handler(handler: (ByteArray) -> Unit) { bodyHandler = handler }
                override suspend fun _endHandler(handler: () -> Unit) { endHandler = handler }
                override suspend fun _sendHeader(
                    code: Int,
                    message: String,
                    headers: Http.Headers
                ) {
                    client.writeString(buildString {
                        append("$httpVersion $code $message\r\n")
                        for (header in headers) append("${header.first}: ${header.second}\r\n")
                        append("\r\n")
                    })
                }

                override val _output: AsyncOutputStream = client

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
        if (info.contentLength != null) {
            var remaining = info.contentLength
            while (remaining > 0) {
                val toRead = min(BodyChunkSize.toLong(), remaining).toInt()
                val read = cb.readBytesUpToFirst(toRead)
                bodyHandler(read)
                remaining -= read.size
            }
        }
        endHandler()

        requestCompleted.await()
    }

    suspend fun handleWebsocket(info: RequestInfo) {
        val headers = info.headers
        val client = info.client
        val url = info.url
        val httpVersion = info.httpVersion
        val websocketVersion = (headers["Sec-WebSocket-Version"]?.trim()?.toInt() ?: -1)
        val websocketProtocol = headers["Sec-WebSocket-Protocol"]?.split(",")?.map { it.trim() }
            ?: listOf()
        val websocketKey = headers["Sec-WebSocket-Key"]
        val websocketAcceptKey =
            "${websocketKey}258EAFA5-E914-47DA-95CA-C5AB0DC85B11".toByteArray().sha1().base64
        val origin = headers["origin"]

        val onStringMessageSignal = AsyncSignal<String>()
        val onByteArrayMessageSignal = AsyncSignal<ByteArray>()
        val onCloseSignal = AsyncSignal<WsCloseInfo>()
        val queue = AsyncQueue()

        try {
            coroutineScope {
                val websocketScope = this
                fun send(msg: WsFrame) {
                    try {
                        queue(websocketScope.coroutineContext) {
                            try {
                                client.write(msg.toByteArray())
                            } catch (e: IOException) {
                                // Do nothing
                            }
                        }
                    } catch (e: IOException) {
                        logger.error { "Error on queue" }
                        // Do nothing
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
                    override val address: AsyncAddress get() = client.address

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

                val pingJob = asyncImmediately {
                    try {
                        while (true) {
                            receivedPong = false
                            //Console.error("PING")
                            send(WsFrame(byteArrayOf(1, 2, 3), WsOpcode.Ping, masked = false))
                            delay(10.seconds)
                            if (!receivedPong) {
                                throw WsCloseException(WsCloseInfo.GoingAway.copy(reason = "Disconnecting client because of timeout"))
                            }
                        }
                    } finally {
                        //Console.error("PING COMPLETED")
                    }
                }

                val readPacketJob = asyncImmediately {
                    try {
                        while (true) {
                            //Console.error("READ FRAME")
                            val frame = WsFrame.readWsFrameOrNull(client) ?: break
                            //Console.error("READ FRAME:$frame")
                            //println("frame: $frame")
                            if (!frame.isFinal) {
                                throw WsCloseException(
                                    WsCloseInfo.MessageTooBig.copy(reason = "Unsupported non-final websocket frames")
                                )
                            }
                            when (frame.type) {
                                WsOpcode.Continuation -> {
                                    throw WsCloseException(
                                        WsCloseInfo.MessageTooBig.copy(reason = "Unsupported websocket frame CONTINUATION")
                                    )
                                }
                                WsOpcode.Text -> onStringMessageSignal(frame.data.toString(Charsets.UTF8))
                                WsOpcode.Binary -> onByteArrayMessageSignal(frame.data)
                                WsOpcode.Pong -> receivedPong = true
                                WsOpcode.Close -> {
                                    throw WsCloseException(
                                        WsCloseInfo.fromBytes(frame.data).copy(reason = "Normal close")
                                    )
                                }
                                else -> {
                                    throw WsCloseException(
                                        WsCloseInfo.InvalidFramePayloadData.copy(reason = "Invalid frame ${frame.type}")
                                    )
                                }
                            }
                        }
                        throw WsCloseException(WsCloseInfo.AbnormalClosure)
                    } finally {
                        //Console.error("READ PACKET COMPLETED")
                    }
                }

                //listOf(handlerJob, pingJob, readPacketJob).joinAll()
                //Console.error("handleWebsocket.beforeJobs")
                try {
                    listOf(pingJob, readPacketJob).awaitAll()
                } finally {
                    pingJob.cancel()
                    readPacketJob.cancel()
                }
                //Console.error("handleWebsocket.afterJobs")
            }
        } catch (e: Throwable) {
            //Console.error("handleWebsocket.catch: ${e::class}, ${e.message}")

            val info: WsCloseInfo = when (e) {
                is WsCloseException -> e.close
                is CancellationException, is IOException -> WsCloseInfo.GoingAway
                else -> WsCloseInfo.AbnormalClosure
            }
            onCloseSignal(info)

            if (e !is CancellationException) {
                e.printStackTraceWithExtraMessage("HttpPortable.server.catch")
            }

            // Rethrow
            if (e is CancellationException) throw e
        } finally {
            //Console.error("handleWebsocket.finally")
        }
    }

    class WsCloseException(val close: WsCloseInfo) : CancellationException(close.reason)

    override suspend fun closeInternal() {
        onClose()
    }
}
