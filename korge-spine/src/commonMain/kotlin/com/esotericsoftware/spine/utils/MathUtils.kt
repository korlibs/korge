package com.esotericsoftware.spine.utils

import kotlin.math.*

/** Utility and fast math functions.
 *
 *
 * Thanks to Riven on JavaGaming.org for the basis of sin/cos/floor/ceil.
 * @author Nathan Sweet
 */
object MathUtils {

    // ---
    const val FLOAT_ROUNDING_ERROR = 0.000001f // 32 bits
    const val PI = 3.1415927f
    const val PI2 = PI * 2
    private const val SIN_BITS = 14 // 16KB. Adjust for accuracy.
    private const val SIN_MASK = (-1 shl SIN_BITS).inv()
    private const val SIN_COUNT = SIN_MASK + 1
    private const val radFull = PI * 2
    private const val degFull = 360f
    private const val radToIndex = SIN_COUNT / radFull
    private const val degToIndex = SIN_COUNT / degFull

    /** multiply by this to convert from radians to degrees  */
    const val radiansToDegrees = 180f / PI
    const val radDeg = radiansToDegrees

    /** multiply by this to convert from degrees to radians  */
    const val degreesToRadians = PI / 180
    const val degRad = degreesToRadians

    /** Returns the sine in radians from a lookup table. For optimal precision, use radians between -PI2 and PI2 (both
     * inclusive).  */

    fun sin(radians: Float): Float = SIN_TABLE[(radians * radToIndex).toInt() and SIN_MASK]

    /** Returns the cosine in radians from a lookup table. For optimal precision, use radians between -PI2 and PI2 (both
     * inclusive).  */

    fun cos(radians: Float): Float = SIN_TABLE[((radians + PI / 2) * radToIndex).toInt() and SIN_MASK]

    /** Returns the sine in degrees from a lookup table. For optimal precision, use radians between -360 and 360 (both
     * inclusive).  */

    fun sinDeg(degrees: Float): Float = SIN_TABLE[(degrees * degToIndex).toInt() and SIN_MASK]

    /** Returns the cosine in degrees from a lookup table. For optimal precision, use radians between -360 and 360 (both
     * inclusive).  */

    fun cosDeg(degrees: Float): Float = SIN_TABLE[((degrees + 90) * degToIndex).toInt() and SIN_MASK]
    // ---
    /** Returns atan2 in radians, less accurate than atan2 but may be faster. Average error of 0.00231 radians (0.1323
     * degrees), largest error of 0.00488 radians (0.2796 degrees).  */

    fun atan2(y: Float, x: Float): Float {
        if (x == 0f) {
            if (y > 0f) return PI / 2
            return if (y == 0f) 0f else -PI / 2
        }
        val atan: Float
        val z = y / x
        if (abs(z) < 1f) {
            atan = z / (1f + 0.28f * z * z)
            return if (x < 0f) atan + (if (y < 0f) -PI else PI) else atan
        }
        atan = PI / 2 - z / (z * z + 0.28f)
        return if (y < 0f) atan - PI else atan
    }

    private val SIN_TABLE = FloatArray(SIN_COUNT).also { table ->
        for (i in 0 until SIN_COUNT) table[i] = sin((i + 0.5f) / SIN_COUNT * radFull.toDouble()).toFloat()
        var i = 0
        while (i < 360) {
            table[(i * degToIndex) as Int and SIN_MASK] = sin(i * degreesToRadians.toDouble()).toFloat()
            i += 90
        }
    }
}
