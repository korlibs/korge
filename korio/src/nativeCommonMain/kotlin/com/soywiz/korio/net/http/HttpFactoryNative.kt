package com.soywiz.korio.net.http

import com.soywiz.korio.net.*
import com.soywiz.korio.*
import com.soywiz.korio.stream.*

internal actual val httpFactory: HttpFactory by lazy {
	object : HttpFactory {
		override fun createClient(): HttpClient = HttpPortable.createClient()
		override fun createServer(): HttpServer = HttpPortable.createServer()
	}
}
