package korlibs.wasm

import korlibs.template.*
import kotlin.test.*

class WASMLibTest {
    @Test
    fun test() = suspendTest {
        val adder = ADDER.also { it.initOnce(coroutineContext) }
        assertEquals(16, adder.add(7, 9))
    }

    object ADDER : WASMLib(byteArrayOf(
        0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x0a, 0x02, 0x60, 0x02, 0x7f, 0x7f, 0x01,
        0x7f, 0x60, 0x00, 0x00, 0x03, 0x03, 0x02, 0x00, 0x01, 0x04, 0x04, 0x01, 0x70, 0x00, 0x01, 0x05,
        0x03, 0x01, 0x00, 0x00, 0x06, 0x06, 0x01, 0x7f, 0x00, 0x41, 0x08, 0x0b, 0x07, 0x18, 0x03, 0x06,
        0x6d, 0x65, 0x6d, 0x6f, 0x72, 0x79, 0x02, 0x00, 0x05, 0x74, 0x61, 0x62, 0x6c, 0x65, 0x01, 0x00,
        0x03, 0x61, 0x64, 0x64, 0x00, 0x00, 0x09, 0x07, 0x01, 0x00, 0x41, 0x00, 0x0b, 0x01, 0x01, 0x0a,
        0x0c, 0x02, 0x07, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6a, 0x0b, 0x02, 0x00, 0x0b
    )) {
        fun add(a: Int, b: Int): Int = invokeFuncInt("add", a, b)
    }

    val hello_world_wasm = byteArrayOf(
        0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x0a, 0x02, 0x60, 0x02, 0x7f, 0x7f, 0x01,
        0x7f, 0x60, 0x00, 0x00, 0x03, 0x03, 0x02, 0x00, 0x01, 0x04, 0x04, 0x01, 0x70, 0x00, 0x01, 0x05,
        0x03, 0x01, 0x00, 0x00, 0x06, 0x06, 0x01, 0x7f, 0x00, 0x41, 0x08, 0x0b, 0x07, 0x18, 0x03, 0x06,
        0x6d, 0x65, 0x6d, 0x6f, 0x72, 0x79, 0x02, 0x00, 0x05, 0x74, 0x61, 0x62, 0x6c, 0x65, 0x01, 0x00,
        0x03, 0x61, 0x64, 0x64, 0x00, 0x00, 0x09, 0x07, 0x01, 0x00, 0x41, 0x00, 0x0b, 0x01, 0x01, 0x0a,
        0x0c, 0x02, 0x07, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6a, 0x0b, 0x02, 0x00, 0x0b
    )

}
