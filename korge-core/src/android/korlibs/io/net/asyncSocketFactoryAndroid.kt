package korlibs.io.net

internal actual val asyncSocketFactory: AsyncSocketFactory by lazy {
	object : AsyncSocketFactory() {
		override suspend fun createClient(secure: Boolean): AsyncClient = JvmAsyncClient(secure = secure)
		override suspend fun createServer(port: Int, host: String, backlog: Int, secure: Boolean): AsyncServer =
			JvmAsyncServer(port, host, backlog, secure = secure).apply { init() }
	}
}
