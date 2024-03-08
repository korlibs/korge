package korlibs.io.util

import kotlin.test.Test
import kotlin.test.assertEquals

class StrReaderTest {
    @Test
    fun test() {
        val sr = StrReader("testing")
        sr.skip(4)
        assertEquals(4, sr.pos)
        assertEquals(null, sr.matchLit("ling"))
        assertEquals(4, sr.pos)
        assertEquals("ing", sr.matchLit("ing"))
        assertEquals(7, sr.pos)
    }
}
