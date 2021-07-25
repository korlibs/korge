package com.soywiz.korge.view.tiles

import com.soywiz.korge.tiled.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class TiledMapCollisionTest {
    @Test
    fun test() = suspendTest {
        val tiledMap = resourcesVfs["tilecollision/untitled.tmx"].readTiledMap()
        val tiledMapView = TiledMapView(tiledMap)
        assertTrue(tiledMapView.pixelHitTest(-16, -16) == null, "outside bounds")
        assertTrue(tiledMapView.pixelHitTest(16, 16) == null, "empty tile")
        assertTrue(tiledMapView.pixelHitTest(48, 16) != null, "block tile")
    }
}
