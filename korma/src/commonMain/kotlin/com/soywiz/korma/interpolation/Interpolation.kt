package com.soywiz.korma.interpolation

interface Interpolable<T> {
    fun interpolateWith(ratio: Double, other: T): T
}

interface MutableInterpolable<T> {
    fun setToInterpolated(ratio: Double, l: T, r: T): T
}

@Suppress("UNCHECKED_CAST", "USELESS_CAST", "DeprecatedCallableAddReplaceWith")
@Deprecated("Kotlin.JS can't differentiate numeric types, so this might cause strange issues with Ints having decimals")
fun <T> Double.interpolateAny(min: T, max: T): T = when (min) {
    is Float -> this.interpolate(min as Float, max as Float) as T
    is Int -> this.interpolate(min as Int, max as Int) as T
    is Double -> this.interpolate(min as Double, max as Double) as T
    is Long -> this.interpolate(min as Long, max as Long) as T
    is Interpolable<*> -> (min as Interpolable<Any>).interpolateWith(this, max as Interpolable<Any>) as T
    else -> throw IllegalArgumentException("Value is not interpolable")
}

fun Double.interpolate(l: Float, r: Float): Float = (l + (r - l) * this).toFloat()
fun Double.interpolate(l: Double, r: Double): Double = (l + (r - l) * this)
fun Double.interpolate(l: Int, r: Int): Int = (l + (r - l) * this).toInt()
fun Double.interpolate(l: Long, r: Long): Long = (l + (r - l) * this).toLong()
fun <T> Double.interpolate(l: Interpolable<T>, r: Interpolable<T>): T = l.interpolateWith(this, r as T)
fun <T : Interpolable<T>> Double.interpolate(l: T, r: T): T = l.interpolateWith(this, r)
