package korlibs.korge.ipc

import java.io.*

data class KorgeIPCInfo(val path: String = DEFAULT_PATH) {
    companion object {
        val KORGE_IPC_prop get() = System.getProperty("korge.ipc")
        val KORGE_IPC_env get() = System.getenv("KORGE_IPC")

        val DEFAULT_PATH_OR_NULL = KORGE_IPC_prop
            ?: KORGE_IPC_env

        val DEFAULT_PATH = DEFAULT_PATH_OR_NULL
            ?: "${System.getProperty("java.io.tmpdir")}/KORGE_IPC-unset"
        val PROCESS_PATH = "${System.getProperty("java.io.tmpdir")}/KORGE_IPC-${ProcessHandle.current().pid()}"
    }
}

fun KorgeIPCInfo.createIPC(): KorgeIPC = KorgeIPC(path)

class KorgeIPC(val path: String = KorgeIPCInfo.DEFAULT_PATH) : AutoCloseable {
    init {
        println("KorgeIPC:$path")
    }

    val framePath = "$path.frame"
    val socketPath = "$path.socket"

    val frame = KorgeFrameBuffer(framePath)
    //val events = KorgeOldEventsBuffer("$path.events")

    private val _events = ArrayDeque<IPCPacket>()

    private val connectedSockets = LinkedHashSet<KorgeIPCSocket>()

    var onConnect: ((socket: KorgeIPCSocket) -> Unit)? = null
    var onClose: ((socket: KorgeIPCSocket) -> Unit)? = null
    var onEvent: ((socket: KorgeIPCSocket, e: IPCPacket) -> Unit)? = null

    val socket = KorgeIPCSocket.openOrListen(socketPath, object : KorgeIPCSocketListener {
        var isServer = false

        override fun onServerStarted(socket: KorgeIPCServerSocket) {
            isServer = true
        }

        override fun onConnect(socket: KorgeIPCSocket) {
            synchronized(connectedSockets) {
                connectedSockets += socket
                println("onConnect[$socketPath][$socket] : ${connectedSockets.size}")
            }
            onConnect?.invoke(socket)
        }

        override fun onClose(socket: KorgeIPCSocket) {
            synchronized(connectedSockets) {
                connectedSockets -= socket
                println("onClose[$socketPath][$socket] : ${connectedSockets.size}")
            }
            onClose?.invoke(socket)
        }

        override fun onEvent(socket: KorgeIPCSocket, e: IPCPacket) {
            //println("onEvent[$socketPath][$socket]: $e")
            synchronized(_events) {
                _events += e
            }
            this@KorgeIPC.onEvent?.invoke(socket, e)
            // BROAD CAST PACKETS SO ALL THE CLIENTS ARE OF THE EVENT
            if (isServer) {
                for (sock in connectedSockets) {
                    if (sock == socket) continue
                    sock.writePacket(e)
                }
            }
        }
    }, serverDeleteOnExit = true)

    val availableEvents get() = synchronized(_events) { _events.size }
    fun writeEvent(e: IPCPacket) {
        synchronized(connectedSockets) {
            for (socket in connectedSockets) {
                socket.writePacket(e)
            }
        }
    }
    fun tryReadEvent(): IPCPacket? {
        return synchronized(_events) { _events.removeLastOrNull() }
    }
    fun readEvent(): IPCPacket {
        while (socket.isOpen) {
            tryReadEvent()?.let { return it }
            Thread.sleep(1L)
        }
        error("Socket is closed")
    }

    //val availableEvents get() = events.availableRead
    //fun resetEvents() {
    //    events.reset()
    //}
    //fun writeEvent(e: IPCOldEvent) = events.writeEvent(e)
    //fun readEvent(e: IPCOldEvent = IPCOldEvent()): IPCOldEvent? = events.readEvent(e)
    fun setFrame(f: IPCFrame) = frame.setFrame(f)
    fun getFrame(): IPCFrame = frame.getFrame()
    fun getFrameId(): Int = frame.getFrameId()

    override fun close() {
        println("/KorgeIPC:$path")
        frame.close()
        //events.close()
        socket.close()
    }

    fun closeAndDelete() {
        close()
        File(socketPath).delete()
        File(framePath).delete()
        //socket.delete()
    }
}
