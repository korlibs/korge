package korlibs.image.tiles

import korlibs.math.*
import korlibs.math.geom.slice.*
import kotlin.test.*

class TileSliceOrientationTest {
    @Test
    fun testTileFlipXYRot() {
        assertEquals(
            listOf(0, 1, 1, 0,   1, 1, 0, 0),
            (0 until 8).map { SliceOrientation(it).tileFlipX.toInt() }
        )
        assertEquals(
            listOf(0, 0, 1, 1,   0, 1, 1, 0),
            (0 until 8).map { SliceOrientation(it).tileFlipY.toInt() }
        )
        assertEquals(
            listOf(0, 1, 0, 1,   0, 1, 0, 1),
            (0 until 8).map { SliceOrientation(it).tileRot.toInt() }
        )
    }

    @Test
    fun testTileOrientation() {
        assertEquals(
            (0 until 8).map { SliceOrientation(it) },
            (0 until 8).map { Tile(0, SliceOrientation(it)).orientation }
        )
    }
}
