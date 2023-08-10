@file:OptIn(ExperimentalStdlibApi::class)

package korlibs.io.net.ws

import korlibs.io.async.Signal
import korlibs.io.lang.IOException
import korlibs.io.net.http.Http
import kotlinx.coroutines.channels.Channel

abstract class WebSocketClient protected constructor(val url: String, val protocols: List<String>?, debug: Boolean) {
	val onOpen = Signal<Unit>()
	val onError = Signal<Throwable>()
	val onClose = Signal<CloseInfo>()

	val onBinaryMessage = Signal<ByteArray>()
	val onStringMessage = Signal<String>()
	val onAnyMessage = Signal<Any>()

    data class CloseInfo(val code: Int, val message: String?, val wasClean: Boolean)

    object CloseReasons {
        val NORMAL = 1000
        val GOING_AWAY = 1001
        val PROTOCOL_ERROR = 1002
        val UNACCEPTABLE = 1003
        val RESERVED = 1004
        val RESERVED_1005 = 1005
        val RESERVED_1006 = 1006
        val INCONSISENT = 1007
        val POLICY_VIOLATION = 1008
        val TOO_BIG = 1009
        val MISSING_EXTENSION = 1010
        val UNEXPECTED = 1011
        val RESERVED_1015 = 1015
    }

    open fun close(code: Int = 1000, reason: String = "OK"): Unit = Unit
    fun close(info: WsCloseInfo) = close(info.code, info.reason)
	open suspend fun send(message: String): Unit = Unit
	open suspend fun send(message: ByteArray): Unit = Unit

    fun messageChannel(limit: Int = Channel.UNLIMITED): Channel<Any> =
        Channel<Any>(limit).also { messages -> onAnyMessage.add { messages.trySend(it) } }

    fun messageChannelString(limit: Int = Channel.UNLIMITED): Channel<Any> =
        Channel<Any>(limit).also { messages -> onStringMessage.add { messages.trySend(it) } }

    fun messageChannelBinary(limit: Int = Channel.UNLIMITED): Channel<Any> =
        Channel<Any>(limit).also { messages -> onBinaryMessage.add { messages.trySend(it) } }
}

expect suspend fun WebSocketClient(
    url: String,
    protocols: List<String>?,
    origin: String?,
    wskey: String,
    debug: Boolean,
    headers: Http.Headers,
    dummy: Boolean,
    wsInit: WebSocketClient.() -> Unit = {},
): WebSocketClient

suspend fun WebSocketClient(
    url: String,
    protocols: List<String>? = null,
    origin: String? = null,
    wskey: String = DEFAULT_WSKEY,
    debug: Boolean = false,
    headers: Http.Headers = Http.Headers(),
    wsInit: WebSocketClient.() -> Unit = {},
): WebSocketClient = WebSocketClient(url, protocols, origin, wskey, debug, headers, true, wsInit)

const val DEFAULT_WSKEY = "mywskey12345adfg"

class WebSocketException(message: String) : IOException(message)
