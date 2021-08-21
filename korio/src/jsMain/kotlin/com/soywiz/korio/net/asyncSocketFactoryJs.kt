package com.soywiz.korio.net

import com.soywiz.korio.*
import com.soywiz.korio.util.*
import kotlin.coroutines.*

internal actual val asyncSocketFactory: AsyncSocketFactory by lazy {
	object : AsyncSocketFactory() {
		override suspend fun createClient(secure: Boolean): AsyncClient = jsRuntime.createClient(secure)
		override suspend fun createServer(port: Int, host: String, backlog: Int, secure: Boolean): AsyncServer =
            jsRuntime.createServer(port, host, backlog, secure)
	}
}
