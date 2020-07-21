/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.badlogic.gdx.math

import java.io.Serializable

import com.badlogic.gdx.utils.GdxRuntimeException

/** Encapsulates a 3D vector. Allows chaining operations by returning a reference to itself in all modification methods.
 * @author badlogicgames@gmail.com
 */
class Vector3 : Serializable {

    /** the x-component of this vector  */
    @JvmField
    var x: Float = 0.toFloat()

    /** the y-component of this vector  */
    @JvmField
    var y: Float = 0.toFloat()

    /** the z-component of this vector  */
    @JvmField
    var z: Float = 0.toFloat()

    val isUnit: Boolean
        get() = isUnit(0.000000001f)

    val isZero: Boolean
        get() = x == 0f && y == 0f && z == 0f

    /** Constructs a vector at (0,0,0)  */
    constructor() {}

    /** Creates a vector with the given components
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     */
    constructor(x: Float, y: Float, z: Float) {
        this[x, y] = z
    }

    /** Creates a vector from the given vector
     * @param vector The vector
     */
    constructor(vector: Vector3) {
        this.set(vector)
    }

    /** Sets the vector to the given components
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     * @return this vector for chaining
     */
    operator fun set(x: Float, y: Float, z: Float): Vector3 {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    fun set(vector: Vector3): Vector3 {
        return this.set(vector.x, vector.y, vector.z)
    }

    /** Sets the components from the array. The array must have at least 3 elements
     *
     * @param values The array
     * @return this vector for chaining
     */
    fun set(values: FloatArray): Vector3 {
        return this.set(values[0], values[1], values[2])
    }

    /** Sets the components of the given vector and z-component
     *
     * @param vector The vector
     * @param z The z-component
     * @return This vector for chaining
     */
    operator fun set(vector: Vector2, z: Float): Vector3 {
        return this.set(vector.x, vector.y, z)
    }

    /** Sets the components from the given spherical coordinate
     * @param azimuthalAngle The angle between x-axis in radians [0, 2pi]
     * @param polarAngle The angle between z-axis in radians [0, pi]
     * @return This vector for chaining
     */
    fun setFromSpherical(azimuthalAngle: Float, polarAngle: Float): Vector3 {
        val cosPolar = MathUtils.cos(polarAngle)
        val sinPolar = MathUtils.sin(polarAngle)

        val cosAzim = MathUtils.cos(azimuthalAngle)
        val sinAzim = MathUtils.sin(azimuthalAngle)

        return this.set(cosAzim * sinPolar, sinAzim * sinPolar, cosPolar)
    }

    fun setToRandomDirection(): Vector3 {
        val u = MathUtils.random()
        val v = MathUtils.random()

        val theta = MathUtils.PI2 * u // azimuthal angle
        val phi = Math.acos((2f * v - 1f).toDouble()).toFloat() // polar angle

        return this.setFromSpherical(theta, phi)
    }

    fun cpy(): Vector3 {
        return Vector3(this)
    }

    fun add(vector: Vector3): Vector3 {
        return this.add(vector.x, vector.y, vector.z)
    }

    /** Adds the given vector to this component
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @param z The z-component of the other vector
     * @return This vector for chaining.
     */
    fun add(x: Float, y: Float, z: Float): Vector3 {
        return this.set(this.x + x, this.y + y, this.z + z)
    }

    /** Adds the given value to all three components of the vector.
     *
     * @param values The value
     * @return This vector for chaining
     */
    fun add(values: Float): Vector3 {
        return this.set(this.x + values, this.y + values, this.z + values)
    }

    fun sub(a_vec: Vector3): Vector3 {
        return this.sub(a_vec.x, a_vec.y, a_vec.z)
    }

    /** Subtracts the other vector from this vector.
     *
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @param z The z-component of the other vector
     * @return This vector for chaining
     */
    fun sub(x: Float, y: Float, z: Float): Vector3 {
        return this.set(this.x - x, this.y - y, this.z - z)
    }

    fun scl(scalar: Float): Vector3 {
        return this.set(this.x * scalar, this.y * scalar, this.z * scalar)
    }

    fun len2(): Float {
        return x * x + y * y + z * z
    }

    fun nor(): Vector3 {
        val len2 = this.len2()
        return if (len2 == 0f || len2 == 1f) this else this.scl(1f / Math.sqrt(len2.toDouble()).toFloat())
    }

    fun dot(vector: Vector3): Float {
        return x * vector.x + y * vector.y + z * vector.z
    }

    /** Sets this vector to the cross product between it and the other vector.
     * @param vector The other vector
     * @return This vector for chaining
     */
    fun crs(vector: Vector3): Vector3 {
        return this.set(y * vector.z - z * vector.y, z * vector.x - x * vector.z, x * vector.y - y * vector.x)
    }

    /** Left-multiplies the vector by the given matrix, assuming the fourth (w) component of the vector is 1.
     * @param matrix The matrix
     * @return This vector for chaining
     */
    fun mul(matrix: Matrix4): Vector3 {
        val l_mat = matrix.`val`
        return this.set(x * l_mat[Matrix4.M00] + y * l_mat[Matrix4.M01] + z * l_mat[Matrix4.M02] + l_mat[Matrix4.M03], x * l_mat[Matrix4.M10] + y * l_mat[Matrix4.M11] + z * l_mat[Matrix4.M12] + l_mat[Matrix4.M13], x * l_mat[Matrix4.M20] + y * l_mat[Matrix4.M21] + z * l_mat[Matrix4.M22] + l_mat[Matrix4.M23])
    }

    /** Left-multiplies the vector by the given matrix.
     * @param matrix The matrix
     * @return This vector for chaining
     */
    fun mul(matrix: Matrix3): Vector3 {
        val l_mat = matrix.`val`
        return set(x * l_mat[Matrix3.M00] + y * l_mat[Matrix3.M01] + z * l_mat[Matrix3.M02], x * l_mat[Matrix3.M10] + y * l_mat[Matrix3.M11] + z * l_mat[Matrix3.M12], x * l_mat[Matrix3.M20] + y * l_mat[Matrix3.M21] + z * l_mat[Matrix3.M22])
    }

    /** Multiplies the vector by the given [Quaternion].
     * @return This vector for chaining
     */
    fun mul(quat: Quaternion): Vector3 {
        return quat.transform(this)
    }

    /** Multiplies this vector by the given matrix dividing by w, assuming the fourth (w) component of the vector is 1. This is
     * mostly used to project/unproject vectors via a perspective projection matrix.
     *
     * @param matrix The matrix.
     * @return This vector for chaining
     */
    fun prj(matrix: Matrix4): Vector3 {
        val l_mat = matrix.`val`
        val l_w = 1f / (x * l_mat[Matrix4.M30] + y * l_mat[Matrix4.M31] + z * l_mat[Matrix4.M32] + l_mat[Matrix4.M33])
        return this.set((x * l_mat[Matrix4.M00] + y * l_mat[Matrix4.M01] + z * l_mat[Matrix4.M02] + l_mat[Matrix4.M03]) * l_w, (x * l_mat[Matrix4.M10] + y * l_mat[Matrix4.M11] + z * l_mat[Matrix4.M12] + l_mat[Matrix4.M13]) * l_w, (x * l_mat[Matrix4.M20] + y * l_mat[Matrix4.M21] + z * l_mat[Matrix4.M22] + l_mat[Matrix4.M23]) * l_w)
    }

    /** Multiplies this vector by the first three columns of the matrix, essentially only applying rotation and scaling.
     *
     * @param matrix The matrix
     * @return This vector for chaining
     */
    fun rot(matrix: Matrix4): Vector3 {
        val l_mat = matrix.`val`
        return this.set(x * l_mat[Matrix4.M00] + y * l_mat[Matrix4.M01] + z * l_mat[Matrix4.M02], x * l_mat[Matrix4.M10] + y * l_mat[Matrix4.M11] + z * l_mat[Matrix4.M12], x * l_mat[Matrix4.M20] + y * l_mat[Matrix4.M21] + z * l_mat[Matrix4.M22])
    }

    /** Rotates this vector by the given angle in degrees around the given axis.
     *
     * @param degrees the angle in degrees
     * @param axisX the x-component of the axis
     * @param axisY the y-component of the axis
     * @param axisZ the z-component of the axis
     * @return This vector for chaining
     */
    fun rotate(degrees: Float, axisX: Float, axisY: Float, axisZ: Float): Vector3 {
        return this.mul(tmpMat.setToRotation(axisX, axisY, axisZ, degrees))
    }

    /** Rotates this vector by the given angle in degrees around the given axis.
     *
     * @param axis the axis
     * @param degrees the angle in degrees
     * @return This vector for chaining
     */
    fun rotate(axis: Vector3, degrees: Float): Vector3 {
        tmpMat.setToRotation(axis, degrees)
        return this.mul(tmpMat)
    }

    fun isUnit(margin: Float): Boolean {
        return Math.abs(len2() - 1f) < margin
    }

    fun isZero(margin: Float): Boolean {
        return len2() < margin
    }

    fun isOnLine(other: Vector3, epsilon: Float): Boolean {
        return len2(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x) <= epsilon
    }

    fun isOnLine(other: Vector3): Boolean {
        return len2(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x) <= MathUtils.FLOAT_ROUNDING_ERROR
    }

    fun isCollinear(other: Vector3, epsilon: Float): Boolean {
        return isOnLine(other, epsilon) && hasSameDirection(other)
    }

    fun isCollinear(other: Vector3): Boolean {
        return isOnLine(other) && hasSameDirection(other)
    }

    fun isCollinearOpposite(other: Vector3, epsilon: Float): Boolean {
        return isOnLine(other, epsilon) && hasOppositeDirection(other)
    }

    fun isCollinearOpposite(other: Vector3): Boolean {
        return isOnLine(other) && hasOppositeDirection(other)
    }

    fun isPerpendicular(vector: Vector3): Boolean {
        return MathUtils.isZero(dot(vector))
    }

    fun isPerpendicular(vector: Vector3, epsilon: Float): Boolean {
        return MathUtils.isZero(dot(vector), epsilon)
    }

    fun hasSameDirection(vector: Vector3): Boolean {
        return dot(vector) > 0
    }

    fun hasOppositeDirection(vector: Vector3): Boolean {
        return dot(vector) < 0
    }

    fun lerp(target: Vector3, alpha: Float): Vector3 {
        x += alpha * (target.x - x)
        y += alpha * (target.y - y)
        z += alpha * (target.z - z)
        return this
    }

    fun interpolate(target: Vector3, alpha: Float, interpolator: Interpolation): Vector3 {
        return lerp(target, interpolator.apply(0f, 1f, alpha))
    }

    /** Converts this `Vector3` to a string in the format `(x,y,z)`.
     * @return a string representation of this object.
     */
    override fun toString(): String {
        return "($x,$y,$z)"
    }

    fun limit(limit: Float): Vector3 {
        return limit2(limit * limit)
    }

    fun limit2(limit2: Float): Vector3 {
        val len2 = len2()
        if (len2 > limit2) {
            scl(Math.sqrt((limit2 / len2).toDouble()).toFloat())
        }
        return this
    }

    fun setLength(len: Float): Vector3 {
        return setLength2(len * len)
    }

    fun setLength2(len2: Float): Vector3 {
        val oldLen2 = len2()
        return if (oldLen2 == 0f || oldLen2 == len2) this else scl(Math.sqrt((len2 / oldLen2).toDouble()).toFloat())
    }

    fun clamp(min: Float, max: Float): Vector3 {
        val len2 = len2()
        if (len2 == 0f) return this
        val max2 = max * max
        if (len2 > max2) return scl(Math.sqrt((max2 / len2).toDouble()).toFloat())
        val min2 = min * min
        return if (len2 < min2) scl(Math.sqrt((min2 / len2).toDouble()).toFloat()) else this
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + x.toBits()
        result = prime * result + y.toBits()
        result = prime * result + z.toBits()
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null) return false
        if (javaClass != obj.javaClass) return false
        val other = obj as Vector3?
        if (x.toBits() != other!!.x.toBits()) return false
        if (y.toBits() != other.y.toBits()) return false
        return if (z.toBits() != other.z.toBits()) false else true
    }

