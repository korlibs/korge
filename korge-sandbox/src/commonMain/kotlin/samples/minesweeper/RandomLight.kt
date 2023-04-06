package samples.minesweeper

import korlibs.image.bitmap.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.math.random.*
import kotlin.random.*

class RandomLight(
	parent: Container,
	light: BmpSlice
) : Process(parent) {
	var w2: Double = stage.widthD / 2
	var h2: Double = stage.heightD / 2
	val random = Random
	var sx: Double = 0.0
	var sy: Double = 0.0
	var inca: Double = 0.0
	var incs: Double = 0.0
	var excx: Double = 0.0
	var excy: Double = 0.0

	init {
		image(light, Anchor.CENTER).apply {
			blendMode = BlendMode.ADD
		}
	}

	override suspend fun main() {
		sx = random[-w2, w2]
		sy = random[-h2, h2]
		inca = random[0.0001, 0.03]
		incs = random[0.5, 2.0]
		excx = random[0.7, 1.3]
		excy = random[0.7, 1.3]
		alphaF = 0.1f

		while (true) {
			rotation += inca.degrees
			xD = w2 - cosd(rotation) * w2 * excx + sx
			yD = h2 - sind(rotation) * h2 * excy + sy
			scaleD = 1 + (cosd(rotation) / 6) * incs

            //println("FRAME! $x -> $x2")

            // Check if a light sphere collided with another one
			// The default collision system is inner circle
			if (this.collision<RandomLight>() != null) {
				alphaF = (alphaF + 0.01).coerceIn(0.1, 0.8).toFloat()
			} else {
				alphaF = (alphaF - 0.05).coerceIn(0.1, 0.8).toFloat()
			}

			frame()
		}
	}
}
