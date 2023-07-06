package korlibs.korge.view

import korlibs.image.color.*
import kotlin.test.*

class BlendModeTest {
    @Test
    fun testToString() {
        assertEquals("INHERIT", BlendMode.INHERIT.toString())
        assertEquals("NONE", BlendMode.NONE.toString())
        assertEquals("NORMAL", BlendMode.NORMAL.toString())
        assertEquals("ADD", BlendMode.ADD.toString())
    }

    @Test
    fun testNormal() {
        assertEquals(Colors["#7f0000ff"], BlendMode.NORMAL.apply(Colors["#7f0000ff"], Colors["#ffffffff"]))
        assertEquals(Colors["#ff8080ff"], BlendMode.NORMAL.apply(Colors["#7f00007f"], Colors["#ffffffff"]))

        assertEquals("Blending(outRGB = (srcRGB * 1) + (dstRGB * (1 - srcA)), outA = (srcA * 1) + (dstA * (1 - srcA)))", BlendMode.NORMAL.factors.toString())
    }

    @Test
    fun testAdd() {
        assertEquals(Colors["#cb4c4cff"], BlendMode.ADD.apply(Colors["#4c4c4c4c"], Colors["#7f0000ff"]))
        assertEquals("Blending(outRGB = (srcRGB * 1) + (dstRGB * 1), outA = (srcA * 1) + (dstA * 1))", BlendMode.ADD.factors.toString())
    }

    @Test
    fun testInvert() {
        assertEquals(Colors["#000000ff"], BlendMode.INVERT.apply(Colors["#ffffffff"], Colors["#ffffff00"]))
    }
}
