package samples

import korlibs.datastructure.*
import korlibs.image.atlas.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.tiles.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.korge.view.align.*
import korlibs.korge.view.camera.*
import korlibs.korge.view.tiles.*
import korlibs.math.geom.*
import korlibs.memory.*
import korlibs.number.*
import kotlin.math.*
import kotlin.random.*

class MainTilemapTest : Scene() {
    override suspend fun SContainer.sceneMain() {
        val tileSize = 200
        val tileSet = makeSimpleTileSet(tileSize)
        //val mapSize = 200
        val mapSize = 100
        //val mapSize = 16
        val donutMap = makeDonutMap(mapSize, tileSet)
        lateinit var tilemap: TileMap

        //filters(IdentityFilter)
        solidRect(width, height, Colors["#3e0000"])
        val cameraContainer = cameraContainer(Size(width, height)) {
            tilemap = tileMap(donutMap.copy(tileSet = tileSet)).centerOn(this)
        }//.filters(BlurFilter())
        cameraContainer.cameraY = -80.0 * mapSize
        val statusOverlay = text("")

        var zoom = -4.5
        fun updateZoom() {
            cameraContainer.cameraZoom = 2.0.pow(zoom)
        }
        onMagnify {
            //println("onscroll: ${it.scrollDeltaYPixels}")
            zoom += it.amount
            updateZoom()
        }
        onScroll {
            //println("onscroll: ${it.scrollDeltaXPixels}, ${it.scrollDeltaYPixels}")
            //zoom -= (it.scrollDeltaYPixels / 240)
            //updateZoom()

            cameraContainer.cameraX += it.scrollDeltaXPixels * 100 * -zoom
            cameraContainer.cameraY += it.scrollDeltaYPixels * 100 * -zoom
        }
        updateZoom()

        var wasDown = false
        val downVals = object {
            var mouse: Point = Point.ZERO
            var camAngle: Angle = 0.degrees
            var camPos: Point = Point.ZERO
        }

        addFastUpdater {
            val mouseButtons = input.mouseButtons
            val isDown = (mouseButtons and 5) != 0
            if (isDown) {
                if (!wasDown) {
                    wasDown = true
                    downVals.mouse = input.mousePos
                    downVals.camAngle = cameraContainer.cameraAngle
                    downVals.camPos = Point(cameraContainer.cameraX, cameraContainer.cameraY)
                } else {
                    val rightMouse = (mouseButtons and 4) != 0
                    if (rightMouse) {
                        val downAngle = Point.ZERO.angleTo(downVals.mouse)
                        val mouseAngle = Point.ZERO.angleTo(input.mousePos)
                        val newAngle = downVals.camAngle - (downAngle - mouseAngle)
                        val dy = downVals.mouse.y - input.mousePos.y
                        cameraContainer.cameraAngle = newAngle //downVals.camAngle - dy.degrees
                    } else { // leftMouse
                        val newCamPos =
                            cameraContainer.content.globalToLocal(downVals.mouse) - cameraContainer.content.globalToLocal(input.mousePos) + downVals.camPos
                        cameraContainer.cameraX = newCamPos.x
                        cameraContainer.cameraY = newCamPos.y
                    }
                }
            }
            statusOverlay.text = listOf(
                "zoom: ${zoom.niceStr(2)}",
                "angle: ${cameraContainer.cameraAngle}",
                "pos: ${cameraContainer.cameraX.niceStr(2)}, ${cameraContainer.cameraY.niceStr(2)}\n",
                "ngroups: ${tilemap.totalGroupsCount}",
                "ntiles: ${tilemap.totalTilesRendered}",
                "nvertices: ${tilemap.totalVertexCount}",
                "nindices: ${tilemap.totalIndexCount}",
                "nbatches: ${tilemap.totalBatchCount}",
                "niterations: ${tilemap.totalIterationCount}"
            ).joinToString("\n")
            wasDown = isDown
        }
    }

    private fun makeDonutMap(
        mapWidth: Int,
        tileSet: TileSet
    ): TileMapData {
        val rand = Random(3)
        val mapValues2 = TileMapData(mapWidth, mapWidth, TileSet.EMPTY)
        val center = Point(mapWidth / 2, mapWidth / 2)
        for (x in 0 until mapWidth) for (y in 0 until mapWidth) {
            val p = Point(x, y)
            val dist = (p - center).length
            val onDisc = dist < mapWidth / 2
            val tooClose = dist < (mapWidth / 2) * 0.7
            mapValues2[x, y] =
                Tile(if (onDisc && !tooClose) 1 + rand.nextInt(tileSet.tilesMap.size - 1) else 0)
        }
        return mapValues2
    }

    private fun makeSimpleTileSet(tileWidth: Int): TileSet {
        val atlas = MutableAtlasUnit()
        val tileBitmaps =
            listOf(Colors.TRANSPARENT, Colors.GREEN, Colors.ORANGE, Colors.GREENYELLOW, Colors.YELLOW).map { c ->
                atlas.add(Bitmap32(tileWidth, tileWidth, premultiplied = true).also {
                    it.fill(c)
                    for (i in (tileWidth / 5) until (tileWidth * 4 / 5)) {
                        it[i, tileWidth / 2] = Colors.TRANSPARENT
                        it[tileWidth / 2, i] = Colors.TRANSPARENT
                    }
                }).slice
            }
        return TileSet.fromBitmapSlices(tileWidth, tileWidth, tileBitmaps)
    }
}
