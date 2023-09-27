package korlibs.number

import kotlin.test.*

class BFloatTest {
    @Test
    fun test() {
        assertEquals(1f, BFloat(1f).float)
        assertEquals(.5f, BFloat(.5f).float)
        assertEquals(.125f, BFloat(.125f).float)
        assertEquals(.75f, BFloat(.75f).float)
        assertEquals(0f, BFloat(0f).float)
        assertEquals(2f, BFloat(2f).float)
        assertEquals(1.5f, BFloat(1.5f).float)
        assertEquals(-1.5f, BFloat(-1.5f).float)
        assertEquals(.3f, BFloat(.3f).float, .001f)
        assertEquals(10f, BFloat(10f).toFloat())
        assertEquals(100f, BFloat(100f).toFloat())
        assertEquals(1000f, BFloat(1000f).toFloat())
    }
}
