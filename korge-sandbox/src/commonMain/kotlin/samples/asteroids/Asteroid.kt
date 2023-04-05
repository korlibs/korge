package samples.asteroids

import korlibs.time.milliseconds
import korlibs.korge.view.*
import korlibs.math.geom.*

class Asteroid(
    private val assets: Assets,
    private val asteroidSize: Int = 3
) : BaseImage(assets.asteroidBitmap) {

	var angle = 30.degrees

	init {
		anchor(.5, .5)
		scaleD = asteroidSize.toDouble() / 3.0
		name = "asteroid"
		speed = 0.6f
		addUpdater { time ->
			val scale = time / 16.0.milliseconds
			val dx = angle.cosineD * scale
			val dy = angle.sineD * scale
			xD += dx
			yD += dy
			rotation += scale.degrees
			if (yD < 0 && dy < 0) angle += 45.degrees
			if (xD < 0 && dx < 0) angle += 45.degrees
			if (xD > WIDTH && dx > 0) angle += 45.degrees
			if (yD > HEIGHT && dy > 0) angle += 45.degrees
		}
	}

	fun divide() {
		if (asteroidSize > 1) {
			Asteroid(assets, asteroidSize - 1).xy(xD, yD).addTo(parent!!).also {
				it.angle = this.angle + 45.degrees
				it.speed = this.speed * 1.5f
			}
			Asteroid(assets, asteroidSize - 1).xy(xD, yD).addTo(parent!!).also {
				it.angle = this.angle - 45.degrees
				it.speed = this.speed * 1.5f
			}
		}
		removeFromParent()
	}
}
