package samples

import korlibs.datastructure.*
import korlibs.korge.scene.Scene
import korlibs.korge.time.delayFrame
import korlibs.korge.view.*
import korlibs.korge.view.tiles.TileMapRepeat
import korlibs.korge.view.tiles.tileMap
import korlibs.image.bitmap.Bitmap32
import korlibs.image.bitmap.slice
import korlibs.image.color.*
import korlibs.image.format.readBitmap
import korlibs.image.tiles.*
import korlibs.io.async.launchImmediately
import korlibs.io.file.std.resourcesVfs
import korlibs.math.geom.*

class MainTiledBackground : Scene() {
    override suspend fun SContainer.sceneMain() {

        val tileset = TileSet(
            TileSetTileInfo(0, bitmap("korge.png").toBMP32().scaleLinear(0.5, 0.5).slice()),
            TileSetTileInfo(1, bitmap("korim.png").toBMP32().scaleLinear(0.5, 0.5).slice()),
            //TileSetTileInfo(1, Bitmap32(256, 256, Colors.MEDIUMAQUAMARINE).premultipliedIfRequired().slice())
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
