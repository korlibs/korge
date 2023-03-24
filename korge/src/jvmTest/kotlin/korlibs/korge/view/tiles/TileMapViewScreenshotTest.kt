package korlibs.korge.view.tiles

import korlibs.datastructure.*
import korlibs.korge.testing.*
import korlibs.korge.view.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.tiles.*
import korlibs.math.geom.*
import kotlin.test.*

class TileMapViewScreenshotTest {
    @Test
    fun testTilemapScreenshotTest() = korgeScreenshotTest(
        windowSize = SizeInt(512, 512),
        bgcolor = Colors.RED
    ) {

        val tileset = TileSet(
            TileSetTileInfo(0, Bitmap32(133, 173, Colors.RED.premultiplied).slice()),
            TileSetTileInfo(1, Bitmap32(133, 173, Colors.GREEN.premultiplied).slice()),
        )
        val tilemap = tileMap(IntArray2(2, 2, intArrayOf(0, 1, 1, 0)), repeatX = TileMapRepeat.REPEAT, repeatY = TileMapRepeat.REPEAT, tileset = tileset)
        tilemap.xy(3000, 1500)

        assertScreenshot(this, "offsetInfiniteTilemap", includeBackground = false)

        tilemap.xy(-3011, -1513)

        assertScreenshot(this, "offsetInfiniteTilemap2", includeBackground = false)

    }
}
