package com.soywiz.korio.net.ws

import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korio.async.*
import com.soywiz.korio.experimental.KorioInternal
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import com.soywiz.korio.net.http.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import com.soywiz.krypto.encoding.*
import kotlinx.coroutines.Job
import kotlin.coroutines.*
import kotlin.random.*

suspend fun RawSocketWebSocketClient(
    url: String,
    protocols: List<String>? = null,
    origin: String? = null,
    wskey: String? = "wskey",
    debug: Boolean = false,
    connect: Boolean = true,
    headers: Http.Headers = Http.Headers(),
    masked: Boolean = true
): WebSocketClient {
    if (OS.isJsBrowserOrWorker) error("RawSocketWebSocketClient is not supported on JS browser. Use WebSocketClient instead")
    val uri = URL(url)
    val secure: Boolean = uri.isSecureScheme
    return RawSocketWebSocketClient(coroutineContext, AsyncClient.create(secure = secure), uri, protocols, debug, origin, wskey ?: "mykey", headers, masked).also { if (connect) it.internalConnect() }
}

class RawSocketWebSocketClient(
    val coroutineContext: CoroutineContext,
    val client: AsyncClient,
    val urlUrl: URL,
    protocols: List<String>? = null,
    debug: Boolean = false,
    val origin: String? = null,
    val key: String = "mykey",
    val headers: Http.Headers = Http.Headers(),
    val masked: Boolean = true,
    val random: Random = Random
) : WebSocketClient(urlUrl.fullUrl, protocols, debug) {
    private var frameIsBinary = false
    val host = urlUrl.host ?: "127.0.0.1"
    val port = urlUrl.port

    internal fun buildHeader(): String {
        val baseHeaders = Http.Headers.build {
            put("Host", "$host:$port")
            put("Pragma", "no-cache")
            put("Cache-Control", "no-cache")
            put("Upgrade", "websocket")
            if (protocols != null) {
                put("Sec-WebSocket-Protocol", protocols.joinToString(", "))
            }
            put("Sec-WebSocket-Version", "13")
            put("Connection", "Upgrade")
            put("Sec-WebSocket-Key", key.toByteArray().toBase64())
            if (origin != null) {
                put("Origin", origin)
            }
            put("User-Agent", HttpClient.DEFAULT_USER_AGENT)
        }
        val computedHeaders = baseHeaders.withReplaceHeaders(headers)
        return (buildList<String> {
            add("GET ${urlUrl.pathWithQuery} HTTP/1.1")
            for (item in computedHeaders) {
                add("${item.first}: ${item.second}")
            }
        }.joinToString("\r\n") + "\r\n\r\n")
    }

    private var readPacketsJob: Job? = null

    @KorioInternal
    suspend fun internalConnect() {
        if (OS.isJsBrowserOrWorker) error("RawSocketWebSocketClient is not supported on JS browser. Use WebSocketClient instead")

        client.connect(host, port)
        client.writeBytes(buildHeader().toByteArray())

        // Read response
        val headers = arrayListOf<String>()
        while (true) {
            val line = client.readLine().trimEnd()
            if (line.isEmpty()) {
                headers += line
                break
            }
        }

        readPacketsJob = launchImmediately(coroutineContext) {
            delay(1.milliseconds)
            internalReadPackets()
        }
    }

    private val chunks = arrayListOf<ByteArray>()
    private var isTextFrame = false

    @KorioInternal
    suspend fun internalReadPackets() {
        var close = CloseInfo(CloseReasons.NORMAL, null, false)
        onOpen(Unit)
        try {
            loop@ while (!closed) {
                val frame = readWsFrameOrNull() ?: break

                if (frame.type == WsOpcode.Close) {
                    val closeReason = if (frame.data.size >= 2) frame.data.readU16BE(0) else CloseReasons.UNEXPECTED
                    val closeMessage = if (frame.data.size >= 3) frame.data.readString(2, frame.data.size - 2) else null
                    close = CloseInfo(closeReason, closeMessage, true)
                    break@loop
                }

                when (frame.type) {
                    WsOpcode.Ping -> {
                        sendWsFrame(WsFrame(frame.data, WsOpcode.Pong, masked = masked))
                    }
                    WsOpcode.Pong -> {
                        lastPong = DateTime.now()
                    }
                    WsOpcode.Text, WsOpcode.Binary, WsOpcode.Continuation -> {
                        if (frame.type != WsOpcode.Continuation) {
                            chunks.clear()
                            isTextFrame = (frame.type == WsOpcode.Text)
                        }
                        chunks.add(frame.data)
                        if (frame.isFinal) {
                            val payloadBinary = chunks.join()
                            chunks.clear()
                            val payload: Any = if (isTextFrame) payloadBinary.toString(UTF8) else payloadBinary
                            when (payload) {
                                is String -> onStringMessage(payload)
                                is ByteArray -> onBinaryMessage(payload)
                            }
                            onAnyMessage(payload)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            //e.printStackTrace()
            onError(e)
        }
        onClose(close)
    }

    private var lastPong: DateTime? = null

    var closed = false

    override fun close(code: Int, reason: String) {
        closed = true

        launchImmediately(coroutineContext) {
            sendWsFrame(WsCloseInfo(code, reason).toFrame(masked))
            readPacketsJob?.cancel()
        }
    }

    override suspend fun send(message: String) {
        sendWsFrame(WsFrame(message.toByteArray(UTF8), WsOpcode.Text, masked = masked))
    }

    override suspend fun send(message: ByteArray) {
        sendWsFrame(WsFrame(message, WsOpcode.Binary, masked = masked))
    }

    companion object {
        suspend fun readWsFrame(s: AsyncInputStream): WsFrame = WsFrame.readWsFrame(s)
        suspend fun readWsFrameOrNull(s: AsyncInputStream): WsFrame? = WsFrame.readWsFrameOrNull(s)
    }

    suspend fun readWsFrame(): WsFrame = readWsFrame(client)
    suspend fun readWsFrameOrNull(): WsFrame? = readWsFrameOrNull(client)

    suspend fun sendWsFrame(frame: WsFrame, random: Random = this.random) {
        // masked should be true (since sent from the client)
        client.writeBytes(frame.toByteArray(random))
    }
}

