package com.soywiz.korio.net.ws

import com.soywiz.korio.async.*
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.util.*
import org.khronos.webgl.*
import org.w3c.dom.*

actual suspend fun WebSocketClient(
	url: String,
	protocols: List<String>?,
	origin: String?,
	wskey: String?,
	debug: Boolean,
    headers: Http.Headers,
    dummy: Boolean
): WebSocketClient = JsWebSocketClient(url, protocols, DEBUG = debug, headers = headers).apply { init() }

class JsWebSocketClient(
    url: String, protocols: List<String>?, val DEBUG: Boolean,
    val headers: Http.Headers // @NOTE: WebSocket on JS doesn't allow to set headers, so this will be ignored
) : WebSocketClient(url, protocols, true) {

	val jsws = if (protocols != null) {
		WebSocket(url, arrayOf(*protocols.toTypedArray()))
	} else {
		WebSocket(url)
	}.apply {
		this.binaryType = BinaryType.ARRAYBUFFER
		this.addEventListener("open", { onOpen(Unit) })
		this.addEventListener("close", { e ->
			val event = e as CloseEvent
			onClose(CloseInfo(event.code.toInt(), event.reason, event.wasClean))
		})
		this.addEventListener("message", { e ->
			val event = e as MessageEvent
			val data = event.data
			if (DEBUG) println("[WS-RECV]: $data :: stringListeners=${onStringMessage.listenerCount}, binaryListeners=${onBinaryMessage.listenerCount}, anyListeners=${onAnyMessage.listenerCount}")
			if (data is String) {
				val js = data.unsafeCast<String>()
				onStringMessage(js)
				onAnyMessage(js)
			} else {
				val jb = data.unsafeCast<ArrayBuffer>()
                val ba = jb.toByteArray()
				onBinaryMessage(ba)
				onAnyMessage(ba)
			}
		})
	}

	suspend fun init() {
		if (DEBUG) println("[WS] Wait connection ($url)...")
		onOpen.waitOne()
		if (DEBUG) println("[WS] Connected!")
	}

	override fun close(code: Int, reason: String) {
		//jsws.methods["close"](code, reason)
		jsws.close()
	}

	override suspend fun send(message: String) {
		if (DEBUG) println("[WS-SEND]: $message")
		jsws.send(message)
	}

	override suspend fun send(message: ByteArray) {
		if (DEBUG) println("[WS-SEND]: ${message.toList()}")
		jsws.send(message.toInt8Array())
	}
}
