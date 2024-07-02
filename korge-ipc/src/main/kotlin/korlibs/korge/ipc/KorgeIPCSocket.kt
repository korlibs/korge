package korlibs.korge.ipc

import korlibs.io.stream.*
import korlibs.memory.*
import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.*
import java.net.*
import java.nio.*
import java.nio.channels.*
import java.util.concurrent.*

internal val threadPool = Executors.newCachedThreadPool()

interface KorgeIPCSocketListener {
    fun onServerStarted(socket: KorgeIPCServerSocket) {}
    fun onConnect(socket: KorgeIPCSocket) {}
    fun onClose(socket: KorgeIPCSocket) {}
    fun onEvent(socket: KorgeIPCSocket, e: IPCPacket) {}
}

interface BaseKorgeIPCSocket : AutoCloseable {
    val isServer: Boolean
    val isOpen: Boolean
}

class KorgeIPCSocket(var socketOpt: SocketChannel?, val id: Long) : BaseKorgeIPCSocket {
    override fun toString(): String = "KorgeIPCSocket($id)"
    val socket get() = socketOpt ?: error("Socket is closed")
    fun writePacket(packet: IPCPacket) = IPCPacket.write(socket, packet)
    fun readPacket(): IPCPacket = IPCPacket.read(socket).also { it.optSocket = this }

    private var openSocket: Future<*>? = null

    private fun open(path: String, listener: KorgeIPCSocketListener) {
        socketOpt = KorgeUnixSocket.open(path)
        openSocket = threadPool.submit {
            listener.onConnect(this)
            try {
                while (true) {
                    listener.onEvent(this, readPacket())
                }
            } catch (e: ClosedChannelException) {
                // Do nothing
            } catch (e: SocketException) {
                // Do nothing
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                listener.onClose(this)
            }
        }
    }

    override fun close() {
        socketOpt?.close()
        openSocket?.cancel(true)
        openSocket = null
    }

    override val isServer: Boolean get() = false

    override val isOpen: Boolean get() = socketOpt?.isOpen == true

    companion object {
        fun openOrListen(path: String, listener: KorgeIPCSocketListener, server: Boolean? = null, serverDelete: Boolean = false, serverDeleteOnExit: Boolean = false): BaseKorgeIPCSocket {
            return when (server) {
                true -> listen(path, listener, delete = serverDelete, deleteOnExit = serverDeleteOnExit)
                false -> open(path, listener)
                null -> try {
                    open(path, listener)
                } catch (e: ConnectException) {
                    //e.printStackTrace()
                    //File(path).deleteOnExit()
                    try {
                        listen(path, listener, deleteOnExit = serverDeleteOnExit)
                    } catch (e: BindException) {
                        File(path).delete()
                        listen(path, listener, delete = true, deleteOnExit = serverDeleteOnExit)
                    }
                }
            }
        }

        fun listen(path: String, listener: KorgeIPCSocketListener, delete: Boolean = false, deleteOnExit: Boolean = false): KorgeIPCServerSocket {
            return KorgeIPCServerSocket.listen(path, listener, delete, deleteOnExit).also {
                println("KorgeIPCServerSocket.listen:$path")
            }
        }
        fun open(path: String, listener: KorgeIPCSocketListener, id: Long = 0L): KorgeIPCSocket {
            return KorgeIPCSocket(null, id).also { it.open(path, listener) }.also {
                println("KorgeIPCServerSocket.open:$path")
            }
        }
    }
}

