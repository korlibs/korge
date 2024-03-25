package korlibs.math

import kotlin.math.absoluteValue

///** Check if the absolute value of [this] floating point value is small (abs(this) <= 1e-6) */
//public fun Float.isAlmostZero(): Boolean = abs(this) <= 1e-6
///** Check if the absolute value of [this] floating point value is small (abs(this) <= 1e-19) */
//public fun Double.isAlmostZero(): Boolean = abs(this) <= 1e-19
//

//fun Double.isAlmostEquals(other: Double, epsilon: Double = 0.0001): Boolean = (this - other).absoluteValue < epsilon
fun Double.isAlmostEquals(other: Double, epsilon: Double = 0.000001): Boolean = (this - other).absoluteValue < epsilon
fun Double.isAlmostZero(): Boolean = kotlin.math.abs(this) <= 1e-19

fun Float.isAlmostEquals(other: Float, epsilon: Float = 0.00001f): Boolean = (this - other).absoluteValue < epsilon
fun Float.isAlmostZero(): Boolean = kotlin.math.abs(this) <= 1e-6
