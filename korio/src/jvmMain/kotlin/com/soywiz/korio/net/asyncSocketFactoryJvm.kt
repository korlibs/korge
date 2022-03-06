package com.soywiz.korio.net

import com.soywiz.korio.async.*
import com.soywiz.korio.net.ssl.*
import kotlinx.coroutines.*
import java.io.*
import java.net.*
import java.nio.*
import java.nio.channels.*
import java.nio.channels.CompletionHandler
import java.util.concurrent.*
import javax.net.ssl.*
import kotlin.coroutines.*

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
                JvmAsyncServerSocketChannel(port, host, backlog).apply { init() }
            }
	}
}

private suspend fun <T> doIo(block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.IO, block)

class JvmNioAsyncClient(private var client: AsynchronousSocketChannel? = null) : AsyncClient {
    fun <T> CancellableContinuation<T>.getCompletionHandler(): CompletionHandler<T, Unit> {
        val continuation = this
        var cancelled = false
        invokeOnCancellation {
            cancelled = true
        }
        //println("JvmNioAsyncClient.Started in thread ${Thread.currentThread().id}")

        return object : CompletionHandler<T, Unit> {
            override fun completed(result: T, attachment: Unit?) {
                //println("JvmNioAsyncClient.Completed result=$result in thread ${Thread.currentThread().id}")
                if (!cancelled) continuation.resume(result)
            }
            override fun failed(exc: Throwable, attachment: Unit?) { if (!cancelled) continuation.resumeWithException(exc) }
        }
    }

    private suspend fun <T> suspendCompletion(
        block: (CompletionHandler<T, Unit>) -> Unit
    ): T = suspendCancellableCoroutine {
        block(it.getCompletionHandler())
    }

    override val address: AsyncAddress get() = client?.remoteAddress.toAsyncAddress()

    override suspend fun connect(host: String, port: Int): Unit {
        val client = doIo { AsynchronousSocketChannel.open() }
        this.client = client
        suspendCompletion<Void> { client.connect(InetSocketAddress(host, port), Unit, it) }
    }

    override val connected: Boolean get() = client?.isOpen ?: false

    override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
        return suspendCompletion<Int> { client!!.read(ByteBuffer.wrap(buffer, offset, len), 0L, TimeUnit.MILLISECONDS, Unit, it) }.toInt()
    }
    override suspend fun write(buffer: ByteArray, offset: Int, len: Int): Unit {
        suspendCompletion<Int> { client!!.write(ByteBuffer.wrap(buffer, offset, len), 0L, TimeUnit.MILLISECONDS, Unit, it) }
    }

    override suspend fun close() {
        doIo { client?.close() }
        client = null
    }
}

class JvmAsyncClient(private var socket: Socket? = null, val secure: Boolean = false) : AsyncClient {
    //private val queue = AsyncThread()
    //private val connectionQueue = queue
    //private val readQueue = queue
    //private val writeQueue = queue

    private val connectionQueue = AsyncThread2()
    private val readQueue = AsyncThread2()
    private val writeQueue = AsyncThread2()

    private var socketIs: InputStream? = null
    private var socketOs: OutputStream? = null

    init {
        setStreams()
    }

    private fun setStreams() {
        socketIs = socket?.getInputStream()
        socketOs = socket?.getOutputStream()
    }

    override val address: AsyncAddress get() = socket?.remoteSocketAddress.toAsyncAddress()

    override suspend fun connect(host: String, port: Int): Unit {
        connectionQueue {
            doIo {
                socket = if (secure) SSLSocketFactory.getDefault().createSocket(host, port) else Socket(host, port)
                setStreams()
            }
        }
    }

    override val connected: Boolean get() = socket?.isConnected ?: false

    override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
        return readQueue { doIo { socketIs?.read(buffer, offset, len) ?: -1 } }
    }
    override suspend fun write(buffer: ByteArray, offset: Int, len: Int): Unit {
        writeQueue { doIo { socketOs?.write(buffer, offset, len) } }
    }

    override suspend fun close() {
        connectionQueue {
            doIo {
                socket?.close()
                socketIs?.close()
                socketOs?.close()
                socket = null
                socketIs = null
                socketOs = null
            }
        }
    }
}

class JvmAsyncServer(
    override val requestPort: Int,
    override val host: String,
    override val backlog: Int = -1,
    val secure: Boolean = false
) : AsyncServer {
    val ssc = if (secure) SSLServerSocketFactory.getDefault().createServerSocket() else ServerSocket()
    suspend fun init() = doIo { ssc.bind(InetSocketAddress(host, requestPort), backlog) }
    override val port: Int get() = (ssc.localSocketAddress as? InetSocketAddress)?.port ?: -1
    override suspend fun accept(): AsyncClient = JvmAsyncClient(doIo { ssc.accept() })
}
