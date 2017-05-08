package com.soywiz.korge.render

import com.soywiz.korag.AG
import com.soywiz.korim.color.Colors
import com.soywiz.korio.util.Extra
import com.soywiz.korio.util.Pool
import com.soywiz.korma.Matrix2d

class RenderContext2D(val batch: BatchBuilder2D) : Extra by Extra.Mixin() {
	val mpool = Pool<Matrix2d> { Matrix2d() }
	val m = Matrix2d()
	var blendFactors = AG.Blending.NORMAL
	var multiplyColor = Colors.WHITE

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

	fun setMatrix(matrix: Matrix2d) {
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

	fun rotate(angle: Double) {
		m.prerotate(angle)
	}

	fun imageScale(texture: Texture, x: Double, y: Double, scale: Double) {
		//println(m)
		batch.drawQuad(texture, x.toFloat(), y.toFloat(), (texture.width * scale).toFloat(), (texture.height * scale).toFloat(), m = m, colorMul = multiplyColor, blendFactors = blendFactors)
	}
}
