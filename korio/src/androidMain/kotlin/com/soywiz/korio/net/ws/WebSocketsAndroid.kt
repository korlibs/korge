package com.soywiz.korio.net.ws

import com.soywiz.korio.net.http.Http

actual suspend fun WebSocketClient(
	url: String,
	protocols: List<String>?,
	origin: String?,
	wskey: String?,
	debug: Boolean,
    headers: Http.Headers,
    dummy: Boolean
): WebSocketClient = RawSocketWebSocketClient(url, protocols, origin, wskey, debug, headers = headers)
