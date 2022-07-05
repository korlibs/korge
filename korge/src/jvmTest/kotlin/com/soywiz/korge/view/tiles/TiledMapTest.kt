package com.soywiz.korge.view.tiles

import com.soywiz.korim.tiles.tiled.readTiledMap
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.localCurrentDirVfs
import com.soywiz.korio.file.std.resourcesVfs
import kotlin.test.Test

class TiledMapTest {
    val vfs = localCurrentDirVfs["src/commonTest/resources"]
    //val vfs = resourcesVfs
    @Test
    fun test() = suspendTest {
        vfs["tmxbug1/lv1.tmx"].readTiledMap()
    }

    @Test
    fun test2() = suspendTest {
        resourcesVfs["tiled/Abstract_Platformer.tmx"].readTiledMap()
        resourcesVfs["tiled/platformer.tmx"].readTiledMap()
    }
}
