package samples.asteroids

import com.soywiz.kds.Extra
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.vector.LineCap
import com.soywiz.korma.geom.vector.lineToV

val MainAsteroids.assets by Extra.PropertyThis<MainAsteroids, Assets> { Assets(SHIP_SIZE) }

/**
 * Holds bitmaps used in the game
 */
class Assets(val shipSize: Int = 24) {

    private val asteroidSize = shipSize * 2

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

    val asteroidBitmap = Bitmap32Context2d(asteroidSize, asteroidSize) { // Let's use software vector rendering here for testing purposes
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
