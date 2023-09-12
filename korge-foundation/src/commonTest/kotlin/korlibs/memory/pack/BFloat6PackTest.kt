package korlibs.memory.pack

import kotlin.test.*

class BFloat6PackTest {
    @Test
    fun test() {
        val pack = bfloat6PackOf(1f, 2f, 3f, 4f, 5f, 6f)
        assertEquals(
            listOf(1f, 2f, 3f, 4f, 5f, 6f),
            listOf(pack.bf0, pack.bf1, pack.bf2, pack.bf3, pack.bf4, pack.bf5)
        )
    }
}
