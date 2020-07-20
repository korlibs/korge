package com.badlogic.gdx.math

import java.util.*

/** Utility and fast math functions.
 *
 *
 * Thanks to Riven on JavaGaming.org for the basis of sin/cos/floor/ceil.
 * @author Nathan Sweet
 */
object MathUtils {
    const val nanoToSec = 1 / 1000000000f

    // ---
    const val FLOAT_ROUNDING_ERROR = 0.000001f // 32 bits
    const val PI = 3.1415927f
    const val PI2 = PI * 2
    const val E = 2.7182818f
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
    @JvmStatic
    fun sin(radians: Float): Float {
        return Sin.table[(radians * radToIndex).toInt() and SIN_MASK]
    }

    /** Returns the cosine in radians from a lookup table. For optimal precision, use radians between -PI2 and PI2 (both
     * inclusive).  */
    @JvmStatic
    fun cos(radians: Float): Float {
        return Sin.table[((radians + PI / 2) * radToIndex).toInt() and SIN_MASK]
    }

    /** Returns the sine in degrees from a lookup table. For optimal precision, use radians between -360 and 360 (both
     * inclusive).  */
    @JvmStatic
    fun sinDeg(degrees: Float): Float {
        return Sin.table[(degrees * degToIndex).toInt() and SIN_MASK]
    }

    /** Returns the cosine in degrees from a lookup table. For optimal precision, use radians between -360 and 360 (both
     * inclusive).  */
    @JvmStatic
    fun cosDeg(degrees: Float): Float {
        return Sin.table[((degrees + 90) * degToIndex).toInt() and SIN_MASK]
    }
    // ---
    /** Returns atan2 in radians, less accurate than Math.atan2 but may be faster. Average error of 0.00231 radians (0.1323
     * degrees), largest error of 0.00488 radians (0.2796 degrees).  */
    @JvmStatic
    fun atan2(y: Float, x: Float): Float {
        if (x == 0f) {
            if (y > 0f) return PI / 2
            return if (y == 0f) 0f else -PI / 2
        }
        val atan: Float
        val z = y / x
        if (Math.abs(z) < 1f) {
            atan = z / (1f + 0.28f * z * z)
            return if (x < 0f) atan + (if (y < 0f) -PI else PI) else atan
        }
        atan = PI / 2 - z / (z * z + 0.28f)
        return if (y < 0f) atan - PI else atan
    }

    /** Returns acos in radians, less accurate than Math.acos but may be faster.  */
    @JvmStatic
    fun acos(a: Float): Float {
        var a = a
        return 1.5707963267948966f - a * (1f + a.let { a *= it; a } * (-0.141514171442891431f + a * -0.719110791477959357f)) / 1f + a * (-0.439110389941411144f + a * -0.471306172023844527f)
    }

    /** Returns asin in radians, less accurate than Math.asin but may be faster.  */
    @JvmStatic
    fun asin(a: Float): Float {
        var a = a
        return (a * (1f + a.let { a *= it; a } * (-0.141514171442891431f + a * -0.719110791477959357f))
            / (1f + a * (-0.439110389941411144f + a * -0.471306172023844527f)))
    }

    // ---
    var random = Random()

    /** Returns a random number between 0 (inclusive) and the specified value (inclusive).  */
    @JvmStatic
    fun random(range: Int): Int {
        return random.nextInt(range + 1)
    }

    /** Returns a random number between start (inclusive) and end (inclusive).  */
    @JvmStatic
    fun random(start: Int, end: Int): Int {
        return start + random.nextInt(end - start + 1)
    }

    /** Returns a random number between 0 (inclusive) and the specified value (inclusive).  */
    @JvmStatic
    fun random(range: Long): Long {
        return (random.nextDouble() * range).toLong()
    }

    /** Returns a random number between start (inclusive) and end (inclusive).  */
    @JvmStatic
    fun random(start: Long, end: Long): Long {
        return start + (random.nextDouble() * (end - start)).toLong()
    }

    /** Returns a random boolean value.  */
    @JvmStatic
    fun randomBoolean(): Boolean {
        return random.nextBoolean()
    }

    /** Returns true if a random value between 0 and 1 is less than the specified value.  */
    @JvmStatic
    fun randomBoolean(chance: Float): Boolean {
        return random() < chance
    }

    /** Returns random number between 0.0 (inclusive) and 1.0 (exclusive).  */
    @JvmStatic
    fun random(): Float {
        return random.nextFloat()
    }

    /** Returns a random number between 0 (inclusive) and the specified value (exclusive).  */
    @JvmStatic
    fun random(range: Float): Float {
        return random.nextFloat() * range
    }

    /** Returns a random number between start (inclusive) and end (exclusive).  */
    @JvmStatic
    fun random(start: Float, end: Float): Float {
        return start + random.nextFloat() * (end - start)
    }

    /** Returns -1 or 1, randomly.  */
    @JvmStatic
    fun randomSign(): Int {
        return 1 or (random.nextInt() shr 31)
    }

