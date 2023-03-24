package korlibs.memory.pack

import kotlin.test.*

class Half8PackTest {
    @Test
    fun test() {
        val pack = half8PackOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f)
        assertEquals(
            listOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f),
            listOf(pack.h0, pack.h1, pack.h2, pack.h3, pack.h4, pack.h5, pack.h6, pack.h7)
        )
    }
}