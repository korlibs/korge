package korlibs.io.net.http

import korlibs.io.*

internal actual val httpFactory: HttpFactory by lazy {
	object : HttpFactory {
		init {
			System.setProperty("http.keepAlive", "false")
		}

		//override fun createClient(): HttpClient = HttpClientJvm()
		override fun createClient(): HttpClient = HttpPortable.createClient()
		override fun createServer(): HttpServer = HttpPortable.createServer()
	}
}
