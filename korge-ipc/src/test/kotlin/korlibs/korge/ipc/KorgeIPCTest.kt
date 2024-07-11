package korlibs.korge.ipc

import org.junit.*
import org.junit.Test
import kotlin.test.*

class KorgeIPCTest {
    @Test
    fun test() {
        assertEquals(
            """{"nodeId":1}""",
            IPCPacket.fromJson(IPCPacket.REQUEST_NODE_PROPS, IPCNodePropsRequest(1L)).dataString
        )
    }
}
