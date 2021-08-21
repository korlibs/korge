package com.soywiz.korio.net

import com.soywiz.korio.*
import com.soywiz.korio.util.*
import kotlin.coroutines.*

internal actual val asyncSocketFactory: AsyncSocketFactory by lazy {
	object : AsyncSocketFactory() {
		override suspend fun createClient(secure: Boolean): AsyncClient {
            if (NodeDeno.node) return NodeJsAsyncClient(coroutineContext)
			error("AsyncClient is not supported on JS browser")
        }
		override suspend fun createServer(port: Int, host: String, backlog: Int, secure: Boolean): AsyncServer {
            if (NodeDeno.node) return NodeJsAsyncServer().init(port, host, backlog)
			error("AsyncServer is not supported on JS browser")
        }
	}
}
