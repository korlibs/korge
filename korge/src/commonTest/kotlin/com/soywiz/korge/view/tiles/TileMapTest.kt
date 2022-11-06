package com.soywiz.korge.view.tiles

import com.soywiz.kds.*
import com.soywiz.korag.log.LogAG
import com.soywiz.korag.log.LogBaseAG
import com.soywiz.korge.view.scale
import com.soywiz.korge.view.viewsLog
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.collider.*
import com.soywiz.korma.geom.shape.*
import kotlin.test.*

class TileMapTest {
    @Test
    fun test() {
        viewsLog {
            val log = it.ag as LogAG
            log.logFilter = { str, kind -> kind != LogBaseAG.Kind.DRAW_DETAILS && kind != LogBaseAG.Kind.SHADER }
            views.stage.tileMap(IntArray2(16, 16) { 0 }, TileSet.fromBitmapSlices(32, 32, listOf(Bitmap32(32, 32).slice()))).scale(0.1)
            it.views.render()
        }
    }

    @Test
    fun testHitTest() {
        val tileSet = TileSet(intMapOf(
            0 to TileSetTileInfo(0, Bitmap32(16, 16, premultiplied = true).slice()),
            1 to TileSetTileInfo(0, Bitmap32(16, 16, premultiplied = true).slice()),
            2 to TileSetTileInfo(0, Bitmap32(16, 16, premultiplied = true).slice()),
            3 to TileSetTileInfo(0, Bitmap32(16, 16, premultiplied = true).slice()),
        ), collisionsMap = intMapOf(
            0 to com.soywiz.korim.tiles.TileShapeInfoImpl(HitTestDirectionFlags.NONE, Shape2d.Rectangle(0, 0, 16, 16), Matrix()),
            1 to com.soywiz.korim.tiles.TileShapeInfoImpl(HitTestDirectionFlags.NONE, Shape2d.Empty, Matrix()),
            2 to com.soywiz.korim.tiles.TileShapeInfoImpl(HitTestDirectionFlags.ALL, Shape2d.Empty, Matrix()),
            3 to com.soywiz.korim.tiles.TileShapeInfoImpl(HitTestDirectionFlags.ALL, Shape2d.Rectangle(0, 0, 16, 16), Matrix()),
        )
        )
        val map = TileMap(StackedIntArray2(IntArray2(2, 2, intArrayOf(0, 1, 2, 3))), tileSet)
        assertEquals(false, map.pixelHitTest(5, 5, HitTestDirection.DOWN))
        assertEquals(false, map.pixelHitTest(16 + 5, 5, HitTestDirection.DOWN))
        assertEquals(false, map.pixelHitTest(5, 16 + 5, HitTestDirection.DOWN))
        assertEquals(true, map.pixelHitTest(16 + 5, 16 + 5, HitTestDirection.DOWN))
    }
}
