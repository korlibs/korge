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

/** A simple quaternion class.
 * @see [http://en.wikipedia.org/wiki/Quaternion](http://en.wikipedia.org/wiki/Quaternion)
 *
 * @author badlogicgames@gmail.com
 * @author vesuvio
 * @author xoppa
 */
data class Quaternion(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var w: Float = 1f
) {


    /** Get the angle in radians of the rotation this quaternion represents. Does not normalize the quaternion. Use
     * [.getAxisAngleRad] to get both the axis and the angle of this rotation. Use
     * [.getAngleAroundRad] to get the angle around a specific axis.
     * @return the angle in radians of the rotation
     */
    val angleRad: Float
        get() = (2.0 * kotlin.math.acos((if (this.w > 1) this.w / len() else this.w).toDouble())).toFloat()

    /** Get the angle in degrees of the rotation this quaternion represents. Use [.getAxisAngle] to get both the axis
     * and the angle of this rotation. Use [.getAngleAround] to get the angle around a specific axis.
     * @return the angle in degrees of the rotation
     */
    val angle: Float
        get() = angleRad * MathUtils.radiansToDegrees

    /** Sets the components of the quaternion
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     * @param w The w-component
     * @return This quaternion for chaining
     */
    operator fun set(x: Float, y: Float, z: Float, w: Float): Quaternion {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        return this
    }

    /** Sets the quaternion components from the given quaternion.
     * @param quaternion The quaternion.
     * @return This quaternion for chaining.
     */
    fun set(quaternion: Quaternion): Quaternion {
        return this.set(quaternion.x, quaternion.y, quaternion.z, quaternion.w)
    }

    /** Sets the quaternion components from the given axis and angle around that axis.
     *
     * @param axis The axis
     * @param angle The angle in degrees
     * @return This quaternion for chaining.
     */
    operator fun set(axis: Vector3, angle: Float): Quaternion {
        return setFromAxis(axis.x, axis.y, axis.z, angle)
    }

    /** @return the euclidean length of this quaternion
     */
    fun len(): Float {
        return kotlin.math.sqrt((x * x + y * y + z * z + w * w).toDouble()).toFloat()
    }

    override fun toString(): String {
        return "[$x|$y|$z|$w]"
    }

    /** @return the length of this quaternion without square root
     */
    fun len2(): Float {
        return x * x + y * y + z * z + w * w
    }

    /** Normalizes this quaternion to unit length
     * @return the quaternion for chaining
     */
    fun nor(): Quaternion {
        var len = len2()
        if (len != 0f && !isEqual(len, 1f)) {
            len = kotlin.math.sqrt(len.toDouble()).toFloat()
            w /= len
            x /= len
            y /= len
            z /= len
        }
        return this
    }

    private fun isEqual(a: Float, b: Float): Boolean {
        return kotlin.math.abs(a - b) <= MathUtils.FLOAT_ROUNDING_ERROR
    }


    /** Conjugate the quaternion.
     *
     * @return This quaternion for chaining
     */
    fun conjugate(): Quaternion {
        x = -x
        y = -y
        z = -z
        return this
    }

    // TODO : this would better fit into the vector3 class
    /** Transforms the given vector using this quaternion
     *
     * @param v Vector to transform
     */
    fun transform(v: Vector3): Vector3 {
        tmp2.set(this)
        tmp2.conjugate()
        tmp2.mulLeft(tmp1.set(v.x, v.y, v.z, 0f)).mulLeft(this)

        v.x = tmp2.x
        v.y = tmp2.y
        v.z = tmp2.z
        return v
    }

    /** Multiplies this quaternion with another one in the form of this = other * this
     *
     * @param other Quaternion to multiply with
     * @return This quaternion for chaining
     */
    fun mulLeft(other: Quaternion): Quaternion {
        val newX = other.w * this.x + other.x * this.w + other.y * this.z - other.z * this.y
        val newY = other.w * this.y + other.y * this.w + other.z * this.x - other.x * this.z
        val newZ = other.w * this.z + other.z * this.w + other.x * this.y - other.y * this.x
        val newW = other.w * this.w - other.x * this.x - other.y * this.y - other.z * this.z
        this.x = newX
        this.y = newY
        this.z = newZ
        this.w = newW
        return this
    }

    /** Add the x,y,z,w components of the passed in quaternion to the ones of this quaternion  */
    fun add(quaternion: Quaternion): Quaternion {
        this.x += quaternion.x
        this.y += quaternion.y
        this.z += quaternion.z
        this.w += quaternion.w
        return this
    }

    /** Add the x,y,z,w components of the passed in quaternion to the ones of this quaternion  */
    fun add(qx: Float, qy: Float, qz: Float, qw: Float): Quaternion {
        this.x += qx
        this.y += qy
        this.z += qz
        this.w += qw
        return this
    }

    // TODO : the matrix4 set(quaternion) doesnt set the last row+col of the matrix to 0,0,0,1 so... that's why there is this
    // method
    /** Fills a 4x4 matrix with the rotation matrix represented by this quaternion.
     *
     * @param matrix Matrix to fill
     */
    fun toMatrix(matrix: FloatArray) {
        val xx = x * x
        val xy = x * y
        val xz = x * z
        val xw = x * w
        val yy = y * y
        val yz = y * z
        val yw = y * w
        val zz = z * z
        val zw = z * w
        // Set matrix from quaternion
        matrix[Matrix4.M00] = 1 - 2 * (yy + zz)
        matrix[Matrix4.M01] = 2 * (xy - zw)
        matrix[Matrix4.M02] = 2 * (xz + yw)
        matrix[Matrix4.M03] = 0f
        matrix[Matrix4.M10] = 2 * (xy + zw)
        matrix[Matrix4.M11] = 1 - 2 * (xx + zz)
        matrix[Matrix4.M12] = 2 * (yz - xw)
        matrix[Matrix4.M13] = 0f
        matrix[Matrix4.M20] = 2 * (xz - yw)
        matrix[Matrix4.M21] = 2 * (yz + xw)
        matrix[Matrix4.M22] = 1 - 2 * (xx + yy)
        matrix[Matrix4.M23] = 0f
        matrix[Matrix4.M30] = 0f
        matrix[Matrix4.M31] = 0f
        matrix[Matrix4.M32] = 0f
        matrix[Matrix4.M33] = 1f
    }

    /** Sets the quaternion to an identity Quaternion
     * @return this quaternion for chaining
     */
    fun idt(): Quaternion = this.set(0f, 0f, 0f, 1f)

    // todo : the setFromAxis(v3,float) method should replace the set(v3,float) method

    /** Sets the quaternion components from the given axis and angle around that axis.
     * @param x X direction of the axis
     * @param y Y direction of the axis
     * @param z Z direction of the axis
     * @param degrees The angle in degrees
     * @return This quaternion for chaining.
     */
    fun setFromAxis(x: Float, y: Float, z: Float, degrees: Float): Quaternion {
        return setFromAxisRad(x, y, z, degrees * MathUtils.degreesToRadians)
    }

    /** Sets the quaternion components from the given axis and angle around that axis.
     * @param x X direction of the axis
     * @param y Y direction of the axis
     * @param z Z direction of the axis
     * @param radians The angle in radians
     * @return This quaternion for chaining.
     */
    fun setFromAxisRad(x: Float, y: Float, z: Float, radians: Float): Quaternion {
        var d = Vector3.len(x, y, z)
        if (d == 0f) return idt()
        d = 1f / d
        val l_ang = if (radians < 0) MathUtils.PI2 - -radians % MathUtils.PI2 else radians % MathUtils.PI2
        val l_sin = kotlin.math.sin((l_ang / 2).toDouble()).toFloat()
        val l_cos = kotlin.math.cos((l_ang / 2).toDouble()).toFloat()
        return this.set(d * x * l_sin, d * y * l_sin, d * z * l_sin, l_cos).nor()
    }

    companion object {
        private val tmp1 = Quaternion(0f, 0f, 0f, 0f)
        private val tmp2 = Quaternion(0f, 0f, 0f, 0f)

    }
}
