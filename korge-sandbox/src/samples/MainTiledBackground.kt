package samples

import korlibs.datastructure.*
import korlibs.image.bitmap.*
import korlibs.image.format.*
import korlibs.image.tiles.*
import korlibs.image.tiles.TileMapRepeat
import korlibs.io.file.std.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.korge.view.tiles.*

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
        addFastUpdater {
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
