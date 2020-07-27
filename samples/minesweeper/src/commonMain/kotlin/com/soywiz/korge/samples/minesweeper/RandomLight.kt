package com.soywiz.korge.samples.minesweeper

import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.random.*
import kotlin.math.*
import kotlin.random.*

class RandomLight(
	parent: Container,
	light: BmpSlice
) : Process(parent) {
	var w2: Double = stage.width / 2
	var h2: Double = stage.height / 2
	val random = Random
	var sx: Double = 0.0
	var sy: Double = 0.0
	var inca: Double = 0.0
	var incs: Double = 0.0
	var excx: Double = 0.0
	var excy: Double = 0.0

	init {
		image(light, 0.5, 0.5).apply {
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
		alpha = 0.1

		while (true) {
			rotationDegrees += inca
			x = w2 - cos(rotationDegrees) * w2 * excx + sx
			y = h2 - sin(rotationDegrees) * h2 * excy + sy
			scale = 1 + (cos(rotationDegrees) / 6) * incs

			// Check if a light sphere collided with another one
			// The default collision system is inner circle
			if (this.collision<RandomLight>() != null) {
				alpha = (alpha + 0.01).coerceIn(0.1, 0.8)
			} else {
				alpha = (alpha - 0.05).coerceIn(0.1, 0.8)
			}

			frame()
		}
	}
}
