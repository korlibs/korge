package samples.rpg

import com.soywiz.klock.*
import com.soywiz.korev.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.tiled.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.animation.*
import com.soywiz.korge.view.camera.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.atlas.*
import com.soywiz.korim.format.*
import com.soywiz.korim.tiles.tiled.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*

class MainRpgScene : ScaledScene(512, 512) {
    override suspend fun SContainer.sceneMain() {
        val rootSceneContainer = sceneContainer()

        rootSceneContainer.changeTo({ RpgIngameScene() },
            transition = MaskTransition(transition = TransitionFilter.Transition.CIRCULAR, reversed = false, filtering = true),
            //transition = AlphaTransition,
            time = 0.5.seconds
        )
    }
}

class RpgIngameScene : Scene() {
    val atlas = MutableAtlasUnit(2048, 2048)
    lateinit var tilemap: TiledMap
    lateinit var characters: ImageDataContainer

    override suspend fun SContainer.sceneInit() {

        val sw = Stopwatch().start()

		println("start resources loading...")

        tilemap = resourcesVfs["BasicTilemap/untitled.tmx"].readTiledMap(atlas = atlas)
        characters = resourcesVfs["vampire.ase"].readImageDataContainer(ASE.toProps(), atlas = atlas)

        println("loaded resources in ${sw.elapsed}")
    }

    override suspend fun SContainer.sceneMain() {
        container {
            scale(2.0)

            lateinit var character: ImageDataView
            lateinit var tiledMapView: TiledMapView

            val cameraContainer = cameraContainer(
                256.0, 256.0, clip = true,
                block = {
                    clampToBounds = true
                }
            ) {
                tiledMapView = tiledMapView(tilemap, smoothing = false, showShapes = false)
				tiledMapView.filter = IdentityFilter(false)

                println("tiledMapView[\"start\"]=${tiledMapView["start"].firstOrNull}")
                val npcs = tiledMapView.tiledMap.data.getObjectByType("npc")
                for (obj in tiledMapView.tiledMap.data.objectLayers.objects) {
                    println("- obj = $obj")
                }
                println("NPCS=$npcs")
                println(tiledMapView.firstDescendantWith { it.getPropString("type") == "start" })
                val startPos = tiledMapView["start"].firstOrNull?.pos ?: Point(50, 50)
                val charactersLayer = tiledMapView["characters"].first as Container

                println("charactersLayer=$charactersLayer")

                // Keep zIndex in sync with y
                charactersLayer.addUpdater {
                    charactersLayer.forEachChild {
                        it.zIndex = it.y
                    }
                }

                for (obj in tiledMapView.tiledMap.data.getObjectByType("npc")) {
                    val npc = charactersLayer.imageDataView(
                        characters[obj.str("skin")],
                        "down",
                        playing = false,
                        smoothing = false
                    ) {
                        xy(obj.x, obj.y)
                    }
                }
                character =
                    charactersLayer.imageDataView(characters["vampire"], "right", playing = false, smoothing = false) {
                        xy(startPos)
                    }
            }

            cameraContainer.cameraViewportBounds.copyFrom(tiledMapView.getLocalBoundsOptimized())

            stage!!.controlWithKeyboard(character, tiledMapView)

            cameraContainer.follow(character, setImmediately = true)

            //cameraContainer.tweenCamera(cameraContainer.getCameraRect(Rectangle(200, 200, 100, 100)))
        }
    }
}

fun Stage.controlWithKeyboard(
	char: ImageDataView,
	collider: HitTestable,
	up: Key = Key.UP,
	right: Key = Key.RIGHT,
	down: Key = Key.DOWN,
	left: Key = Key.LEFT,
) {
	addUpdater { dt ->
		val speed = 2.0 * (dt / 16.0.milliseconds)
		var dx = 0.0
		var dy = 0.0
		val pressingLeft = keys[left]
		val pressingRight = keys[right]
		val pressingUp = keys[up]
		val pressingDown = keys[down]
		if (pressingLeft) dx = -1.0
		if (pressingRight) dx = +1.0
		if (pressingUp) dy = -1.0
		if (pressingDown) dy = +1.0
		if (dx != 0.0 || dy != 0.0) {
			val dpos = Point(dx, dy).normalized * speed
			char.moveWithHitTestable(collider, dpos.x, dpos.y)
		}
		char.animation = when {
			pressingLeft -> "left"
			pressingRight -> "right"
			pressingUp -> "up"
			pressingDown -> "down"
			else -> char.animation
		}
		if (pressingLeft || pressingRight || pressingUp || pressingDown) {
			char.play()
		} else {
			char.stop()
			char.rewind()
		}
	}
}
