package korlibs.io.net.http

import korlibs.io.net.*
import korlibs.io.*
import korlibs.io.stream.*

internal actual val httpFactory: HttpFactory by lazy {
	object : HttpFactory {
		override fun createClient(): HttpClient = HttpPortable.createClient()
		override fun createServer(): HttpServer = HttpPortable.createServer()
	}
}
