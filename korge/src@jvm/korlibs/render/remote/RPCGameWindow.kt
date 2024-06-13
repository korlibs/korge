package korlibs.render.remote

import korlibs.io.util.*
import korlibs.render.*


class RPCGameWindowExecutor(val client: RPCClient, val gameWindow: GameWindow) : AutoCloseable {
    val handler = client.registerPacketHandler(2000 until 3000, ::handle)

    override fun close() {
        handler.close()
    }

    fun handle(packet: Packet) {
        val id = packet.id
        val data = packet.data
        when (id) {
            RPCGameWindow.CLIENT_SET_TITLE -> {
                gameWindow.title = data.toByteArray().decodeToString()
                println("gameWindow.title=${gameWindow.title}")
            }
        }
    }
}

class RPCGameWindow(val client: RPCClient) : GameWindow() {
    companion object {
        const val CLIENT_SET_TITLE = 2001

        const val SERVER_DISPATCH_INIT = 2101
    }

    override var title: String = ""
        set(value) {
            field = value
            client.sendPacket(CLIENT_SET_TITLE, value.encodeToByteArray())
        }

    val handler = client.registerPacketHandler(2000 until 3000, ::handle)

    fun handle(packet: Packet) {
        val id = packet.id
        val data = packet.data
        when (id) {
            SERVER_DISPATCH_INIT -> dispatchInitEvent()
        }
    }

    override fun close(exitCode: Int) {
        handler.close()
        super.close(exitCode)
    }
}
