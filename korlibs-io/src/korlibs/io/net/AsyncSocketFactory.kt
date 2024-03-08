package korlibs.io.net

import korlibs.io.async.AsyncCloseable
import korlibs.io.async.Signal
import korlibs.io.async.launchImmediately
import korlibs.io.concurrent.atomic.korAtomic
import korlibs.io.lang.Closeable
import korlibs.io.lang.IOException
import korlibs.io.lang.printStackTraceWithExtraMessage
import korlibs.io.stream.AsyncInputStream
import korlibs.io.stream.AsyncOutputStream
import korlibs.io.stream.DequeSyncStream
import korlibs.io.stream.SyncStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.supervisorScope
import kotlin.coroutines.coroutineContext

abstract class AsyncSocketFactory {
    open suspend fun createClient(secure: Boolean = false): AsyncClient = TODO()
	open suspend fun createServer(port: Int, host: String = "127.0.0.1", backlog: Int = 511, secure: Boolean = false): AsyncServer = TODO()
}

internal expect val asyncSocketFactory: AsyncSocketFactory

suspend fun AsyncSocketFactory.createClient(host: String, port: Int, secure: Boolean = false): AsyncClient =
    createClient(secure).apply { connect(host, port) }

suspend fun createTcpClient(secure: Boolean = false): AsyncClient = asyncSocketFactory.createClient(secure)
suspend fun createTcpServer(port: Int = AsyncServer.ANY_PORT, host: String = "127.0.0.1", backlog: Int = 511, secure: Boolean = false): AsyncServer = asyncSocketFactory.createServer(port, host, backlog, secure)

suspend fun createTcpClient(host: String, port: Int, secure: Boolean = false): AsyncClient = asyncSocketFactory.createClient(host, port, secure)

interface AsyncClient : AsyncInputStream, AsyncOutputStream, AsyncCloseable {
	suspend fun connect(host: String, port: Int)
    val address: AsyncAddress get() = AsyncAddress()

	val connected: Boolean
	override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int
	override suspend fun write(buffer: ByteArray, offset: Int, len: Int)
	override suspend fun close()
	//suspend open fun reconnect() = Unit

	object Stats {
		val writeCountStart = korAtomic(0L)
		val writeCountEnd = korAtomic(0L)
		val writeCountError = korAtomic(0L)

		override fun toString(): String = "AsyncClient.Stats($writeCountStart/$writeCountEnd/$writeCountError)"
	}

	companion object {
		suspend operator fun invoke(host: String, port: Int, secure: Boolean = false, connect: Boolean = true): AsyncClient =
            asyncSocketFactory.createClient(secure).also { if (connect) it.connect(host, port)  }
		suspend fun create(secure: Boolean = false): AsyncClient = asyncSocketFactory.createClient(secure)
		suspend fun createAndConnect(host: String, port: Int, secure: Boolean = false): AsyncClient = invoke(host, port, secure)
	}
}

class FakeAsyncClient(
    val serverToClient: SyncStream = DequeSyncStream(),
    val clientToServer: SyncStream = DequeSyncStream(),
    val onConnect: Signal<Pair<String, Int>> = Signal(),
    val onClose: Signal<Unit> = Signal()
) : AsyncClient {
    override var connected: Boolean = false

    override val address: AsyncAddress get() = AsyncAddress()

    override suspend fun connect(host: String, port: Int) {
        onConnect(host to port)
        connected = true
    }
    override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int = serverToClient.read(buffer, offset, len)
    // @TODO: BUG: Required override because of Bug
    override suspend fun read(): Int = serverToClient.read()

    override suspend fun write(buffer: ByteArray, offset: Int, len: Int) = clientToServer.write(buffer, offset, len)
    override suspend fun close() {
        onClose(Unit)
    }
}

data class AsyncAddress(val address: String = "0.0.0.0", val port: Int = 0)

interface AsyncServer : AsyncCloseable {
	val requestPort: Int
	val host: String
	val backlog: Int
	val port: Int

	companion object {
		val ANY_PORT = 0

		suspend operator fun invoke(port: Int, host: String = "127.0.0.1", backlog: Int = -1) =
			asyncSocketFactory.createServer(port, host, backlog)
	}

	suspend fun accept(): AsyncClient

	suspend fun listen(handler: suspend (AsyncClient) -> Unit): Closeable {
		val job = launchImmediately(coroutineContext) {
            try {
                while (true) {
                    try {
                        //Console.error("AsyncServer.listen.accept[0]: $this")
                        val client = accept()
                        //Console.error("AsyncServer.listen.accept[1]")
                        launchImmediately(coroutineContext) {
                            supervisorScope {
                                try {
                                    handler(client)
                                } catch (e: Throwable) {
                                    kotlin.runCatching { client.close() }
                                    if (e !is IOException && e !is CancellationException) {
                                        e.printStackTraceWithExtraMessage("Failed in AsyncServer.listen.handler")
                                    }
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        //Console.error("AsyncServer.listen.inner.catch: ${e::class}")
                        if (e is CancellationException || e is IOException) throw e
                        e.printStackTraceWithExtraMessage("Failed in AsyncServer.listen.accept")
                    }
                }
            } catch (e: Throwable) {
                //if (e is CancellationException) Console.error("AsyncServer.listen.cancel")
                // close socket?
                runCatching { close() }
            } finally {
                //Console.error("AsyncServer.listen.finally")
            }
		}
		return Closeable {
            //Console.error("AsyncServer.listen: Closing server...")
            job.cancel()
        }
	}

    suspend fun listenFlow(): Flow<AsyncClient> = flow { while (true) emit(accept()) }

    // Provide a default implementation
    override suspend fun close() {
    }
}
