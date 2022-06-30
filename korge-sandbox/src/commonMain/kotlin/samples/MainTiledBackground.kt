package samples

import com.soywiz.korge.Korge
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.time.delayFrame
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.tiles.BaseTileMap
import com.soywiz.korge.view.tiles.TileSet
import com.soywiz.korge.view.tiles.tileMap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.slice
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.std.resourcesVfs

class MainTiledBackground : Scene() {
    override suspend fun SContainer.sceneMain() {
        val tileset = TileSet(mapOf(0 to bitmap("korge.png").toBMP32().scaleLinear(0.5, 0.5).slice()))
        val tilemap = tileMap(Bitmap32(1, 1), repeatX = BaseTileMap.Repeat.REPEAT, repeatY = BaseTileMap.Repeat.REPEAT, tileset = tileset)
        launchImmediately {
            while (true) {
                tilemap.x += 1
                tilemap.y += 0.25
                delayFrame()
            }
        }
    }

    suspend fun bitmap(path: String) = resourcesVfs[path].readBitmap()
}
