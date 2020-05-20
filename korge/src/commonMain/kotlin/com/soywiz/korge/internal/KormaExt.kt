package com.soywiz.korge.internal

import com.soywiz.korma.geom.*

// @TODO: Move these to KorMA: instead of using IMatrix that requires a method lookup, we use Matrix directly that hopefully makes a much faster direct field access
// @TODO: We might need to totally remove IMatrix to avoid method lookup since the properties have an override val

internal fun Matrix.fastTransformX(px: Double, py: Double): Double = (this.a * px + this.c * py + this.tx)
internal fun Matrix.fastTransformY(px: Double, py: Double): Double = (this.d * py + this.b * px + this.ty)

internal fun Matrix.fastTransformX(p: Point): Double = fastTransformX(p.x, p.y)
internal fun Matrix.fastTransformY(p: Point): Double = fastTransformY(p.x, p.y)

internal fun Matrix.fastTransformXf(px: Double, py: Double): Float = (this.a * px + this.c * py + this.tx).toFloat()
internal fun Matrix.fastTransformYf(px: Double, py: Double): Float = (this.d * py + this.b * px + this.ty).toFloat()

internal fun Matrix.fastTransformXf(px: Float, py: Float): Float = (this.a * px + this.c * py + this.tx).toFloat()
internal fun Matrix.fastTransformYf(px: Float, py: Float): Float = (this.d * py + this.b * px + this.ty).toFloat()
