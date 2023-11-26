package korlibs.io.net

import korlibs.io.net.ssl.*

internal actual val asyncSocketFactory: AsyncSocketFactory by lazy {
	object : AsyncSocketFactory() {
		override suspend fun createClient(secure: Boolean): AsyncClient {
            //if (secure) return JvmAsyncClient(secure = secure)
            return JvmNioAsyncClient().let { if (secure) AsyncClientSSLProcessor(it) else it }
        }
		override suspend fun createServer(port: Int, host: String, backlog: Int, secure: Boolean): AsyncServer =
            // @TODO: Make JvmAsyncServerSocketChannel support secure SSL sockets
            if (secure) {
                JvmAsyncServer(port, host, backlog, secure = secure).apply { init() }
            } else {
                JvmNioAsyncServer(port, host, backlog).apply { init() }
            }
	}
}
