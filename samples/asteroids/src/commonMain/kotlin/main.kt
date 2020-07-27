import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.klock.hr.*
import com.soywiz.korev.*
import com.soywiz.korge.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.random.*
import kotlin.random.*

val WIDTH = 640
val HEIGHT = 480

val SHIP_SIZE = 24
val BULLET_SIZE = 14
val Stage.assets by Extra.PropertyThis<Stage, Assets> { Assets(views, SHIP_SIZE) }

suspend fun main() = Korge(
	width = WIDTH, height = HEIGHT,
	virtualWidth = WIDTH, virtualHeight = HEIGHT,
	bgcolor = Colors["#222"],
	clipBorders = false
) {
	views.gameWindow.icon = assets.shipBitmap

	val ship = image(assets.shipBitmap).center().position(320, 240)

	fun pressing(key: Key) = views.input.keys[key]

	spawnAsteroid(WIDTH - 20.0, 20.0)

	ship.onCollision {
		if (it is Asteroid) {
			//ship.removeFromParent()
			println("GAME OVER!")
		}
	}

	val random = Random(0)
	for (n in 0 until 15) {
		val asteroid = spawnAsteroid(0.0, 0.0)
		do {
			asteroid.x = random[0.0, WIDTH.toDouble()]
			asteroid.y = random[0.0, HEIGHT.toDouble()]
			asteroid.angle = random[0.0, 360.0].degrees
		} while (asteroid.collidesWith(ship) || ship.distanceTo(asteroid) < 100.0)
	}

	var bulletReload = 0.0
	addHrUpdater { time ->
		val scale = time / 16.hrMilliseconds
		if (pressing(Key.LEFT)) ship.rotation -= 3.degrees * scale
		if (pressing(Key.RIGHT)) ship.rotation += 3.degrees * scale
		if (pressing(Key.UP)) ship.advance(2.0 * scale)
		if (pressing(Key.DOWN)) ship.advance(-1.5 * scale)

		if (bulletReload > 0) bulletReload -= 1 * scale

		if (bulletReload <= 0 && pressing(Key.SPACE)) {
			bulletReload = 20.0
			val bullet = image(assets.bulletBitmap)
				.center()
				.position(ship.x, ship.y)
				.rotation(ship.rotation)
				.advance(assets.shipSize * 0.75)

			bullet.onCollision {
				if (it is Asteroid) {
					bullet.removeFromParent()
					it.divide()
				}
			}

			fun bulletFrame(time: TimeSpan) {
				val scale = time / 16.milliseconds
				bullet.advance(+3.0 * scale)
				if (bullet.x < -BULLET_SIZE || bullet.y < -BULLET_SIZE || bullet.x > WIDTH + BULLET_SIZE || bullet.y > HEIGHT + BULLET_SIZE) {
					bullet.removeFromParent()
				}
			}

			//launch {
			//	while (true) {
			//		bulletFrame()
			//		bullet.delayFrame()
			//	}
			//}
			bullet.addUpdater { bulletFrame(it) }
		}
	}

	//image(shipBitmap)
}

class Asteroid(val assets: Assets, val asteroidSize: Int = 3) : Image(assets.asteroidBitmap) {
	var angle = 30.degrees

	init {
		anchor(.5, .5)
		scale = asteroidSize.toDouble() / 3.0
		name = "asteroid"
		speed = 0.6
		addHrUpdater { time ->
			val scale = time / 16.hrMilliseconds
			val dx = angle.cosine * scale
			val dy = angle.sine * scale
			x += dx
			y += dy
			rotationDegrees += scale
			if (y < 0 && dy < 0) angle += 45.degrees
			if (x < 0 && dx < 0) angle += 45.degrees
			if (x > WIDTH && dx > 0) angle += 45.degrees
			if (y > HEIGHT && dy > 0) angle += 45.degrees
		}
	}

	fun divide() {
		if (asteroidSize > 1) {
			Asteroid(assets, asteroidSize - 1).xy(x, y).addTo(parent!!).also {
				it.angle = this.angle + 45.degrees
				it.speed = this.speed * 1.5
			}
			Asteroid(assets, asteroidSize - 1).xy(x, y).addTo(parent!!).also {
				it.angle = this.angle - 45.degrees
				it.speed = this.speed * 1.5
			}
		}
		removeFromParent()
	}
}

fun Stage.spawnAsteroid(x: Double, y: Double): Asteroid {
	return Asteroid(assets).addTo(this).xy(x, y)
	//solidRect(10.0, 20.0, Colors.RED).xy(20, 20)
}

fun View.distanceTo(other: View) = Point.distance(x, y, other.x, other.y)

fun View.advance(amount: Double, rot: Angle = (-90).degrees) = this.apply {
	x += (this.rotation + rot).cosine * amount
	y += (this.rotation + rot).sine * amount
}

class Assets(val views: Views, val shipSize: Int = 24) {
	val asteroidSize = shipSize * 2
	val shipBitmap = NativeImage(shipSize, shipSize).context2d {
		lineWidth = 0.05
		lineCap = LineCap.ROUND
		stroke(Colors.WHITE) {
			scale(shipSize)
			moveTo(0.5, 0.0)
			lineTo(1.0, 1.0)
			lineTo(0.5, 0.8)
			lineTo(0.0, 1.0)
			close()
		}
	}
	val bulletBitmap = NativeImage(3, (shipSize * 0.3).toInt()).context2d {
		lineWidth = 1.0
		lineCap = LineCap.ROUND
		stroke(Colors.WHITE) {
			moveTo(width / 2.0, 0.0)
			lineToV(height.toDouble())
		}
	}
	val asteroidBitmap = Bitmap32(asteroidSize, asteroidSize).context2d { // Let's use software vector rendering here, for testing purposes
		lineWidth = 0.05
		lineCap = LineCap.ROUND
		stroke(Colors.WHITE) {
			scale(asteroidSize)
			moveTo(0.0, 0.5)
			lineTo(0.2, 0.0)
			lineTo(0.7, 0.0)
			lineTo(1.0, 0.5)
			lineTo(0.7, 1.0)
			lineTo(0.3, 1.0)
			close()
		}
	}
}
