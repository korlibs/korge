package korlibs.io.net

import java.net.*
import javax.net.ssl.*

class JvmAsyncServer(
    override val requestPort: Int,
    override val host: String,
    override val backlog: Int = -1,
    val secure: Boolean = false
) : AsyncServer {
    val ssc: ServerSocket = when {
        secure -> SSLServerSocketFactory.getDefault().createServerSocket()
        else -> ServerSocket()
    }.also {
        // So accept only waits in 100 ms, so we are able to cancel, and do not blocks too much time IO threads
        it.soTimeout = 100
    }
    suspend fun init() = doIo { ssc.bind(InetSocketAddress(host, requestPort), backlog) }
    override val port: Int get() = (ssc.localSocketAddress as? InetSocketAddress)?.port ?: -1

    override suspend fun accept(): AsyncClient {
        while (true) {
            try {
                return doIo { JvmAsyncClient(ssc.accept()) }
            } catch (e: SocketTimeoutException) {
                continue
            }
        }
    }

    override suspend fun close() = doIo { ssc.close() }
}
