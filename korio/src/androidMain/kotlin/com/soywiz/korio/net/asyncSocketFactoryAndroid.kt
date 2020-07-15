package com.soywiz.korio.net

import com.soywiz.korio.async.*
import com.soywiz.korio.concurrent.atomic.*
import kotlinx.coroutines.*
import java.io.*
import java.net.*
import javax.net.ssl.SSLServerSocketFactory
import javax.net.ssl.SSLSocketFactory

internal actual val asyncSocketFactory: AsyncSocketFactory by lazy {
	object : AsyncSocketFactory() {
		override suspend fun createClient(secure: Boolean): AsyncClient = JvmAsyncClient(secure = secure)
		override suspend fun createServer(port: Int, host: String, backlog: Int, secure: Boolean): AsyncServer =
			JvmAsyncServer(port, host, backlog, secure = secure).apply { init() }
	}
}

@PublishedApi
internal val socketDispatcher get() = Dispatchers.IO
private suspend inline fun <T> doIo(crossinline block: () -> T): T = withContext(socketDispatcher) { block() }

class JvmAsyncClient(private var socket: Socket? = null, val secure: Boolean = false) : AsyncClient {
    private val readQueue = AsyncThread()
    private val writeQueue = AsyncThread()

    private var socketIs: InputStream? = null
    private var socketOs: OutputStream? = null

    init {
        setStreams()
    }

    private fun setStreams() {
        socketIs = socket?.getInputStream()
        socketOs = socket?.getOutputStream()
    }

    override suspend fun connect(host: String, port: Int): Unit = doIo {
        socket = if (secure) SSLSocketFactory.getDefault().createSocket(host, port) else Socket(host, port)
        setStreams()
    }

    override val connected: Boolean get() = socket?.isConnected ?: false

    override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int = readQueue { doIo { socketIs?.read(buffer, offset, len) ?: -1 } }
    override suspend fun write(buffer: ByteArray, offset: Int, len: Int): Unit = writeQueue { doIo { socketOs?.write(buffer, offset, len) }.let { Unit } }

    override suspend fun close() {
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
