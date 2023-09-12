package korlibs.memory.pack

import kotlin.test.*

class Float4PackTest {
    @Test
    fun test() {
        val pack = float4PackOf(1f, 2f, 3f, 4f)
        assertEquals(1f, pack.f0)
        assertEquals(2f, pack.f1)
        assertEquals(3f, pack.f2)
        assertEquals(4f, pack.f3)

        assertEquals(float4PackOf(1f, 2f, 3f, 4f), float4PackOf(1f, 2f, 3f, 4f))
    }
}
