package samples

import korlibs.image.bitmap.*
import korlibs.image.format.*
import korlibs.image.tiles.*
import korlibs.io.file.std.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.korge.view.tiles.*

class MainTiledBackground : Scene() {
    override suspend fun SContainer.sceneMain() {

        val tileset = TileSet(
            listOf(
                TileSetTileInfo(0, bitmap("korge.png").toBMP32().scaleLinear(0.5, 0.5).slice()),
                TileSetTileInfo(1, bitmap("korim.png").toBMP32().scaleLinear(0.5, 0.5).slice()),
            ),
            //TileSetTileInfo(1, Bitmap32(256, 256, Colors.MEDIUMAQUAMARINE).premultipliedIfRequired().slice())
        )
        println(tileset.get(0))
        println(tileset.get(1))
        val tileMapData = TileMapData(2, 2, tileset, repeatX = TileMapRepeat.REPEAT, repeatY = TileMapRepeat.REPEAT)
        tileMapData[0, 0] = Tile(0)
        tileMapData[1, 0] = Tile(1)
        tileMapData[0, 1] = Tile(1)
        tileMapData[1, 1] = Tile(0)
        val tilemap = tileMap(tileMapData)
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
