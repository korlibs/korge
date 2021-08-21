package com.soywiz.korio.net.http

import com.soywiz.korio.jsRuntime

internal actual val httpFactory: HttpFactory by lazy {
	object : HttpFactory {
		override fun createClient(): HttpClient = jsRuntime.createHttpClient()
		override fun createServer(): HttpServer = jsRuntime.createHttpServer()
	}
}
