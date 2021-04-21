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
import com.soywiz.korio.util.encoding.*
import com.soywiz.krypto.encoding.*
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

open class WsFrame(val data: ByteArray, val type: WsOpcode, val isFinal: Boolean = true, val masked: Boolean = true) {
    fun toByteArray(random: Random = Random): ByteArray = MemorySyncStreamToByteArray {
        val sizeMask = (if (masked) 0x80 else 0x00)

        write8(type.id or (if (isFinal) 0x80 else 0x00))

        when {
            data.size < 126 -> write8(data.size or sizeMask)
            data.size < 65536 -> {
                write8(126 or sizeMask)
                write16BE(data.size)
            }
            else -> {
                write8(127 or sizeMask)
                write32BE(0)
                write32BE(data.size)
            }
        }

        if (masked) {
            val mask = Random.nextBytes(4)
            writeBytes(mask)
            writeBytes(applyMask(data, mask))
        } else {
            writeBytes(data)
        }
    }

    companion object {
        fun applyMask(payload: ByteArray, mask: ByteArray?): ByteArray {
            if (mask == null) return payload
            val maskedPayload = ByteArray(payload.size)
            for (n in 0 until payload.size) maskedPayload[n] = (payload[n].toInt() xor mask[n % mask.size].toInt()).toByte()
            return maskedPayload
        }
    }
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

        launchImmediately(coroutineContext) {
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
            sendWsFrame(WsFrame(MemorySyncStreamToByteArray {
                write16BE(code)
                writeString(reason)
            }, WsOpcode.Close, masked = masked))
        }
    }

    override suspend fun send(message: String) {
        sendWsFrame(WsFrame(message.toByteArray(UTF8), WsOpcode.Text, masked = masked))
    }

    override suspend fun send(message: ByteArray) {
        sendWsFrame(WsFrame(message, WsOpcode.Binary, masked = masked))
    }

    companion object {
        suspend fun readWsFrame(s: AsyncInputStream): WsFrame = readWsFrameOrNull(s) ?: error("End of stream")

        suspend fun readWsFrameOrNull(s: AsyncInputStream): WsFrame? {
            val b0 = s.read()
            if (b0 < 0) return null
            val b1 = s.readU8()

            val isFinal = b0.extract(7)
            val opcode = WsOpcode(b0.extract(0, 4))

            val partialLength = b1.extract(0, 7)
            val isMasked = b1.extract(7)

            val length = when (partialLength) {
                126 -> s.readU16BE()
                127 -> {
                    val hi = s.readS32BE()
                    if (hi != 0) error("message too long > 2**32")
                    s.readS32BE()
                }
                else -> partialLength
            }
            val mask = if (isMasked) s.readBytesExact(4) else null
            val unmaskedData = s.readBytesExact(length)
            val finalData = WsFrame.applyMask(unmaskedData, mask)
            return WsFrame(finalData, opcode, isFinal, isMasked)
        }

    }

    suspend fun readWsFrame(): WsFrame = readWsFrame(client)

    suspend fun readWsFrameOrNull(): WsFrame? = readWsFrameOrNull(client)

    suspend fun sendWsFrame(frame: WsFrame, random: Random = this.random) {
        // masked should be true (since sent from the client)
        client.writeBytes(frame.toByteArray(random))
    }
}

inline class WsOpcode(val id: Int) {
    companion object {
        val Continuation = WsOpcode(0x00)
        val Text = WsOpcode(0x01)
        val Binary = WsOpcode(0x02)
        val Close = WsOpcode(0x08)
        val Ping = WsOpcode(0x09)
        val Pong = WsOpcode(0x0A)
    }
}