    /** Returns a triangularly distributed random number between -1.0 (exclusive) and 1.0 (exclusive), where values around zero are
     * more likely.
     *
     *
     * This is an optimized version of [randomTriangular(-1, 1, 0)][.randomTriangular]  */
    @JvmStatic
    fun randomTriangular(): Float {
        return random.nextFloat() - random.nextFloat()
    }

    /** Returns a triangularly distributed random number between `-max` (exclusive) and `max` (exclusive), where values
     * around zero are more likely.
     *
     *
     * This is an optimized version of [randomTriangular(-max, max, 0)][.randomTriangular]
     * @param max the upper limit
     */
    @JvmStatic
    fun randomTriangular(max: Float): Float {
        return (random.nextFloat() - random.nextFloat()) * max
    }
    /** Returns a triangularly distributed random number between `min` (inclusive) and `max` (exclusive), where values
     * around `mode` are more likely.
     * @param min the lower limit
     * @param max the upper limit
     * @param mode the point around which the values are more likely
     */
    /** Returns a triangularly distributed random number between `min` (inclusive) and `max` (exclusive), where the
     * `mode` argument defaults to the midpoint between the bounds, giving a symmetric distribution.
     *
     *
     * This method is equivalent of [randomTriangular(min, max, (min + max) * .5f)][.randomTriangular]
     * @param min the lower limit
     * @param max the upper limit
     */
    @JvmOverloads
    @JvmStatic
    fun randomTriangular(min: Float, max: Float, mode: Float = (min + max) * 0.5f): Float {
        val u = random.nextFloat()
        val d = max - min
        return if (u <= (mode - min) / d) min + Math.sqrt(u * d * (mode - min).toDouble()).toFloat() else max - Math.sqrt((1 - u) * d * (max - mode).toDouble()).toFloat()
    }
    // ---
    /** Returns the next power of two. Returns the specified value if the value is already a power of two.  */
    @JvmStatic
    fun nextPowerOfTwo(value: Int): Int {
        var value = value
        if (value == 0) return 1
        value--
        value = value or (value shr 1)
        value = value or (value shr 2)
        value = value or (value shr 4)
        value = value or (value shr 8)
        value = value or (value shr 16)
        return value + 1
    }

    @JvmStatic
    fun isPowerOfTwo(value: Int): Boolean {
        return value != 0 && value and value - 1 == 0
    }

    // ---
    @JvmStatic
    fun clamp(value: Short, min: Short, max: Short): Short {
        if (value < min) return min
        return if (value > max) max else value
    }

    @JvmStatic
    fun clamp(value: Int, min: Int, max: Int): Int {
        if (value < min) return min
        return if (value > max) max else value
    }

    @JvmStatic
    fun clamp(value: Long, min: Long, max: Long): Long {
        if (value < min) return min
        return if (value > max) max else value
    }

    @JvmStatic
    fun clamp(value: Float, min: Float, max: Float): Float {
        if (value < min) return min
        return if (value > max) max else value
    }

    @JvmStatic
    fun clamp(value: Double, min: Double, max: Double): Double {
        if (value < min) return min
        return if (value > max) max else value
    }
    // ---
    /** Linearly interpolates between fromValue to toValue on progress position.  */
    @JvmStatic
    fun lerp(fromValue: Float, toValue: Float, progress: Float): Float {
        return fromValue + (toValue - fromValue) * progress
    }

    /** Linearly normalizes value from a range. Range must not be empty. This is the inverse of [.lerp].
     * @param rangeStart Range start normalized to 0
     * @param rangeEnd Range end normalized to 1
     * @param value Value to normalize
     * @return Normalized value. Values outside of the range are not clamped to 0 and 1
     */
    @JvmStatic
    fun norm(rangeStart: Float, rangeEnd: Float, value: Float): Float {
        return (value - rangeStart) / (rangeEnd - rangeStart)
    }

    /** Linearly map a value from one range to another. Input range must not be empty. This is the same as chaining
     * [.norm] from input range and [.lerp] to output range.
     * @param inRangeStart Input range start
     * @param inRangeEnd Input range end
     * @param outRangeStart Output range start
     * @param outRangeEnd Output range end
     * @param value Value to map
     * @return Mapped value. Values outside of the input range are not clamped to output range
     */
    @JvmStatic
    fun map(inRangeStart: Float, inRangeEnd: Float, outRangeStart: Float, outRangeEnd: Float, value: Float): Float {
        return outRangeStart + (value - inRangeStart) * (outRangeEnd - outRangeStart) / (inRangeEnd - inRangeStart)
    }

    /** Linearly interpolates between two angles in radians. Takes into account that angles wrap at two pi and always takes the
     * direction with the smallest delta angle.
     *
     * @param fromRadians start angle in radians
     * @param toRadians target angle in radians
     * @param progress interpolation value in the range [0, 1]
     * @return the interpolated angle in the range [0, PI2[
     */
    @JvmStatic
    fun lerpAngle(fromRadians: Float, toRadians: Float, progress: Float): Float {
        val delta = (toRadians - fromRadians + PI2 + PI) % PI2 - PI
        return (fromRadians + delta * progress + PI2) % PI2
    }

