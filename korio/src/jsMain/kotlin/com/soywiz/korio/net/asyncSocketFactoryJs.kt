package com.soywiz.korio.net

import com.soywiz.korio.*
import com.soywiz.korio.util.*
import kotlin.coroutines.*

internal actual val asyncSocketFactory: AsyncSocketFactory by lazy {
	object : AsyncSocketFactory() {
		override suspend fun createClient(secure: Boolean): AsyncClient {
            //if (OS.isJsBrowserOrWorker) error("AsyncClient is not supported on JS browser")
            //return NodeJsAsyncClient(coroutineContext)
			error("AsyncClient is not supported on JS browser")
        }
		override suspend fun createServer(port: Int, host: String, backlog: Int, secure: Boolean): AsyncServer {
            //if (OS.isJsBrowserOrWorker) error("AsyncServer is not supported on JS browser")
            //return NodeJsAsyncServer().init(port, host, backlog)
			error("AsyncServer is not supported on JS browser")
        }
	}
}