    fun epsilonEquals(other: Vector3?, epsilon: Float): Boolean {
        if (other == null) return false
        if (Math.abs(other.x - x) > epsilon) return false
        if (Math.abs(other.y - y) > epsilon) return false
        return if (Math.abs(other.z - z) > epsilon) false else true
    }

    fun setZero(): Vector3 {
        this.x = 0f
        this.y = 0f
        this.z = 0f
        return this
    }

    companion object {
        private const val serialVersionUID = 3840054589595372522L

        val X = Vector3(1f, 0f, 0f)
        val Y = Vector3(0f, 1f, 0f)
        val Z = Vector3(0f, 0f, 1f)
        val Zero = Vector3(0f, 0f, 0f)

        private val tmpMat = Matrix4()

        /** @return The euclidean length
         */
        fun len(x: Float, y: Float, z: Float): Float {
            return Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        }

        /** @return The squared euclidean length
         */
        fun len2(x: Float, y: Float, z: Float): Float {
            return x * x + y * y + z * z
        }

        /** @return The dot product between the two vectors
         */
        fun dot(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
            return x1 * x2 + y1 * y2 + z1 * z2
        }
    }
}
/**
 * Compares this vector with the other vector using MathUtils.FLOAT_ROUNDING_ERROR for fuzzy equality testing
 *
 * @param x x component of the other vector to compare
 * @param y y component of the other vector to compare
 * @param z z component of the other vector to compare
 * @return true if vector are equal, otherwise false
 */
