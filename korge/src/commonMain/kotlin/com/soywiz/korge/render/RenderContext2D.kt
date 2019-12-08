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
			colorMul = multiplyColor,
			blendFactors = blendFactors
		)
	}

    @PublishedApi
    internal val scissorPool = Pool(8) { AG.Scissor(0, 0, 0, 0) }

	inline fun scissor(scissor: AG.Scissor?, block: () -> Unit) {
		val oldScissor = batch.scissor
        var returnScissor: AG.Scissor? = null

		batch.flush()
		if (scissor != null) {
			val left = m.transformX(scissor.left.toDouble(), scissor.top.toDouble()).toInt()
			val top = m.transformY(scissor.left.toDouble(), scissor.top.toDouble()).toInt()
			val right = m.transformX(scissor.right.toDouble(), scissor.bottom.toDouble()).toInt()
			val bottom = m.transformY(scissor.right.toDouble(), scissor.bottom.toDouble()).toInt()

            returnScissor = scissorPool.alloc().setTo(left, top, right - left, bottom - top)

			batch.scissor = returnScissor
			//println("batch.scissor: ${batch.scissor}")
		} else {
			batch.scissor = null
		}
		try {
			block()
		} finally {

            if (returnScissor != null) {
                scissorPool.free(returnScissor)
            }

			batch.flush()
			batch.scissor = oldScissor
		}
	}
}

@PublishedApi
internal fun AG.Scissor.copyFrom(that: AG.Scissor): AG.Scissor = this.apply {
    this.x = that.x
    this.y = that.y
    this.width = that.width
    this.height = that.height
}

@PublishedApi
internal fun AG.Scissor.setTo(x: Int, y: Int, width: Int, height: Int): AG.Scissor = this.apply {
    this.x = x
    this.y = y
    this.width = width
    this.height = height
}
