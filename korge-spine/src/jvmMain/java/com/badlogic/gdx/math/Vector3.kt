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
import com.badlogic.gdx.utils.NumberUtils

/** Encapsulates a 3D vector. Allows chaining operations by returning a reference to itself in all modification methods.
 * @author badlogicgames@gmail.com
 */
class Vector3 : Serializable, Vector<Vector3> {

    /** the x-component of this vector  */
    @JvmField
    var x: Float = 0.toFloat()

    /** the y-component of this vector  */
    @JvmField
    var y: Float = 0.toFloat()

    /** the z-component of this vector  */
    @JvmField
    var z: Float = 0.toFloat()

    override val isUnit: Boolean
        get() = isUnit(0.000000001f)

    override val isZero: Boolean
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

    /** Creates a vector from the given array. The array must have at least 3 elements.
     *
     * @param values The array
     */
    constructor(values: FloatArray) {
        this[values[0], values[1]] = values[2]
    }

    /** Creates a vector from the given vector and z-component
     *
     * @param vector The vector
     * @param z The z-component
     */
    constructor(vector: Vector2, z: Float) {
        this[vector.x, vector.y] = z
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

    override fun set(vector: Vector3): Vector3 {
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

    override fun setToRandomDirection(): Vector3 {
        val u = MathUtils.random()
        val v = MathUtils.random()

        val theta = MathUtils.PI2 * u // azimuthal angle
        val phi = Math.acos((2f * v - 1f).toDouble()).toFloat() // polar angle

        return this.setFromSpherical(theta, phi)
    }

    override fun cpy(): Vector3 {
        return Vector3(this)
    }

    override fun add(vector: Vector3): Vector3 {
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

    override fun sub(a_vec: Vector3): Vector3 {
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

    /** Subtracts the given value from all components of this vector
     *
     * @param value The value
     * @return This vector for chaining
     */
    fun sub(value: Float): Vector3 {
        return this.set(this.x - value, this.y - value, this.z - value)
    }

    override fun scl(scalar: Float): Vector3 {
        return this.set(this.x * scalar, this.y * scalar, this.z * scalar)
    }

    override fun scl(other: Vector3): Vector3 {
        return this.set(x * other.x, y * other.y, z * other.z)
    }

    /** Scales this vector by the given values
     * @param vx X value
     * @param vy Y value
     * @param vz Z value
     * @return This vector for chaining
     */
    fun scl(vx: Float, vy: Float, vz: Float): Vector3 {
        return this.set(this.x * vx, this.y * vy, this.z * vz)
    }

    override fun mulAdd(vec: Vector3, scalar: Float): Vector3 {
        this.x += vec.x * scalar
        this.y += vec.y * scalar
        this.z += vec.z * scalar
        return this
    }

    override fun mulAdd(vec: Vector3, mulVec: Vector3): Vector3 {
        this.x += vec.x * mulVec.x
        this.y += vec.y * mulVec.y
        this.z += vec.z * mulVec.z
        return this
    }

    override fun len(): Float {
        return Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
    }

    override fun len2(): Float {
        return x * x + y * y + z * z
    }

    /** @param vector The other vector
     * @return Whether this and the other vector are equal
     */
    fun idt(vector: Vector3): Boolean {
        return x == vector.x && y == vector.y && z == vector.z
    }

    override fun dst(vector: Vector3): Float {
        val a = vector.x - x
        val b = vector.y - y
        val c = vector.z - z
        return Math.sqrt((a * a + b * b + c * c).toDouble()).toFloat()
    }

    /** @return the distance between this point and the given point
     */
    fun dst(x: Float, y: Float, z: Float): Float {
        val a = x - this.x
        val b = y - this.y
        val c = z - this.z
        return Math.sqrt((a * a + b * b + c * c).toDouble()).toFloat()
    }

    override fun dst2(point: Vector3): Float {
        val a = point.x - x
        val b = point.y - y
        val c = point.z - z
        return a * a + b * b + c * c
    }

    /** Returns the squared distance between this point and the given point
     * @param x The x-component of the other point
     * @param y The y-component of the other point
     * @param z The z-component of the other point
     * @return The squared distance
     */
    fun dst2(x: Float, y: Float, z: Float): Float {
        val a = x - this.x
        val b = y - this.y
        val c = z - this.z
        return a * a + b * b + c * c
    }

    override fun nor(): Vector3 {
        val len2 = this.len2()
        return if (len2 == 0f || len2 == 1f) this else this.scl(1f / Math.sqrt(len2.toDouble()).toFloat())
    }

    override fun dot(vector: Vector3): Float {
        return x * vector.x + y * vector.y + z * vector.z
    }

    /** Returns the dot product between this and the given vector.
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @param z The z-component of the other vector
     * @return The dot product
     */
    fun dot(x: Float, y: Float, z: Float): Float {
        return this.x * x + this.y * y + this.z * z
    }

    /** Sets this vector to the cross product between it and the other vector.
     * @param vector The other vector
     * @return This vector for chaining
     */
    fun crs(vector: Vector3): Vector3 {
        return this.set(y * vector.z - z * vector.y, z * vector.x - x * vector.z, x * vector.y - y * vector.x)
    }

    /** Sets this vector to the cross product between it and the other vector.
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @param z The z-component of the other vector
     * @return This vector for chaining
     */
    fun crs(x: Float, y: Float, z: Float): Vector3 {
        return this.set(this.y * z - this.z * y, this.z * x - this.x * z, this.x * y - this.y * x)
    }

    /** Left-multiplies the vector by the given 4x3 column major matrix. The matrix should be composed by a 3x3 matrix representing
     * rotation and scale plus a 1x3 matrix representing the translation.
     * @param matrix The matrix
     * @return This vector for chaining
     */
    fun mul4x3(matrix: FloatArray): Vector3 {
        return set(x * matrix[0] + y * matrix[3] + z * matrix[6] + matrix[9], x * matrix[1] + y * matrix[4] + z * matrix[7]
                + matrix[10], x * matrix[2] + y * matrix[5] + z * matrix[8] + matrix[11])
    }

    /** Left-multiplies the vector by the given matrix, assuming the fourth (w) component of the vector is 1.
     * @param matrix The matrix
     * @return This vector for chaining
     */
    fun mul(matrix: Matrix4): Vector3 {
        val l_mat = matrix.`val`
        return this.set(x * l_mat[Matrix4.M00] + y * l_mat[Matrix4.M01] + z * l_mat[Matrix4.M02] + l_mat[Matrix4.M03], x * l_mat[Matrix4.M10] + y * l_mat[Matrix4.M11] + z * l_mat[Matrix4.M12] + l_mat[Matrix4.M13], x * l_mat[Matrix4.M20] + y * l_mat[Matrix4.M21] + z * l_mat[Matrix4.M22] + l_mat[Matrix4.M23])
    }

    /** Multiplies the vector by the transpose of the given matrix, assuming the fourth (w) component of the vector is 1.
     * @param matrix The matrix
     * @return This vector for chaining
     */
    fun traMul(matrix: Matrix4): Vector3 {
        val l_mat = matrix.`val`
        return this.set(x * l_mat[Matrix4.M00] + y * l_mat[Matrix4.M10] + z * l_mat[Matrix4.M20] + l_mat[Matrix4.M30], x * l_mat[Matrix4.M01] + y * l_mat[Matrix4.M11] + z * l_mat[Matrix4.M21] + l_mat[Matrix4.M31], x * l_mat[Matrix4.M02] + y * l_mat[Matrix4.M12] + z * l_mat[Matrix4.M22] + l_mat[Matrix4.M32])
    }

    /** Left-multiplies the vector by the given matrix.
     * @param matrix The matrix
     * @return This vector for chaining
     */
    fun mul(matrix: Matrix3): Vector3 {
        val l_mat = matrix.`val`
        return set(x * l_mat[Matrix3.M00] + y * l_mat[Matrix3.M01] + z * l_mat[Matrix3.M02], x * l_mat[Matrix3.M10] + y * l_mat[Matrix3.M11] + z * l_mat[Matrix3.M12], x * l_mat[Matrix3.M20] + y * l_mat[Matrix3.M21] + z * l_mat[Matrix3.M22])
    }

    /** Multiplies the vector by the transpose of the given matrix.
     * @param matrix The matrix
     * @return This vector for chaining
     */
    fun traMul(matrix: Matrix3): Vector3 {
        val l_mat = matrix.`val`
        return set(x * l_mat[Matrix3.M00] + y * l_mat[Matrix3.M10] + z * l_mat[Matrix3.M20], x * l_mat[Matrix3.M01] + y * l_mat[Matrix3.M11] + z * l_mat[Matrix3.M21], x * l_mat[Matrix3.M02] + y * l_mat[Matrix3.M12] + z * l_mat[Matrix3.M22])
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

    /** Multiplies this vector by the transpose of the first three columns of the matrix. Note: only works for translation and
     * rotation, does not work for scaling. For those, use [.rot] with [Matrix4.inv].
     * @param matrix The transformation matrix
     * @return The vector for chaining
     */
    fun unrotate(matrix: Matrix4): Vector3 {
        val l_mat = matrix.`val`
        return this.set(x * l_mat[Matrix4.M00] + y * l_mat[Matrix4.M10] + z * l_mat[Matrix4.M20], x * l_mat[Matrix4.M01] + y * l_mat[Matrix4.M11] + z * l_mat[Matrix4.M21], x * l_mat[Matrix4.M02] + y * l_mat[Matrix4.M12] + z * l_mat[Matrix4.M22])
    }

    /** Translates this vector in the direction opposite to the translation of the matrix and the multiplies this vector by the
     * transpose of the first three columns of the matrix. Note: only works for translation and rotation, does not work for
     * scaling. For those, use [.mul] with [Matrix4.inv].
     * @param matrix The transformation matrix
     * @return The vector for chaining
     */
    fun untransform(matrix: Matrix4): Vector3 {
        val l_mat = matrix.`val`
        x -= l_mat[Matrix4.M03]
        y -= l_mat[Matrix4.M03]
        z -= l_mat[Matrix4.M03]
        return this.set(x * l_mat[Matrix4.M00] + y * l_mat[Matrix4.M10] + z * l_mat[Matrix4.M20], x * l_mat[Matrix4.M01] + y * l_mat[Matrix4.M11] + z * l_mat[Matrix4.M21], x * l_mat[Matrix4.M02] + y * l_mat[Matrix4.M12] + z * l_mat[Matrix4.M22])
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

    /** Rotates this vector by the given angle in radians around the given axis.
     *
     * @param radians the angle in radians
     * @param axisX the x-component of the axis
     * @param axisY the y-component of the axis
     * @param axisZ the z-component of the axis
     * @return This vector for chaining
     */
    fun rotateRad(radians: Float, axisX: Float, axisY: Float, axisZ: Float): Vector3 {
        return this.mul(tmpMat.setToRotationRad(axisX, axisY, axisZ, radians))
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

    /** Rotates this vector by the given angle in radians around the given axis.
     *
     * @param axis the axis
     * @param radians the angle in radians
     * @return This vector for chaining
     */
    fun rotateRad(axis: Vector3, radians: Float): Vector3 {
        tmpMat.setToRotationRad(axis, radians)
        return this.mul(tmpMat)
    }

    override fun isUnit(margin: Float): Boolean {
        return Math.abs(len2() - 1f) < margin
    }

    override fun isZero(margin: Float): Boolean {
        return len2() < margin
    }

    override fun isOnLine(other: Vector3, epsilon: Float): Boolean {
        return len2(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x) <= epsilon
    }

    override fun isOnLine(other: Vector3): Boolean {
        return len2(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x) <= MathUtils.FLOAT_ROUNDING_ERROR
    }

    override fun isCollinear(other: Vector3, epsilon: Float): Boolean {
        return isOnLine(other, epsilon) && hasSameDirection(other)
    }

    override fun isCollinear(other: Vector3): Boolean {
        return isOnLine(other) && hasSameDirection(other)
    }

    override fun isCollinearOpposite(other: Vector3, epsilon: Float): Boolean {
        return isOnLine(other, epsilon) && hasOppositeDirection(other)
    }

    override fun isCollinearOpposite(other: Vector3): Boolean {
        return isOnLine(other) && hasOppositeDirection(other)
    }

    override fun isPerpendicular(vector: Vector3): Boolean {
        return MathUtils.isZero(dot(vector))
    }

    override fun isPerpendicular(vector: Vector3, epsilon: Float): Boolean {
        return MathUtils.isZero(dot(vector), epsilon)
    }

    override fun hasSameDirection(vector: Vector3): Boolean {
        return dot(vector) > 0
    }

    override fun hasOppositeDirection(vector: Vector3): Boolean {
        return dot(vector) < 0
    }

    override fun lerp(target: Vector3, alpha: Float): Vector3 {
        x += alpha * (target.x - x)
        y += alpha * (target.y - y)
        z += alpha * (target.z - z)
        return this
    }

    override fun interpolate(target: Vector3, alpha: Float, interpolator: Interpolation): Vector3 {
        return lerp(target, interpolator.apply(0f, 1f, alpha))
    }

    /** Spherically interpolates between this vector and the target vector by alpha which is in the range [0,1]. The result is
     * stored in this vector.
     *
     * @param target The target vector
     * @param alpha The interpolation coefficient
     * @return This vector for chaining.
     */
    fun slerp(target: Vector3, alpha: Float): Vector3 {
        val dot = dot(target)
        // If the inputs are too close for comfort, simply linearly interpolate.
        if (dot > 0.9995 || dot < -0.9995) return lerp(target, alpha)

        // theta0 = angle between input vectors
        val theta0 = Math.acos(dot.toDouble()).toFloat()
        // theta = angle between this vector and result
        val theta = theta0 * alpha

        val st = Math.sin(theta.toDouble()).toFloat()
        val tx = target.x - x * dot
        val ty = target.y - y * dot
        val tz = target.z - z * dot
        val l2 = tx * tx + ty * ty + tz * tz
        val dl = st * if (l2 < 0.0001f) 1f else 1f / Math.sqrt(l2.toDouble()).toFloat()

        return scl(Math.cos(theta.toDouble()).toFloat()).add(tx * dl, ty * dl, tz * dl).nor()
    }

    /** Converts this `Vector3` to a string in the format `(x,y,z)`.
     * @return a string representation of this object.
     */
    override fun toString(): String {
        return "($x,$y,$z)"
    }

    /** Sets this `Vector3` to the value represented by the specified string according to the format of [.toString].
     * @param v the string.
     * @return this vector for chaining
     */
    fun fromString(v: String): Vector3 {
        val s0 = v.indexOf(',', 1)
        val s1 = v.indexOf(',', s0 + 1)
        if (s0 != -1 && s1 != -1 && v[0] == '(' && v[v.length - 1] == ')') {
            try {
                val x = java.lang.Float.parseFloat(v.substring(1, s0))
                val y = java.lang.Float.parseFloat(v.substring(s0 + 1, s1))
                val z = java.lang.Float.parseFloat(v.substring(s1 + 1, v.length - 1))
                return this.set(x, y, z)
            } catch (ex: NumberFormatException) {
                // Throw a GdxRuntimeException
            }

        }
        throw GdxRuntimeException("Malformed Vector3: $v")
    }

    override fun limit(limit: Float): Vector3 {
        return limit2(limit * limit)
    }

    override fun limit2(limit2: Float): Vector3 {
        val len2 = len2()
        if (len2 > limit2) {
            scl(Math.sqrt((limit2 / len2).toDouble()).toFloat())
        }
        return this
    }

    override fun setLength(len: Float): Vector3 {
        return setLength2(len * len)
    }

    override fun setLength2(len2: Float): Vector3 {
        val oldLen2 = len2()
        return if (oldLen2 == 0f || oldLen2 == len2) this else scl(Math.sqrt((len2 / oldLen2).toDouble()).toFloat())
    }

    override fun clamp(min: Float, max: Float): Vector3 {
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
        result = prime * result + NumberUtils.floatToIntBits(x)
        result = prime * result + NumberUtils.floatToIntBits(y)
        result = prime * result + NumberUtils.floatToIntBits(z)
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null) return false
        if (javaClass != obj.javaClass) return false
        val other = obj as Vector3?
        if (NumberUtils.floatToIntBits(x) != NumberUtils.floatToIntBits(other!!.x)) return false
        if (NumberUtils.floatToIntBits(y) != NumberUtils.floatToIntBits(other.y)) return false
        return if (NumberUtils.floatToIntBits(z) != NumberUtils.floatToIntBits(other.z)) false else true
    }

    override fun epsilonEquals(other: Vector3?, epsilon: Float): Boolean {
        if (other == null) return false
        if (Math.abs(other.x - x) > epsilon) return false
        if (Math.abs(other.y - y) > epsilon) return false
        return if (Math.abs(other.z - z) > epsilon) false else true
    }

    /** Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
     * @return whether the vectors are the same.
     */
    @JvmOverloads
    fun epsilonEquals(x: Float, y: Float, z: Float, epsilon: Float = MathUtils.FLOAT_ROUNDING_ERROR): Boolean {
        if (Math.abs(x - this.x) > epsilon) return false
        if (Math.abs(y - this.y) > epsilon) return false
        return if (Math.abs(z - this.z) > epsilon) false else true
    }

    /**
     * Compares this vector with the other vector using MathUtils.FLOAT_ROUNDING_ERROR for fuzzy equality testing
     *
     * @param other other vector to compare
     * @return true if vector are equal, otherwise false
     */
    fun epsilonEquals(other: Vector3): Boolean {
        return epsilonEquals(other, MathUtils.FLOAT_ROUNDING_ERROR)
    }

    override fun setZero(): Vector3 {
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

        /** @return The euclidean distance between the two specified vectors
         */
        fun dst(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
            val a = x2 - x1
            val b = y2 - y1
            val c = z2 - z1
            return Math.sqrt((a * a + b * b + c * c).toDouble()).toFloat()
        }

        /** @return the squared distance between the given points
         */
        fun dst2(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
            val a = x2 - x1
            val b = y2 - y1
            val c = z2 - z1
            return a * a + b * b + c * c
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
