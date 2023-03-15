package com.soywiz.korma.interpolation

import com.soywiz.kds.fastCastTo
import com.soywiz.korma.geom.*

interface Interpolable<T> {
    fun interpolateWith(ratio: Ratio, other: T): T
}

interface MutableInterpolable<T> {
    fun setToInterpolated(ratio: Ratio, l: T, r: T): T
}

fun Ratio.interpolate(l: Point, r: Point): Point = Point(interpolate(l.x, r.x), interpolate(l.y, r.y))
fun Ratio.interpolate(l: Size, r: Size): Size = Size(interpolate(l.width, r.width), interpolate(l.height, r.height))
fun Ratio.interpolate(l: Scale, r: Scale): Scale = Scale(interpolate(l.scaleX, r.scaleX), interpolate(l.scaleY, r.scaleY))
fun Ratio.interpolate(l: Float, r: Float): Float = (l + (r - l) * valueF)
fun Ratio.interpolate(l: Double, r: Double): Double = (l + (r - l) * valueD)
fun Ratio.interpolate(l: Int, r: Int): Int = (l + (r - l) * valueD).toInt()
fun Ratio.interpolate(l: Long, r: Long): Long = (l + (r - l) * valueD).toLong()
fun <T> Ratio.interpolate(l: Interpolable<T>, r: Interpolable<T>): T = l.interpolateWith(this, r.fastCastTo<T>())
fun <T : Interpolable<T>> Ratio.interpolate(l: T, r: T): T = l.interpolateWith(this, r)

fun Ratio.interpolate(l: Matrix, r: Matrix): Matrix = Matrix.interpolated(l, r, this)
fun Ratio.interpolate(l: MatrixTransform, r: MatrixTransform): MatrixTransform = MatrixTransform.interpolated(l, r, this)