class KorgeIPCServerSocket(val socket: ServerSocketChannel) : BaseKorgeIPCSocket {
    companion object {
        fun listen(path: String, listener: KorgeIPCSocketListener, delete: Boolean = false, deleteOnExit: Boolean = false): KorgeIPCServerSocket {
            var id = 0L
            val server = KorgeIPCServerSocket(KorgeUnixSocket.bind(path, delete = delete, deleteOnExit = false))
            threadPool.submit {
                listener.onServerStarted(server)
                while (true) {
                    val socket = KorgeIPCSocket(server.socket.accept(), id++)

                    threadPool.submit {
                        try {
                            socket.use {
                                listener.onConnect(socket)
                                try {
                                    while (true) {
                                        listener.onEvent(socket, socket.readPacket())
                                    }
                                } finally {
                                    listener.onClose(socket)
                                }
                            }
                        } catch (e: ClosedChannelException) {
                            // Do nothing
                        } catch (e: SocketException) {
                            // Do nothing
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            return server
        }

        //fun bind(): kotlinx.coroutines.flow.Flow<AsynchronousSocketChannel> = flow {
        //    val channel = KorgeUnixSocket.bindAsync(path)
        //    try {
        //        while (true) {
        //            emit(channel.acceptSuspend())
        //        }
        //    } catch (e: CancellationException) {
        //        channel.close()
        //    }
        //}
    }

    override val isServer: Boolean get() = true

    override val isOpen: Boolean get() = socket.isOpen

    override fun close() {
        socket.close()
    }
}

//fun SocketChannel.writePacket(packet: Packet) = Packet.write(this, packet)
//fun SocketChannel.readPacket(): Packet = Packet.read(this)

class IPCPacket(
    val type: Int = -1,
    val data: ByteArray = byteArrayOf(),
) {
    val buffer = ByteBuffer.wrap(data)
    val ibuffer = buffer.asIntBuffer()
    val dataString by lazy { data.decodeToString() }

    var optSocket: KorgeIPCSocket? = null
    val socket: KorgeIPCSocket get() = optSocket ?: error("No socket")

    override fun toString(): String = "Packet(type=0x${type.toString(16)}, data=bytes[${data.size}])"

    inline fun <reified T> parseJson(): T = Json.decodeFromString<T>(dataString)

    companion object {
        operator fun invoke(type: Int, size: Int, block: (ByteBuffer) -> Unit): IPCPacket {
            val data = ByteArray(size)
            val buffer = ByteBuffer.wrap(data)
            block(buffer)
            buffer.flip()
            return IPCPacket(type, data)
        }

        operator fun invoke(type: Int, block: SyncOutputStream.() -> Unit): IPCPacket {
            return IPCPacket(type, MemorySyncStreamToByteArray(128, block))
        }

        inline fun <reified T> fromJson(type: Int, value: T): IPCPacket =
            IPCPacket(type, Json.encodeToString<T>(value).encodeToByteArray())

        val RESIZE = 0x0101
        val BRING_BACK = 0x0102
        val BRING_FRONT = 0x0103

        val MOUSE_MOVE = 0x0201
        val MOUSE_DOWN = 0x0202
        val MOUSE_UP = 0x0203
        val MOUSE_CLICK = 0x0204
        val MOUSE_SCROLL = 0x0205

        val KEY_DOWN = 0x0301
        val KEY_UP = 0x0302
        val KEY_TYPE = 0x0303

        val EVENT_GAME_TO_PROJECTOR = 0x0401
        val EVENT_PROJECTOR_TO_GAME = 0x0402

        val REQUEST_NODE_CHILDREN = 0x7701
        val REQUEST_NODE_PROPS = 0x7702
        val REQUEST_NODE_SET_PROP = 0x7703

        val RESPONSE_NODE_CHILDREN = 0x7801
        val RESPONSE_NODE_PROPS = 0x7802
        val RESPONSE_NODE_SET_PROP = 0x7803

        fun write(socket: SocketChannel, packet: IPCPacket) {
            val head = ByteBuffer.allocate(8 + packet.data.size)
            head.putInt(packet.type)
            head.putInt(packet.data.size)
            head.put(packet.data)
            head.flip()
            socket.write(head)
        }

        fun ReadableByteChannel.readFull(dst: ByteBuffer) {
            while (dst.remaining() > 0) {
                val read = read(dst)
                if (read <= 0) error("Couldn't read")
            }
        }

        fun read(socket: SocketChannel): IPCPacket {
            val head = ByteBuffer.allocate(8)
            socket.readFull(head)
            head.flip()
            val type = head.int
            val size = head.int
            val data = ByteArray(size)
            if (size > 0) {
                socket.readFull(ByteBuffer.wrap(data))
            }
            return IPCPacket(type, data)
        }
    }
}

private operator fun IntBuffer.set(index: Int, value: Int) {
    this.put(index, value)
}

inline fun IPCPacket.Companion.packetInts(type: Int, vararg pp: Int): IPCPacket = IPCPacket(type, pp.size * 4) {
    for (p in pp) it.putInt(p)
}

//fun IPCPacket.Companion.packetInts2(type: Int, p0: Int, p1: Int): IPCPacket = IPCPacket(type, 8) {
//    it.putInt(p0)
//    it.putInt(p1)
//}
//
//fun IPCPacket.Companion.packetInts3(type: Int, p0: Int, p1: Int, p2: Int): IPCPacket = IPCPacket(type, 12) {
//    it.putInt(p0)
//    it.putInt(p1)
//    it.putInt(p2)
//}

fun IPCPacket.Companion.keyPacket(type: Int, keyCode: Int, char: Int): IPCPacket = packetInts(type, keyCode, char)
fun IPCPacket.Companion.mousePacket(
    type: Int, x: Int, y: Int, button: Int,
    scrollX: Float = 0f, scrollY: Float = 0f, scrollZ: Float = 0f,
): IPCPacket = packetInts(type, x, y, button, scrollX.reinterpretAsInt(), scrollY.reinterpretAsInt(), scrollZ.reinterpretAsInt())
fun IPCPacket.Companion.resizePacket(type: Int, width: Int, height: Int, scale: Float = 1f): IPCPacket = packetInts(type, width, height, scale.reinterpretAsInt())
fun IPCPacket.Companion.nodePacket(type: Int, nodeId: Int): IPCPacket = packetInts(type, nodeId)

fun IPCPacket.Companion.requestNodeChildrenPacket(nodeId: Int): IPCPacket = nodePacket(IPCPacket.REQUEST_NODE_CHILDREN, nodeId)
fun IPCPacket.Companion.requestNodePropPacket(nodeId: Int): IPCPacket = nodePacket(IPCPacket.REQUEST_NODE_PROPS, nodeId)

@Serializable
data class IPCNodeInfo(
    val nodeId: Long,
    val isContainer: Boolean,
    val className: String,
    val name: String,
) {
    //val flags: Int get() = 0.insert(isContainer, 0)
}

@Serializable
data class IPCNodeChildrenRequest(val nodeId: Long) {
    companion object {
        val ID = IPCPacket.REQUEST_NODE_CHILDREN
    }
}

@Serializable
data class IPCNodeChildrenResponse(
    val nodeId: Long,
    val parentNodeId: Long,
    val children: List<IPCNodeInfo>?
) {
    companion object {
        val ID = IPCPacket.RESPONSE_NODE_CHILDREN
    }
}

@Serializable
class IPCPropInfo(
    val callId: String,
    val showName: String,
    val propType: String,
    val value: String?,
) {
    companion object {
        val ID = IPCPacket.RESPONSE_NODE_PROPS
    }
}

@Serializable
data class IPCNodePropsRequest(val nodeId: Long) {
    companion object {
        val ID = IPCPacket.REQUEST_NODE_PROPS
    }
}

@Serializable
data class IPCNodePropsResponse(
    val nodeId: Long,
    val parentNodeId: Long,
    val propGroups: Map<String, List<IPCPropInfo>>,
) {
    companion object {
        val ID = IPCPacket.RESPONSE_NODE_PROPS
    }
}

@Serializable
data class IPCPacketPropSetRequest(
    val nodeId: Long,
    val callId: String,
    val value: String?,
) {
    companion object {
        val ID = IPCPacket.REQUEST_NODE_SET_PROP
    }
}

@Serializable
data class IPCPacketPropSetResponse(
    val nodeId: Long,
    val callId: String,
    val value: String?,
) {
    companion object {
        val ID = IPCPacket.RESPONSE_NODE_SET_PROP
    }
}
