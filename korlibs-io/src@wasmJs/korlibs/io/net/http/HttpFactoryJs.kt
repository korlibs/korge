package korlibs.io.net.http

import korlibs.io.jsRuntime

internal actual val httpFactory: HttpFactory by lazy {
	object : HttpFactory {
		override fun createClient(): HttpClient = jsRuntime.createHttpClient()
		override fun createServer(): HttpServer = jsRuntime.createHttpServer()
	}
}
