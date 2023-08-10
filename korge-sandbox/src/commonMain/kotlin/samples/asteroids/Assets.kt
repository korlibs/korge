package samples.asteroids

import korlibs.datastructure.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*

val MainAsteroids.assets by Extra.PropertyThis<MainAsteroids, Assets> { Assets(SHIP_SIZE) }

/**
 * Holds bitmaps used in the game
 */
class Assets(val shipSize: Int = 24) {

    private val asteroidSize = shipSize * 2

    val shipBitmap = NativeImage(shipSize, shipSize).context2d {
		lineWidth = 0.05f
		lineCap = LineCap.ROUND
		stroke(Colors.WHITE) {
            scale(shipSize)
            moveTo(Point(0.5, 0.0))
            lineTo(Point(1.0, 1.0))
            lineTo(Point(0.5, 0.8))
            lineTo(Point(0.0, 1.0))
            closePath()
        }
	}

    val bulletBitmap = NativeImage(3, (shipSize * 0.3).toInt()).context2d {
		lineWidth = 1f
		lineCap = LineCap.ROUND
		stroke(Colors.WHITE) {
			moveTo(Point(width / 2.0, 0.0))
			lineToV(height.toFloat())
		}
	}

    val asteroidBitmap = Bitmap32Context2d(asteroidSize, asteroidSize) { // Let's use software vector rendering here for testing purposes
		lineWidth = 0.05f
		lineCap = LineCap.ROUND
		stroke(Colors.WHITE) {
            scale(asteroidSize)
            moveTo(Point(0.0, 0.5))
            lineTo(Point(0.2, 0.0))
            lineTo(Point(0.7, 0.0))
            lineTo(Point(1.0, 0.5))
            lineTo(Point(0.7, 1.0))
            lineTo(Point(0.3, 1.0))
            closePath()
        }
	}
}
