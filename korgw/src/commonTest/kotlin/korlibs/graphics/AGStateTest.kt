package korlibs.graphics

import korlibs.math.geom.*
import kotlin.test.*

class AGStateTest {
    @Test
    fun test() {
        val fb = AGFrameBuffer()
        fb.setSize(0, 0, 200, 600)
        fb.setExtra(hasDepth = false, hasStencil = true)
        fb.setSamples(4)
        assertEquals("AGFrameBufferInfo(width=200, height=600, hasDepth=false, hasStencil=true, samples=4)", fb.info.toString())
    }

    @Test
    fun testBoundsBuilder() {
        var bb = BoundsBuilder()
        bb += AGScissor(10, 20, 110, 120)
        bb += AGScissor(50, 60, 135, 145)
        assertEquals(
            AGScissor(10, 20, 175, 185),
            bb.bounds.toAGScissor()
        )
    }
}
