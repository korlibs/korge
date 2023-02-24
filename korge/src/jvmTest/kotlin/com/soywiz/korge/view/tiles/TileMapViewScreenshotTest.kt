package com.soywiz.korge.view.tiles

import com.soywiz.kds.*
import com.soywiz.korge.testing.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.tiles.*
import kotlin.test.*

class TileMapViewScreenshotTest {
    @Test
    fun testTilemapScreenshotTest() = korgeScreenshotTest(
        width = 512, height = 512,
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
