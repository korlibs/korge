package korlibs.io.net

internal actual val asyncSocketFactory: AsyncSocketFactory = DarwinNativeAsyncSocketFactory

object DarwinNativeAsyncSocketFactory : BaseNativeAsyncSocketFactory() {
    override suspend fun createClient(secure: Boolean): AsyncClient {
        if (secure) return NativeSecureAsyncClient(DarwinSSLSocket())
        return super.createClient(secure)
    }

    class NativeSecureAsyncClient(val socket: DarwinSSLSocket) : AsyncClient {
        override val address: AsyncAddress get() = socket.endpoint.toAsyncAddress()
        override val connected: Boolean get() = socket.connected
        override suspend fun connect(host: String, port: Int) { socket.connect(host, port) }
        override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int = socket.read(buffer, offset, len)
        override suspend fun write(buffer: ByteArray, offset: Int, len: Int) { socket.write(buffer, offset, len) }
        override suspend fun close() { socket.close() }
    }
}
