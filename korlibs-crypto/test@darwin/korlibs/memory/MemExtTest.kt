package korlibs.memory

import kotlinx.cinterop.usePinned
import kotlin.test.Test
import kotlin.test.assertNotNull

class MemExtTest {
    @Test
    fun testStartAddressOf() {
        assertNotNull(byteArrayOf(1).usePinned { it.startAddressOf })
        assertNotNull(byteArrayOf().usePinned { it.startAddressOf })
    }
}
