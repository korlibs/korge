package korlibs.io.net

import korlibs.io.util.nioSuspendCompletion
import kotlinx.coroutines.delay
import java.net.*
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import kotlin.coroutines.cancellation.*

class JvmNioAsyncServer(override val requestPort: Int, override val host: String, override val backlog: Int = -1) :
    AsyncServer {
    val ssc: AsynchronousServerSocketChannel = AsynchronousServerSocketChannel.open()

    suspend fun init() {
        ssc.bind(InetSocketAddress(host, requestPort), backlog)
        for (n in 0 until 100) {
            if (ssc.isOpen) break
            delay(50)
        }
    }

    override val port: Int get() = (ssc.localAddress as? InetSocketAddress)?.port ?: -1

    override suspend fun accept(): AsyncClient {
        try {
            return JvmNioAsyncClient(nioSuspendCompletion<AsynchronousSocketChannel> { ssc.accept(Unit, it) })
        } catch (e: CancellationException) {
            @Suppress("BlockingMethodInNonBlockingContext")
            ssc.close()
            throw e
        }
    }

    override suspend fun close() {
        try {
            @Suppress("BlockingMethodInNonBlockingContext")
            ssc.close()
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            if (e is AsynchronousCloseException) return
            e.printStackTrace()
        }
    }
}
