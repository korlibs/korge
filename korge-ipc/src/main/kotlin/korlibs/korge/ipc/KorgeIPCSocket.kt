package korlibs.korge.ipc

import java.io.*
import java.net.BindException
import java.net.ConnectException
import java.net.SocketException
import java.nio.*
import java.nio.channels.*
import java.util.concurrent.*

private val threadPool = Executors.newCachedThreadPool()

interface KorgeIPCSocketListener {
    fun onConnect(socket: KorgeIPCSocket) {}
    fun onClose(socket: KorgeIPCSocket) {}
    fun onEvent(socket: KorgeIPCSocket, e: IPCPacket) {}
}

interface BaseKorgeIPCSocket : AutoCloseable {
    val isOpen: Boolean
}

class KorgeIPCSocket(var socketOpt: SocketChannel?, val id: Long) : BaseKorgeIPCSocket {
    override fun toString(): String = "KorgeIPCSocket($id)"
    val socket get() = socketOpt ?: error("Socket is closed")
    fun writePacket(packet: IPCPacket) = IPCPacket.write(socket, packet)
    fun readPacket(): IPCPacket = IPCPacket.read(socket)

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

    override val isOpen: Boolean get() = socketOpt?.isOpen == true

    companion object {
        fun openOrListen(path: String, listener: KorgeIPCSocketListener, server: Boolean? = null, serverDeleteOnExit: Boolean = false): BaseKorgeIPCSocket {
            return when (server) {
                true -> listen(path, listener, delete = false)
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
            val server = KorgeUnixSocket.bind(path, delete = delete, deleteOnExit = false)
            threadPool.submit {
                while (true) {
                    val socket = KorgeIPCSocket(server.accept(), id++)

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
            return KorgeIPCServerSocket(server)
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

    override val isOpen: Boolean get() = socket.isOpen

    override fun close() {
        socket.close()
    }
}

//fun SocketChannel.writePacket(packet: Packet) = Packet.write(this, packet)
//fun SocketChannel.readPacket(): Packet = Packet.read(this)

class IPCPacket(
    var type: Int = -1,
    var p0: Int = 0,
    var p1: Int = 0,
    var p2: Int = 0,
    var p3: Int = 0,
    var data: ByteArray = byteArrayOf()
) {
    override fun toString(): String = "Packet(type=$type)"

    companion object {
        val RESIZE = 1
        val BRING_BACK = 2
        val BRING_FRONT = 3

        val MOUSE_MOVE = 10
        val MOUSE_DOWN = 11
        val MOUSE_UP = 12
        val MOUSE_CLICK = 13

        val KEY_DOWN = 20
        val KEY_UP = 21
        val KEY_TYPE = 22

        fun write(socket: SocketChannel, packet: IPCPacket) {
            val head = ByteBuffer.allocate(4 + 4 + (4 * 4) + packet.data.size)
            head.putInt(packet.type)
            head.putInt(packet.p0)
            head.putInt(packet.p1)
            head.putInt(packet.p2)
            head.putInt(packet.p3)
            head.putInt(packet.data.size)
            head.put(packet.data)
            head.flip()
            socket.write(head)
        }

        fun read(socket: SocketChannel): IPCPacket {
            val head = ByteBuffer.allocate(4 + 4 + (4 * 4))
            socket.read(head)
            head.flip()
            val type = head.int
            val p0 = head.int
            val p1 = head.int
            val p2 = head.int
            val p3 = head.int
            val size = head.int
            val data = ByteArray(size)
            if (size > 0) {
                socket.read(ByteBuffer.wrap(data))
            }
            return IPCPacket(type, p0, p1, p2, p3, data)
        }
    }
}
