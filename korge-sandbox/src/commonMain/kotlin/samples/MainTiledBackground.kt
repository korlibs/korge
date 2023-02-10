package samples

import com.soywiz.kds.*
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.time.delayFrame
import com.soywiz.korge.view.*
import com.soywiz.korge.view.tiles.TileMapRepeat
import com.soywiz.korge.view.tiles.tileMap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.slice
import com.soywiz.korim.color.*
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.tiles.TileSet
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.*

class MainTiledBackground : Scene() {
    override suspend fun SContainer.sceneMain() {
        val tileset = TileSet(mapOf(
            0 to bitmap("korge.png").toBMP32().scaleLinear(0.5, 0.5).slice(),
            1 to bitmap("korim.png").toBMP32().scaleLinear(0.5, 0.5).slice(),
            //1 to Bitmap32(256, 256, Colors.MEDIUMAQUAMARINE).premultipliedIfRequired().slice()
        ),
        )
        val tilemap = tileMap(IntArray2(2, 2, intArrayOf(0, 1, 1, 0)), repeatX = TileMapRepeat.REPEAT, repeatY = TileMapRepeat.REPEAT, tileset = tileset)
        tilemap.x += 300
        tilemap.y += 300
        addUpdater {
            //tilemap.rotation += 1.degrees
            tilemap.x += 1
            tilemap.y += 0.25
        }
        //launchImmediately {
        //    while (true) {
        //        tilemap.x += 1
        //        tilemap.y += 0.25
        //        delayFrame()
        //    }
        //}
    }

    suspend fun bitmap(path: String) = resourcesVfs[path].readBitmap()
}
