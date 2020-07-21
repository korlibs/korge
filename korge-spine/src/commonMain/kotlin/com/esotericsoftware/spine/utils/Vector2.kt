package com.esotericsoftware.spine.utils

/** Encapsulates a 2D vector. Allows chaining methods by returning a reference to itself
 * @author badlogicgames@gmail.com
 */
data class Vector2(
    /** the x-component of this vector  */
    var x: Float = 0f,
    /** the y-component of this vector  */
    var y: Float = 0f
) {
    /** Constructs a vector from the given vector
     * @param v The vector
     */
    constructor(v: Vector2) : this(v.x, v.y)

    /** Sets the components of this vector
     * @param x The x-component
     * @param y The y-component
     * @return This vector for chaining
     */
    operator fun set(x: Float, y: Float): Vector2 {
        this.x = x
        this.y = y
        return this
    }

    /** Converts this `Vector2` to a string in the format `(x,y)`.
     * @return a string representation of this object.
     */
    override fun toString(): String = "($x,$y)"

    /** Rotates the Vector2 by the given angle, counter-clockwise assuming the y-axis points up.
     * @param degrees the angle in degrees
     */
    fun rotate(degrees: Float): Vector2 {
        return rotateRad(degrees * MathUtils.degreesToRadians)
    }

    /** Rotates the Vector2 by the given angle, counter-clockwise assuming the y-axis points up.
     * @param radians the angle in radians
     */
    fun rotateRad(radians: Float): Vector2 {
        val cos = kotlin.math.cos(radians.toDouble()).toFloat()
        val sin = kotlin.math.sin(radians.toDouble()).toFloat()

        val newX = this.x * cos - this.y * sin
        val newY = this.x * sin + this.y * cos

        this.x = newX
        this.y = newY

        return this
    }

    companion object {
        val X = Vector2(1f, 0f)
        val Y = Vector2(0f, 1f)
    }
}
/** Compares this vector with the other vector using MathUtils.FLOAT_ROUNDING_ERROR for fuzzy equality testing
 * @param x x component of the other vector to compare
 * @param y y component of the other vector to compare
 * @return true if vector are equal, otherwise false
 */
