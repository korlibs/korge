package com.soywiz.korge.render

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.klogger.*
import com.soywiz.korag.*
import com.soywiz.korge.internal.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*

private val logger = Logger("RenderContext2D")

/**
 * Helper class using [BatchBuilder2D] that keeps a chain of affine transforms [Matrix], [ColorTransform] and [blendFactors]
 * and allows to draw images and scissors with that transform.
 *
 * [keepMatrix], [keepBlendFactors], [keepColor] and [keep] block methods allow to do transformations inside its blocks
 * while restoring its initial state at the end of the block.
 *
 * [setMatrix], [translate], [scale], and [rotate] allows to control the transform matrix.
 *
 * [rect] and [imageScale] allows to render color quads and images.
 *
 * [blendFactors] property allow to specify the blending mode to be used.
 * [multiplyColor] will set the multiplicative color to be usd when drawing rects and images.
 *
 * [scissor] methods allow to specify a scissor rectangle limiting the area where the pixels will be renderer.
 */
@UseExperimental(KorgeInternal::class)
class RenderContext2D(
    @KorgeInternal
    val batch: BatchBuilder2D,
    @KorgeInternal
    val agBitmapTextureManager: AgBitmapTextureManager
) : Extra by Extra.Mixin() {
	init { logger.trace { "RenderContext2D[0]" } }

    @KorgeInternal
	val mpool = Pool<Matrix> { Matrix() }

	init { logger.trace { "RenderContext2D[1]" } }

    @KorgeInternal
	val m = Matrix()

    /** Blending mode to be used in the renders */
	var blendFactors = AG.Blending.NORMAL
    /** Multiplicative color to be used in the renders */
	var multiplyColor = Colors.WHITE

	init { logger.trace { "RenderContext2D[2]" } }

    /** Executes [callback] restoring the initial transformation [Matrix] at the end */
	inline fun <T> keepMatrix(crossinline callback: () -> T) = mpool.alloc { matrix ->
		matrix.copyFrom(m)
		try {
			callback()
		} finally {
			m.copyFrom(matrix)
		}
	}

    /** Executes [callback] restoring the initial [blendFactors] at the end */
	inline fun <T> keepBlendFactors(crossinline callback: () -> T): T {
		val oldBlendFactors = this.blendFactors
		try {
			return callback()
		} finally {
			this.blendFactors = oldBlendFactors
		}
	}

    /** Executes [callback] restoring the initial [multiplyColor] at the end */
    inline fun <T> keepColor(crossinline callback: () -> T): T {
        val multiplyColor = this.multiplyColor
        try {
            return callback()
        } finally {
            this.multiplyColor = multiplyColor
        }
    }

    /** Executes [callback] restoring the transform matrix, the [blendFactors] and the [multiplyColor] at the end */
	inline fun <T> keep(crossinline callback: () -> T): T {
		return keepMatrix {
			keepBlendFactors {
				keepColor {
					callback()
				}
			}
		}
	}

    /** Sets the current transform [matrix] */
	fun setMatrix(matrix: Matrix) {
		this.m.copyFrom(matrix)
	}

    /** Translates the current transform matrix by [dx] and [dy] */
	fun translate(dx: Double, dy: Double) {
		m.pretranslate(dx, dy)
	}

    /** Scales the current transform matrix by [sx] and [sy] */
	fun scale(sx: Double, sy: Double = sx) {
		m.prescale(sx, sy)
	}

    /** Scales the current transform matrix by [scale] */
	fun scale(scale: Double) {
		m.prescale(scale, scale)
	}

    /** Rotates the current transform matrix by [angle] */
	fun rotate(angle: Angle) {
		m.prerotate(angle)
	}

    /** Renders a colored rectangle with the [multiplyColor] with the [blendFactors] at [x], [y] of size [width]x[height] */
    fun rect(x: Double, y: Double, width: Double, height: Double) {
        batch.drawQuad(
            agBitmapTextureManager.getTexture(Bitmaps.white),
            x.toFloat(),
            y.toFloat(),
            width.toFloat(),
            height.toFloat(),
            m = m,
            colorMul = multiplyColor,
            blendFactors = blendFactors
        )
    }

    /** Renders a [texture] with the [blendFactors] at [x], [y] scaling it by [scale].
     * The texture colors will be multiplied by [multiplyColor]. Since it is multiplicative, white won't cause any effect. */
	fun imageScale(texture: Texture, x: Double, y: Double, scale: Double = 1.0) {
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

    /** Temporarily sets the [scissor] (visible rendering area) to [x], [y], [width] and [height] while [block] is executed. */
    inline fun scissor(x: Int, y: Int, width: Int, height: Int, block: () -> Unit) = scissor(tempScissor.setTo(x, y, width, height), block)

    /** Temporarily sets the [scissor] (visible rendering area) to [x], [y], [width] and [height] while [block] is executed. */
    @Deprecated("Kotlin/Native boxes inline+Number")
    inline fun scissor(x: Number, y: Number, width: Number, height: Number, block: () -> Unit) = scissor(x.toInt(), y.toInt(), width.toInt(), height.toInt(), block)

    /** Temporarily sets the [scissor] (visible rendering area) to [x], [y], [width] and [height] while [block] is executed. */
    inline fun scissor(x: Double, y: Double, width: Double, height: Double, block: () -> Unit) = scissor(x.toInt(), y.toInt(), width.toInt(), height.toInt(), block)

    /** Temporarily sets the [scissor] (visible rendering area) to [rect] is executed. */
    inline fun scissor(rect: Rectangle, block: () -> Unit) =
        scissor(rect.x, rect.y, rect.width, rect.height, block)

    /** Temporarily sets the [scissor] (visible rendering area) to [scissor] is executed. */
    inline fun scissor(scissor: AG.Scissor?, block: () -> Unit) {
        val oldScissor = batch.scissor
        val returnScissor = scissorStart(scissor)
        try {
            block()
        } finally {
            scissorEnd(oldScissor, returnScissor)
        }
    }

    @PublishedApi
    internal val scissorPool = Pool(8) { AG.Scissor(0, 0, 0, 0) }

    @PublishedApi
    internal fun scissorStart(scissor: AG.Scissor?): AG.Scissor? {
        var returnScissor: AG.Scissor? = null

        batch.flush()
        if (scissor != null) {
            val left = m.fastTransformX(scissor.left.toDouble(), scissor.top.toDouble()).toInt()
            val top = m.fastTransformY(scissor.left.toDouble(), scissor.top.toDouble()).toInt()
            val right = m.fastTransformX(scissor.right.toDouble(), scissor.bottom.toDouble()).toInt()
            val bottom = m.fastTransformY(scissor.right.toDouble(), scissor.bottom.toDouble()).toInt()

            returnScissor = scissorPool.alloc().setTo(left, top, right - left, bottom - top)

            batch.scissor = returnScissor
            //println("batch.scissor: ${batch.scissor}")
        } else {
            batch.scissor = null
        }

        return returnScissor
    }

    @PublishedApi
    internal fun scissorEnd(oldScissor: AG.Scissor?, returnScissor: AG.Scissor?) {
        if (returnScissor != null) {
            scissorPool.free(returnScissor)
        }

        batch.flush()
        batch.scissor = oldScissor
    }

    @PublishedApi
    internal val tempScissor: AG.Scissor = AG.Scissor(0, 0, 0, 0)
}

// @TODO: Remove once KorGW is updated
@PublishedApi
internal fun AG.Scissor.copyFrom(that: AG.Scissor): AG.Scissor {
    this.x = that.x
    this.y = that.y
    this.width = that.width
    this.height = that.height
    return this
}

// @TODO: Remove once KorGW is updated
@PublishedApi
internal fun AG.Scissor.setTo(x: Int, y: Int, width: Int, height: Int): AG.Scissor {
    this.x = x
    this.y = y
    this.width = width
    this.height = height
    return this
}
