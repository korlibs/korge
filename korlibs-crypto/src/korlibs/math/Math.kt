package korlibs.math

import kotlin.math.*

fun min(a: Int, b: Int, c: Int) = min(min(a, b), c)
fun min(a: Float, b: Float, c: Float) = min(min(a, b), c)
fun min(a: Double, b: Double, c: Double) = min(min(a, b), c)

fun min(a: Int, b: Int, c: Int, d: Int) = min(min(min(a, b), c), d)
fun min(a: Float, b: Float, c: Float, d: Float) = min(min(min(a, b), c), d)
fun min(a: Double, b: Double, c: Double, d: Double) = min(min(min(a, b), c), d)

fun min(a: Int, b: Int, c: Int, d: Int, e: Int) = min(min(min(min(a, b), c), d), e)
fun min(a: Float, b: Float, c: Float, d: Float, e: Float) = min(min(min(min(a, b), c), d), e)
fun min(a: Double, b: Double, c: Double, d: Double, e: Double) = min(min(min(min(a, b), c), d), e)
