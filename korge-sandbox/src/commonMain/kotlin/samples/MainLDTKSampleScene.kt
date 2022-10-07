package samples

import com.soywiz.kds.IntArray2
import com.soywiz.kds.StackedIntArray2
import com.soywiz.klock.measureTime
import com.soywiz.klock.seconds
import com.soywiz.kmem.hasBitSet
import com.soywiz.korge.input.onMagnify
import com.soywiz.korge.input.onRotate
import com.soywiz.korge.scene.PixelatedScene
import com.soywiz.korge.tiled.ldtk.LDTKJson
import com.soywiz.korge.tiled.ldtk.TilesetDefinition
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.alpha
import com.soywiz.korge.view.container
import com.soywiz.korge.view.filter.IdentityFilter
import com.soywiz.korge.view.filter.filters
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.tiles.TileInfo
import com.soywiz.korge.view.tiles.tileMap
import com.soywiz.korge.view.xy
import com.soywiz.korim.bitmap.slice
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.tiles.TileSet
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.ScaleMode

class MainLDTKSampleScene : PixelatedScene(1280, 720, sceneScaleMode = ScaleMode.NO_SCALE, sceneSmoothing = true) {
    override suspend fun SContainer.sceneMain() {
        onMagnify { println("magnify: ${it.amount}") }
        onRotate { println("rotate: ${it.amount}") }
        //onSwipe { println("swipe: ${it.amount}") }

        val json = resourcesVfs["ldtk/Typical_2D_platformer_example.ldtk"].readString()
        val ldtk = measureTime({
            LDTKJson.load(json)
        }) {
            println("Load LDTK [${json.length}] in $it")
        }
        class ExtTileset(val def: TilesetDefinition, val tileset: TileSet?)
        val layersDefsById = ldtk.defs.layers.associateBy { it.uid }
        val tilesetDefsById = ldtk.defs.tilesets.associate { def ->
            val bitmap = def.relPath?.let { resourcesVfs["ldtk/$it"].readBitmap() }
            val tileSet = bitmap?.let { TileSet(bitmap.slice(), def.tileGridSize, def.tileGridSize) }
            def.uid to ExtTileset(def, tileSet)
        }

        val container = container {
            for (level in ldtk.levels) {
                container {
                    val color = Colors[level.levelBgColor ?: ldtk.bgColor]
                    solidRect(level.pxWid, level.pxHei, color)
                    for (layer in (level.layerInstances ?: emptyList()).asReversed()) {
                        //for (layer in (level.layerInstances ?: emptyList())) {
                        val layerDef = layersDefsById[layer.layerDefUid] ?: continue
                        val tilesetExt = tilesetDefsById[layer.tilesetDefUid] ?: continue
                        val intGrid = IntArray2(layer.cWid, layer.cHei, layer.intGridCSV.copyOf(layer.cWid * layer.cHei))
                        val tileData = StackedIntArray2(layer.cWid, layer.cHei, -1)
                        val tileset = tilesetExt.def
                        val gridSize = tileset.tileGridSize

                        //val fsprites = FSprites(layer.autoLayerTiles.size)
                        //val view = fsprites.createView(bitmap).also { it.scale(2) }
                        //addChild(view)
                        for (tile in layer.autoLayerTiles) {
                            val (px, py) = tile.px
                            val (tileX, tileY) = tile.src
                            val x = px / gridSize
                            val y = py / gridSize
                            val dx = px % gridSize
                            val dy = py % gridSize
                            val tx = tileX / gridSize
                            val ty = tileY / gridSize
                            val cellsTilesPerRow = tileset.pxWid / gridSize
                            val tileId = ty * cellsTilesPerRow + tx
                            val flipX = tile.f.hasBitSet(0)
                            val flipY = tile.f.hasBitSet(1)
                            tileData.push(x, y, TileInfo(tileId, flipX = flipX, flipY = flipY, offsetX = dx, offsetY = dy).data)
                        }
                        if (tilesetExt.tileset != null) {
                            tileMap(tileData, tilesetExt.tileset).alpha(layerDef.displayOpacity)
                        }
                        //tileset!!.
                        //println(intGrid)
                    }
                }.xy(level.worldX, level.worldY)
                //break // ONLY FIRST LEVEL
                //}.filters(IdentityFilter.Nearest).scale(2)
            }
        //}.xy(300, 300)
        }.filters(IdentityFilter.Linear).xy(300, 300)
        while (true) {
            tween(container::scale[0.5], time = 1.seconds)
            tween(container::scale[1.25], time = 1.seconds)
        }
    }
}