    /** Linearly interpolates between two angles in degrees. Takes into account that angles wrap at 360 degrees and always takes
     * the direction with the smallest delta angle.
     *
     * @param fromDegrees start angle in degrees
     * @param toDegrees target angle in degrees
     * @param progress interpolation value in the range [0, 1]
     * @return the interpolated angle in the range [0, 360[
     */
    @JvmStatic
    fun lerpAngleDeg(fromDegrees: Float, toDegrees: Float, progress: Float): Float {
        val delta = (toDegrees - fromDegrees + 360 + 180) % 360 - 180
        return (fromDegrees + delta * progress + 360) % 360
    }

    // ---
    private const val BIG_ENOUGH_INT = 16 * 1024
    private const val BIG_ENOUGH_FLOOR = BIG_ENOUGH_INT.toDouble()
    private const val CEIL = 0.9999999
    private const val BIG_ENOUGH_CEIL = 16384.999999999996
    private const val BIG_ENOUGH_ROUND = BIG_ENOUGH_INT + 0.5f.toDouble()

    /** Returns the largest integer less than or equal to the specified float. This method will only properly floor floats from
     * -(2^14) to (Float.MAX_VALUE - 2^14).  */
    @JvmStatic
    fun floor(value: Float): Int {
        return (value + BIG_ENOUGH_FLOOR).toInt() - BIG_ENOUGH_INT
    }

    /** Returns the largest integer less than or equal to the specified float. This method will only properly floor floats that are
     * positive. Note this method simply casts the float to int.  */
    @JvmStatic
    fun floorPositive(value: Float): Int {
        return value.toInt()
    }

    /** Returns the smallest integer greater than or equal to the specified float. This method will only properly ceil floats from
     * -(2^14) to (Float.MAX_VALUE - 2^14).  */
    @JvmStatic
    fun ceil(value: Float): Int {
        return BIG_ENOUGH_INT - (BIG_ENOUGH_FLOOR - value).toInt()
    }

    /** Returns the smallest integer greater than or equal to the specified float. This method will only properly ceil floats that
     * are positive.  */
    @JvmStatic
    fun ceilPositive(value: Float): Int {
        return (value + CEIL).toInt()
    }

    /** Returns the closest integer to the specified float. This method will only properly round floats from -(2^14) to
     * (Float.MAX_VALUE - 2^14).  */
    @JvmStatic
    fun round(value: Float): Int {
        return (value + BIG_ENOUGH_ROUND).toInt() - BIG_ENOUGH_INT
    }

    /** Returns the closest integer to the specified float. This method will only properly round floats that are positive.  */
    @JvmStatic
    fun roundPositive(value: Float): Int {
        return (value + 0.5f).toInt()
    }

    /** Returns true if the value is zero (using the default tolerance as upper bound)  */
    @JvmStatic
    fun isZero(value: Float): Boolean {
        return Math.abs(value) <= FLOAT_ROUNDING_ERROR
    }

    /** Returns true if the value is zero.
     * @param tolerance represent an upper bound below which the value is considered zero.
     */
    @JvmStatic
    fun isZero(value: Float, tolerance: Float): Boolean {
        return Math.abs(value) <= tolerance
    }

    /** Returns true if a is nearly equal to b. The function uses the default floating error tolerance.
     * @param a the first value.
     * @param b the second value.
     */
    @JvmStatic
    fun isEqual(a: Float, b: Float): Boolean {
        return Math.abs(a - b) <= FLOAT_ROUNDING_ERROR
    }

    /** Returns true if a is nearly equal to b.
     * @param a the first value.
     * @param b the second value.
     * @param tolerance represent an upper bound below which the two values are considered equal.
     */
    @JvmStatic
    fun isEqual(a: Float, b: Float, tolerance: Float): Boolean {
        return Math.abs(a - b) <= tolerance
    }

    /** @return the logarithm of value with base a
     */
    @JvmStatic
    fun log(a: Float, value: Float): Float {
        return (Math.log(value.toDouble()) / Math.log(a.toDouble())).toFloat()
    }

    /** @return the logarithm of value with base 2
     */
    @JvmStatic
    fun log2(value: Float): Float {
        return log(2f, value)
    }

    private object Sin {
        val table = FloatArray(SIN_COUNT).also { table ->
            for (i in 0 until SIN_COUNT) table[i] = Math.sin((i + 0.5f) / SIN_COUNT * radFull.toDouble()).toFloat()
            var i = 0
            while (i < 360) {
                table[(i * degToIndex) as Int and SIN_MASK] = Math.sin(i * degreesToRadians.toDouble()).toFloat()
                i += 90
            }
        }
    }
}
