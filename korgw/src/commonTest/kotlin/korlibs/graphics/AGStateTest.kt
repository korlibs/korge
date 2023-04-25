package korlibs.graphics

import korlibs.graphics.shader.*
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

    @Test
    fun testIndexType() {
        assertEquals(AGIndexType.NONE, AGIndexType.fromBytesSize(0))
        assertEquals(AGIndexType.UBYTE, AGIndexType.fromBytesSize(1))
        assertEquals(AGIndexType.USHORT, AGIndexType.fromBytesSize(2))
        assertEquals(AGIndexType.UINT, AGIndexType.fromBytesSize(4))

        assertEquals(AGIndexType.UBYTE, AGIndexType[VarKind.TBOOL])
        assertEquals(AGIndexType.UBYTE, AGIndexType[VarKind.TBYTE])
        assertEquals(AGIndexType.UBYTE, AGIndexType[VarKind.TUNSIGNED_BYTE])
        assertEquals(AGIndexType.USHORT, AGIndexType[VarKind.TSHORT])
        assertEquals(AGIndexType.USHORT, AGIndexType[VarKind.TUNSIGNED_SHORT])
        assertEquals(AGIndexType.UINT, AGIndexType[VarKind.TINT])
        assertEquals(AGIndexType.UINT, AGIndexType[VarKind.TFLOAT])

        assertEquals(0, AGIndexType.NONE.bytesSize)
        assertEquals(1, AGIndexType.UBYTE.bytesSize)
        assertEquals(2, AGIndexType.USHORT.bytesSize)
        assertEquals(4, AGIndexType.UINT.bytesSize)
    }
}
