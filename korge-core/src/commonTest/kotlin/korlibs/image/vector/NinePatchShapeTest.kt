package korlibs.image.vector

import korlibs.image.color.Colors
import korlibs.image.util.NinePatchSlices
import korlibs.image.util.NinePatchSlices2D
import korlibs.image.vector.format.readSVG
import korlibs.io.async.suspendTest
import korlibs.io.file.std.resourcesVfs
import korlibs.math.geom.*
import korlibs.math.geom.range.until
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NinePatchShapeTest {
    @Test
    fun test() = suspendTest {
        val shape = resourcesVfs["chat-bubble.svg"].readSVG().toShape()
        val ninePatch = shape.toNinePatchFromGuides(guideColor = Colors.FUCHSIA)
        assertEquals(Size(128, 128), ninePatch.size)
        assertEquals(NinePatchSlices2D(
            NinePatchSlices(30f until 33f, 80f until 100f),
            NinePatchSlices(40f until 80f)
        ), ninePatch.slices)
        assertTrue { ninePatch.shape is FillShape }
        assertEquals(
            "M0,0 M128,28 C128,12,116,0,100,0 L28,0 C12,0,0,12,0,28 L0,83 C0,98,12,111,28,111 L38,111 L34,128 L76,111 L100,111 C116,111,128,98,128,83 L128,28 Z",
            ninePatch.shape.getPath().roundDecimalPlaces(0).toSvgString()
        )
    }
}
