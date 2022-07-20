package samples

import com.soywiz.klock.milliseconds
import com.soywiz.kmem.clamp
import com.soywiz.korev.Key
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tiled.tiledMapView
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.camera
import com.soywiz.korge.view.fixedSizeContainer
import com.soywiz.korge.view.position
import com.soywiz.korim.tiles.tiled.readTiledMap
import com.soywiz.korio.file.std.resourcesVfs
import kotlin.math.pow

class MainTilemapWithScroll : Scene() {
    override suspend fun SContainer.sceneMain() {
        val tiledMap = resourcesVfs["gfx/sample.tmx"].readTiledMap()
        fixedSizeContainer(512, 256, clip = true) {
            position(128, 128)
            val camera = camera {
                tiledMapView(tiledMap) {
                }
            }
            var dx = 0.0
            var dy = 0.0
            //this.keys.apply {
            //	down { key ->
            //		when (key) {
            //			Key.RIGHT -> dx -= 1.0
            //			Key.LEFT -> dx += 1.0
            //			Key.DOWN -> dy -= 1.0
            //			Key.UP -> dy += 1.0
            //		}
            //	}
            //}
            addUpdater {
                //val scale = 1.0 / (it / 16.666666.hrMilliseconds)
                val scale = if (it == 0.0.milliseconds) 0.0 else (it / 16.666666.milliseconds)
                if (views.input.keys[Key.RIGHT]) dx -= 1.0
                if (views.input.keys[Key.LEFT]) dx += 1.0
                if (views.input.keys[Key.UP]) dy += 1.0
                if (views.input.keys[Key.DOWN]) dy -= 1.0
                dx = dx.clamp(-10.0, +10.0)
                dy = dy.clamp(-10.0, +10.0)
                camera.x += dx * scale
                camera.y += dy * scale
                dx *= 0.9.pow(scale)
                dy *= 0.9.pow(scale)
            }
        }
    }
}
