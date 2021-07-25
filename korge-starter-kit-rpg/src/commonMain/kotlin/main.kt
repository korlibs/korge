import com.soywiz.klock.*
import com.soywiz.korev.*
import com.soywiz.korge.*
import com.soywiz.korge.tiled.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.animation.*
import com.soywiz.korge.view.camera.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korge.view.tween.*
import com.soywiz.korim.atlas.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*

suspend fun main() = Korge(width = 512, height = 512, bgcolor = Colors["#2b2b2b"]) {
	val atlas = MutableAtlasUnit(2048, 2048)

	val tilemap = resourcesVfs["BasicTilemap/untitled.tmx"].readTiledMap(atlas = atlas)
	val characters = resourcesVfs["vampire.ase"].readImageDataContainer(ASE, atlas = atlas)

	container {
		scale(2.0)

		lateinit var character: ImageDataView
		lateinit var tiledMapView: TiledMapView

		val cameraContainer = cameraContainer(
			300.0, 300.0, clip = true,
			block = {
			}
		) {
			tiledMapView = tiledMapView(tilemap)
			character = imageDataView(characters["vampire"], "right") {
				xy(50.0, 50.0)
				stop()
			}
		}

		controlWithKeyboard(character, tiledMapView)

		cameraContainer.follow(character)

		//cameraContainer.tweenCamera(cameraContainer.getCameraRect(Rectangle(200, 200, 100, 100)))
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
