package korlibs.korge.ipc

import java.io.*
import java.net.*
import java.nio.channels.*

object KorgeUnixSocket {
    class UnsupportedUnixDomainSocketAddressException(cause: Throwable) : RuntimeException("Unsupported UnixDomainSocketAddress: ${cause.message}", cause)

    val UNIX_PROTOCOL_FAMILY: ProtocolFamily get() = StandardProtocolFamily::class.java.fields.firstOrNull { it.name == "UNIX" }?.get(null) as? ProtocolFamily? ?: error("Can't find StandardProtocolFamily.UNIX ")

    fun open(path: String): SocketChannel {
        return SocketChannel.open(UnixDomainSocketAddress_of(path))
    }

    fun bind(path: String, delete: Boolean = true, deleteOnExit: Boolean = true): ServerSocketChannel {
        if (delete) File(path).delete()
        return ServerSocketChannel_open(UNIX_PROTOCOL_FAMILY).also { it.bind(UnixDomainSocketAddress_of(path)) }.also {
            if (deleteOnExit) File(path).deleteOnExit()
        }
    }

    /*
    fun bindAsync(path: String, delete: Boolean = true): AsynchronousServerSocketChannel {
        deletePath(path, delete)
        return AsynchronousServerSocketChannel.open().bind(UnixDomainSocketAddress_of(path))
    }

    fun listenSuspend(path: String, delete: Boolean = true): kotlinx.coroutines.flow.Flow<AsynchronousSocketChannel> = flow {
        deletePath(path, delete)
        val channel = KorgeUnixSocket.bindAsync(path)
        try {
            while (true) {
                emit(channel.acceptSuspend())
            }
        } catch (e: CancellationException) {
            channel.close()
        }
    }

    fun openAsync(path: String): AsynchronousSocketChannel {
        return AsynchronousSocketChannel.open().bind(UnixDomainSocketAddress_of(path))
    }

     */

    fun ServerSocketChannel_open(family: ProtocolFamily): ServerSocketChannel =
        ServerSocketChannel::class.java.getMethod("open", ProtocolFamily::class.java).invoke(null, family) as ServerSocketChannel

    fun UnixDomainSocketAddress_of(path: String): java.net.SocketAddress {
        val javaMajorVersion = getJavaMajorVersion()
        if (javaMajorVersion < 16) {
            throw UnsupportedUnixDomainSocketAddressException(RuntimeException("Unix only supported on Java 16, but run on Java $javaMajorVersion"))
        }

        try {
            return Class.forName("java.net.UnixDomainSocketAddress").getMethod("of", String::class.java).invoke(null, path) as java.net.SocketAddress
        } catch (e: Throwable) {
            e.printStackTrace()
            throw UnsupportedUnixDomainSocketAddressException(e)
        }
    }

    private fun getJavaMajorVersion(): Int {
        val version = System.getProperty("java.version")
        return if (version.startsWith("1.")) {
            version.substring(2, 3).toInt()
        } else {
            version.substringBefore('.').toInt()
        }
    }
}

/*
suspend fun AsynchronousServerSocketChannel.acceptSuspend(): AsynchronousSocketChannel = suspendCompletionHandler { accept(null, it) }

suspend fun <T> suspendCompletionHandler(block: (CompletionHandler<T, Any?>) -> Unit): T = suspendCoroutine<T> { c ->
    block(object : CompletionHandler<T, Any?> {
        override fun completed(result: T, attachment: Any?) = c.resume(result)
        override fun failed(exc: Throwable, attachment: Any?) = c.resumeWithException(exc)
    })
}
*/
