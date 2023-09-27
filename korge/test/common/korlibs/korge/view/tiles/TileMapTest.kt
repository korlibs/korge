package korlibs.korge.view.tiles

import korlibs.datastructure.*
import korlibs.graphics.log.AGLog
import korlibs.graphics.log.AGBaseLog
import korlibs.korge.view.scale
import korlibs.korge.view.viewsLog
import korlibs.image.bitmap.*
import korlibs.image.tiles.*
import korlibs.math.geom.*
import korlibs.math.geom.collider.*
import korlibs.math.geom.shape.*
import kotlin.test.*

class TileMapTest {
    @Test
    fun test() {
        viewsLog {
            val log = it.ag as AGLog
            log.logFilter = { str, kind -> kind != AGBaseLog.Kind.DRAW_DETAILS && kind != AGBaseLog.Kind.SHADER }
            views.stage.tileMap(IntArray2(16, 16) { 0 }, TileSet.fromBitmapSlices(32, 32, listOf(Bitmap32(32, 32, premultiplied = true).slice()))).scale(0.1)
            it.views.render()
        }
    }

    @Test
    fun testHitTest() {
        val tileSet = TileSet(intMapOf(
            0 to TileSetTileInfo(0, Bitmap32(16, 16, premultiplied = true).slice(), collision = TileShapeInfoImpl(HitTestDirectionFlags.NONE, Rectangle(0, 0, 16, 16), Matrix())),
            1 to TileSetTileInfo(0, Bitmap32(16, 16, premultiplied = true).slice(), collision = TileShapeInfoImpl(HitTestDirectionFlags.NONE, EmptyShape2D, Matrix())),
            2 to TileSetTileInfo(0, Bitmap32(16, 16, premultiplied = true).slice(), collision = TileShapeInfoImpl(HitTestDirectionFlags.ALL, EmptyShape2D, Matrix())),
            3 to TileSetTileInfo(0, Bitmap32(16, 16, premultiplied = true).slice(), collision = TileShapeInfoImpl(HitTestDirectionFlags.ALL, Rectangle(0, 0, 16, 16), Matrix())),
        ))
        val map = TileMap(SparseChunkedStackedIntArray2(StackedIntArray2(IntArray2(2, 2, intArrayOf(0, 1, 2, 3)))), tileSet)
        assertEquals(false, map.pixelHitTest(5, 5, HitTestDirection.DOWN))
        assertEquals(false, map.pixelHitTest(16 + 5, 5, HitTestDirection.DOWN))
        assertEquals(false, map.pixelHitTest(5, 16 + 5, HitTestDirection.DOWN))
        assertEquals(true, map.pixelHitTest(16 + 5, 16 + 5, HitTestDirection.DOWN))
    }
}
