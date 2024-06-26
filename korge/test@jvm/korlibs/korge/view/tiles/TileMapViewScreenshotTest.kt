package korlibs.korge.view.tiles

import korlibs.datastructure.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.tiles.*
import korlibs.korge.testing.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.math.geom.slice.*
import korlibs.memory.*
import kotlin.test.*

class TileMapViewScreenshotTest {
    @Test
    fun testTilemapScreenshotTest() = korgeScreenshotTest(
        windowSize = Size(512, 512),
        bgcolor = Colors.RED
    ) {

        fun <T : Int64> int64ArrayOf(vararg values: T): Int64Array = Int64Array(values.size) { values[it] }
        fun int64ArrayOf(vararg values: Int): Int64Array = Int64Array(values.size) { values[it].toInt64() }

        val tileset = TileSet(
            TileSetTileInfo(0, Bitmap32(133, 173, Colors.RED.premultiplied).slice()),
            TileSetTileInfo(1, Bitmap32(133, 173, Colors.GREEN.premultiplied).slice()),
        )

        val tilemap = tileMap(TileMapData(StackedInt64Array2(Int64Array2(2, 2, int64ArrayOf(0, 1, 1, 0))), repeatX = TileMapRepeat.REPEAT, repeatY = TileMapRepeat.REPEAT, tileSet = tileset))
        tilemap.xy(3000, 1500)

        assertScreenshot(this, "offsetInfiniteTilemap", includeBackground = false)

        tilemap.xy(-3011, -1513)

        assertScreenshot(this, "offsetInfiniteTilemap2", includeBackground = false)
    }

    @Test
    fun testTilemapOffsetAndOrientation() = korgeScreenshotTest(
        windowSize = Size(400, 272),
        bgcolor = Colors.RED
    ) {

        val tileset = TileSet.fromBitmapSlices(16, 16, listOf(
            Bitmap32(16, 16, Colors.TRANSPARENT).premultipliedIfRequired().slice(),
            //Bitmap32(16, 16, Colors.RED).slice(),
            Bitmap32(16, 16) { x, y ->
                when {
                    x < 8 -> if (y < 8) Colors.RED else Colors.BLUE
                    else -> if (y < 8) Colors.WHITE else Colors.GREEN
                }
            }.premultipliedIfRequired().slice(),
            Bitmap32(16, 16) { x, y ->
                RGBA(64 + x * 8, 64 + y * 8, 255, 255)
            }.premultipliedIfRequired().slice(),
            Bitmap32(16, 16) { x, y ->
                RGBA(255, 64 + x * 8, 64 + y * 8, 255)
            }.premultipliedIfRequired().slice(),
            Bitmap32(16, 16) { x, y ->
                RGBA(64 + x * 8, 255, 64 + y * 8, 255)
            }.premultipliedIfRequired().slice(),
        ))

        val map = TileMapData(8, 16, tileset, Tile.INVALID)
        for (n in 0 until 8) {
            map[n, 0] = if (n < 4) Tile.ZERO else Tile.INVALID
            map[n, 1] = Tile(1, SliceOrientation.VALUES[n])
            map[n, 2] = Tile(n % 5, SliceOrientation.VALUES[n], offsetX = -4)
            //if (n >= 0) break
            map[n, 3] = Tile(1, SliceOrientation.VALUES[n], offsetX = -4)
            map[n, 5] = Tile(1, SliceOrientation.VALUES[n], offsetX = +4)
            map[n, 7] = Tile(1, SliceOrientation.VALUES[n], offsetY = -4)
            map[n, 9] = Tile(1, SliceOrientation.VALUES[n], offsetY = +4)
            map[n, 11] = Tile(1, SliceOrientation.VALUES[n], offsetX = n - 4)
            map[n, 13] = Tile(1, SliceOrientation.VALUES[n], offsetX = n - 4, offsetY = n - 4)
        }

        tileMap(map)
        image(map.render()).xy(128 + 16, 0)
        image(map.tileSet.base).xy(300, 50)

        assertScreenshot(this, "tilemapOffsetAndOrientation", includeBackground = false)

    }
}
