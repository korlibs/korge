package korlibs.io.net

import korlibs.io.async.AsyncThread2
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import javax.net.ssl.SSLSocketFactory

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

    override suspend fun connect(host: String, port: Int) {
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
    override suspend fun write(buffer: ByteArray, offset: Int, len: Int) {
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
