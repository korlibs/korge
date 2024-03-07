package korlibs.image.tiles

import korlibs.math.geom.slice.*
import kotlin.test.*

class TileMapDataTest {
    @Test
    fun testTileMapInfo() {
        val map = TileMapData(2, 2)
        assertEquals(0, map.contentVersion)
        map[0, 0] = Tile(10, SliceOrientation.ROTATE_90)
        assertEquals(1, map.contentVersion)
        map[1, 0] = Tile(11, SliceOrientation.MIRROR_HORIZONTAL_ROTATE_180)
        map[0, 1] = Tile(12, SliceOrientation.ROTATE_180, -777, 12345)
        map[1, 1] = Tile(13, SliceOrientation.NORMAL, 32133, -12345)
        assertEquals(4, map.contentVersion)

        assertEquals("Tile(10, ROTATE_90, 0, 0)", map[0, 0].toOrientationString())
        assertEquals("Tile(11, MIRROR_HORIZONTAL_ROTATE_180, 0, 0)", map[1, 0].toOrientationString())
        assertEquals("Tile(12, ROTATE_180, -777, 12345)", map[0, 1].toOrientationString())
        assertEquals("Tile(13, ROTATE_0, 32133, -12345)", map[1, 1].toOrientationString())
    }
}
