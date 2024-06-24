package korlibs.korge.ipc

import java.nio.*
import kotlin.test.*

class KorgeIPCServerSocketTest {
    val TMP = System.getProperty("java.io.tmpdir")
    val address = "$TMP/korge-demo-${System.currentTimeMillis()}.sock"

    @Test
    fun testIPC(): Unit {
        val address = "$TMP/demo1.sock"
        val ipc1 = KorgeIPC(address, isServer = true)
        val ipc2 = KorgeIPC(address, isServer = false)
        ipc1.onEvent = { socket, e -> println("EVENT1: $socket, $e") }
        ipc2.onEvent = { socket, e -> println("EVENT2: $socket, $e") }
        ipc1.waitConnected()
        ipc1.writeEvent(IPCPacket(777))
        assertEquals(777, ipc2.readEvent().type)
        ipc2.writeEvent(IPCPacket(888))
        assertEquals(888, ipc1.readEvent().type)

        ipc1.closeAndDelete()
        ipc2.closeAndDelete()
    }

    @Test
    fun testListen(): Unit {
        val logS = arrayListOf<String>()
        val logC = arrayListOf<String>()

        val server = KorgeIPCServerSocket.listen(address, object : KorgeIPCSocketListener {
            override fun onConnect(socket: KorgeIPCSocket) {
                logS += "onConnect[CLI->SER][$socket]"
            }

            override fun onClose(socket: KorgeIPCSocket) {
                logS += "onClose[CLI->SER][$socket]"
            }

            override fun onEvent(socket: KorgeIPCSocket, e: IPCPacket) {
                logS += "onEvent[CLI->SER][$socket]: $e"
                socket.writePacket(IPCPacket(2, data = "OK!".toByteArray()))
            }
        })
        val socket = KorgeIPCSocket.open(address, object : KorgeIPCSocketListener {
            override fun onConnect(socket: KorgeIPCSocket) {
                logC += "onConnect[SER->CLI][$socket]"
            }

            override fun onClose(socket: KorgeIPCSocket) {
                logC += "onClose[SER->CLI][$socket]"
            }

            override fun onEvent(socket: KorgeIPCSocket, e: IPCPacket) {
                logC += "onEvent[SER->CLI][$socket]: $e"
                //socket.writePacket(Packet(2, "OK!".toByteArray()))
            }
        }, id = -1L)

        socket.writePacket(IPCPacket(1, data = "HELLO".toByteArray()))
        Thread.sleep(10L)
        socket.close()
        Thread.sleep(10L)
        server.close()

        assertEquals("""
            onConnect[CLI->SER][KorgeIPCSocket(0)]
            onEvent[CLI->SER][KorgeIPCSocket(0)]: Packet(type=0x1, data=bytes[5])
            onClose[CLI->SER][KorgeIPCSocket(0)]
            onConnect[SER->CLI][KorgeIPCSocket(-1)]
            onEvent[SER->CLI][KorgeIPCSocket(-1)]: Packet(type=0x2, data=bytes[3])
            onClose[SER->CLI][KorgeIPCSocket(-1)]
        """.trimIndent(), logS.joinToString("\n") + "\n" + logC.joinToString("\n"))
    }

    @Test
    fun testUnixSocket() {
        val serverSocket = KorgeUnixSocket.bind(address)
        val socket2 = KorgeUnixSocket.open(address)
        val socket = serverSocket.accept()
        socket.write(ByteBuffer.wrap("HELLO".toByteArray()))

        val buffer = ByteBuffer.allocate(100)
        socket2.read(buffer)
        buffer.flip()
        val bytes = ByteArray(buffer.limit())
        buffer.get(bytes)

        assertEquals("HELLO", bytes.decodeToString())
    }
}
