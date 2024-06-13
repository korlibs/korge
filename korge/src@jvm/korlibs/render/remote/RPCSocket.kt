package korlibs.render.remote

import korlibs.datastructure.*
import korlibs.graphics.*
import korlibs.graphics.log.*
import korlibs.image.color.*
import korlibs.io.async.*
import korlibs.memory.*
import korlibs.render.*
import kotlinx.coroutines.*
import java.net.*
import java.nio.*
import java.nio.channels.*
import java.nio.channels.CompletionHandler
import kotlin.coroutines.*

fun main() {
    runBlocking {
        val ag = AGLog()
        val gameWindow = GameWindow()
        println("Before Listening...")
        RPCServer.listen(InetSocketAddress(7771), handler = { server ->
            println("Listening... $server in ${server.server.localAddress}")
            val client = RPCClient(AsynchronousSocketChannel.open().also { it.connect(server.server.localAddress) })
            val gameWindow = RPCGameWindow(client)
            val ag = RPCAG(client)
            gameWindow.title = "hello"
            val fb = AGFrameBufferBase(isMain = true)
            ag.clear(fb, AGFrameBufferInfo.DEFAULT, Colors.RED)
            fb.close()
        }) {
            println("Connected client $it")
            CloseableGroup {
                this += RPCGameWindowExecutor(it, gameWindow)
                this += RPCAGExecutor(it, ag)
            }
        }
    }
}

class Packet(val id: Int, val data: ByteBuffer) {
    constructor(id: Int, data: ByteArray) : this(id, ByteBuffer.wrap(data))

    override fun toString(): String = "Packet(id=$id, data=${data.limit()})"
}

class RPCClient(val socket: AsynchronousSocketChannel) {
    val closeables = arrayListOf<AutoCloseable>()

    inline fun sendPacket(id: Int, size: Int, block: ByteBuffer.() -> Unit) {
        sendPacket(Packet(id, ByteBuffer.allocate(size).also(block).also { it.flipSafe() }))
    }

    fun sendPacket(id: Int, data: ByteArray) {
        sendPacket(Packet(id, data))
    }

    private val header = ByteBuffer.allocate(8)
    fun sendPacket(packet: Packet) {
        header.clearSafe()
        header.putInt(packet.id)
        header.putInt(packet.data.limit())
        header.flipSafe()

        socket.write(header)
        socket.write(packet.data)
    }

    suspend fun recvPacket(): Packet {
        val header = ByteBuffer.allocate(8)
        asyncHandler { socket.read(header, null, it) }
        header.flipSafe()
        val id = header.getInt()
        val len = header.getInt()
        val data = ByteBuffer.allocate(len)
        asyncHandler { socket.read(data, null, it) }
        data.flipSafe()
        return Packet(id, data)
    }

    val handlers = arrayListOf<Pair<IntRange, (Packet) -> Unit>>()

    fun registerPacketHandler(range: IntRange, handler: (Packet) -> Unit): AutoCloseable {
        val h = range to handler
        handlers += h
        return AutoCloseable { handlers -= h }
    }

    suspend fun receiveLoop() {
        while (true) {
            val p = recvPacket()
            println("RECEIVED $p")
            for (handler in handlers) {
                if (p.id in handler.first) {
                    handler.second(p)
                }
            }
        }
    }
}

class RPCServer(val local: SocketAddress, val backlog: Int = 0, val clientCallback: (RPCClient) -> AutoCloseable = { AutoCloseable { } }) {
    val server = AsynchronousServerSocketChannel.open()

    companion object {
        suspend fun listen(local: SocketAddress, backlog: Int = 0, handler: (RPCServer) -> Unit = {}, clientCallback: (RPCClient) -> AutoCloseable = { AutoCloseable { } }) {
            RPCServer(local, backlog, clientCallback).listen(handler)
        }
    }

    suspend fun listen(handler: (RPCServer) -> Unit = {}) {
        withContext(Dispatchers.IO) { server.bind(local, backlog) }
        handler(this)
        while (true) {
            val client = asyncHandler { server.accept(null, it) }
            launchImmediately(coroutineContext) {
                RPCClient(client).also { it.closeables += clientCallback(it) }.receiveLoop()
            }
        }
    }
}

private suspend inline fun <T, R> asyncHandler(crossinline block: (CompletionHandler<T, R>) -> Unit): T {
    return suspendCancellableCoroutine { c: CancellableContinuation<T> ->
        block(object : CompletionHandler<T, R> {
            override fun completed(result: T, attachment: R) = c.resume(result)
            override fun failed(exc: Throwable, attachment: R) = c.resumeWithException(exc)
        })
    }
}

// @TODO: Move to korlibs
private class CloseableGroup : AutoCloseable {
    companion object {
        operator fun invoke(block: CloseableGroup.() -> Unit): CloseableGroup = CloseableGroup().apply(block)
    }

    val items = Deque<AutoCloseable>()

    operator fun plusAssign(other: AutoCloseable) {
        items += other
    }

    override fun close() {
        while (items.isNotEmpty()) {
            items.removeFirst().close()
        }
    }
}
