package samples

import com.soywiz.kds.*
import com.soywiz.korge.input.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.camera.*
import com.soywiz.korge.view.tiles.*
import com.soywiz.korim.atlas.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.tiles.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.MPoint.Companion.Zero
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
        val cameraContainer = cameraContainer(width, height) {
            tilemap = tileMap(donutMap, tileSet).centerOn(this)
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
            var mouse: MPoint = MPoint()
            var camAngle: Angle = 0.degrees
            var camPos: MPoint = MPoint()
        }

        addUpdater {
            val mouseButtons = input.mouseButtons
            val isDown = (mouseButtons and 5) != 0
            if (isDown) {
                if (!wasDown) {
                    wasDown = true
                    downVals.mouse.copyFrom(input.mouse)
                    downVals.camAngle = cameraContainer.cameraAngle
                    downVals.camPos = MPoint(cameraContainer.cameraX, cameraContainer.cameraY)
                } else {
                    val rightMouse = (mouseButtons and 4) != 0
                    if (rightMouse) {
                        val downAngle = Zero.angleTo(downVals.mouse)
                        val mouseAngle = Zero.angleTo(input.mouse)
                        val newAngle = downVals.camAngle - (downAngle - mouseAngle)
                        val dy = downVals.mouse.y - input.mouse.y
                        cameraContainer.cameraAngle = newAngle //downVals.camAngle - dy.degrees
                    } else { // leftMouse
                        val newCamPos =
                            cameraContainer.content.globalToLocal(downVals.mouse) - cameraContainer.content.globalToLocal(input.mouse) + downVals.camPos
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
    ): IntArray2 {
        val rand = Random(3)
        val mapValues2 = IntArray2(mapWidth, mapWidth, 0)
        val center = MPoint(mapWidth / 2, mapWidth / 2)
        for (x in 0 until mapWidth) for (y in 0 until mapWidth) {
            val p = MPoint(x, y)
            val dist = (p - center).length
            val onDisc = dist < mapWidth / 2
            val tooClose = dist < (mapWidth / 2) * 0.7
            mapValues2[x, y] =
                if (onDisc && !tooClose) 1 + rand.nextInt(tileSet.texturesMap.size - 1) else 0
        }
        return mapValues2
    }

    private fun makeSimpleTileSet(tileWidth: Int): TileSet {
        val atlas = MutableAtlasUnit()
        val tileBitmaps =
            listOf(Colors.TRANSPARENT, Colors.GREEN, Colors.ORANGE, Colors.GREENYELLOW, Colors.YELLOW).map { c ->
                atlas.add(Bitmap32(tileWidth, tileWidth).also {
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
