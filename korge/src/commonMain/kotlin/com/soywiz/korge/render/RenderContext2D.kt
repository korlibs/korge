package com.soywiz.korge.render

import com.soywiz.kds.*
import com.soywiz.klogger.*
import com.soywiz.korag.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*

private val logger = Logger("RenderContext2D")

class RenderContext2D(val batch: BatchBuilder2D) : Extra by Extra.Mixin() {
	init { logger.trace { "RenderContext2D[0]" } }

	val mpool = Pool<Matrix> { Matrix() }

	init { logger.trace { "RenderContext2D[1]" } }

	val m = Matrix()
	var blendFactors = AG.Blending.NORMAL
	var multiplyColor = Colors.WHITE

	init { logger.trace { "RenderContext2D[2]" } }

	inline fun <T> keepMatrix(crossinline callback: () -> T) = mpool.alloc { matrix ->
		matrix.copyFrom(m)
		try {
			callback()
		} finally {
			m.copyFrom(matrix)
		}
	}

	inline fun <T> keepBlendFactors(crossinline callback: () -> T): T {
		val oldBlendFactors = this.blendFactors
		try {
			return callback()
		} finally {
			this.blendFactors = oldBlendFactors
		}
	}

	inline fun <T> keep(crossinline callback: () -> T): T {
		return keepMatrix {
			keepBlendFactors {
				keepColor {
					callback()
				}
			}
		}
	}

	inline fun <T> keepColor(crossinline callback: () -> T): T {
		val multiplyColor = this.multiplyColor
		try {
			return callback()
		} finally {
			this.multiplyColor = multiplyColor
		}
	}

	fun setMatrix(matrix: Matrix) {
		this.m.copyFrom(matrix)
	}

	fun translate(dx: Double, dy: Double) {
		m.pretranslate(dx, dy)
	}

	fun scale(sx: Double, sy: Double = sx) {
		m.prescale(sx, sy)
	}

	fun scale(scale: Double) {
		m.prescale(scale, scale)
	}

	fun rotate(angle: Angle) {
		m.prerotate(angle)
	}

	fun imageScale(texture: Texture, x: Double, y: Double, scale: Double) {
		//println(m)
		batch.drawQuad(
			texture,
			x.toFloat(),
			y.toFloat(),
			(texture.width * scale).toFloat(),
			(texture.height * scale).toFloat(),
			m = m,
			colorMulInt = multiplyColor.rgba,
			blendFactors = blendFactors
		)
	}

	inline fun scissor(scissor: AG.Scissor?, block: () -> Unit) {
		val oldScissor = batch.scissor

		batch.flush()
		if (scissor != null) {
			val left = m.transformX(scissor.left.toDouble(), scissor.top.toDouble()).toInt()
			val top = m.transformY(scissor.left.toDouble(), scissor.top.toDouble()).toInt()
			val right = m.transformX(scissor.right.toDouble(), scissor.bottom.toDouble()).toInt()
			val bottom = m.transformY(scissor.right.toDouble(), scissor.bottom.toDouble()).toInt()

			batch.scissor = AG.Scissor(left, top, right - left, bottom - top)
			//println("batch.scissor: ${batch.scissor}")
		} else {
			batch.scissor = null
		}
		try {
			block()
		} finally {
			batch.flush()
			batch.scissor = oldScissor
		}
	}
}
