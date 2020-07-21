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

/** Encapsulates a [column major](http://en.wikipedia.org/wiki/Row-major_order#Column-major_order) 4 by 4 matrix. Like
 * the [Vector3] class it allows the chaining of methods by returning a reference to itself. For example:
 *
 * <pre>
 * Matrix4 mat = new Matrix4().trn(position).mul(camera.combined);
</pre> *
 *
 * @author badlogicgames@gmail.com
 */
class Matrix4 : Serializable {
    @JvmField
    val `val` = FloatArray(16)

    /** @return the backing float array
     */
    val values = `val`

    /** @return the squared scale factor on the X axis
     */
    val scaleXSquared: Float
        get() = values[Matrix4.M00] * values[Matrix4.M00] + values[Matrix4.M01] * values[Matrix4.M01] + values[Matrix4.M02] * values[Matrix4.M02]

    /** @return the squared scale factor on the Y axis
     */
    val scaleYSquared: Float
        get() = values[Matrix4.M10] * values[Matrix4.M10] + values[Matrix4.M11] * values[Matrix4.M11] + values[Matrix4.M12] * values[Matrix4.M12]

    /** @return the squared scale factor on the Z axis
     */
    val scaleZSquared: Float
        get() = values[Matrix4.M20] * values[Matrix4.M20] + values[Matrix4.M21] * values[Matrix4.M21] + values[Matrix4.M22] * values[Matrix4.M22]

    /** @return the scale factor on the X axis (non-negative)
     */
    val scaleX: Float
        get() = if (MathUtils.isZero(values[Matrix4.M01]) && MathUtils.isZero(values[Matrix4.M02]))
            Math.abs(values[Matrix4.M00])
        else
            Math.sqrt(scaleXSquared.toDouble()).toFloat()

    /** @return the scale factor on the Y axis (non-negative)
     */
    val scaleY: Float
        get() = if (MathUtils.isZero(values[Matrix4.M10]) && MathUtils.isZero(values[Matrix4.M12]))
            Math.abs(values[Matrix4.M11])
        else
            Math.sqrt(scaleYSquared.toDouble()).toFloat()

    /** @return the scale factor on the X axis (non-negative)
     */
    val scaleZ: Float
        get() = if (MathUtils.isZero(values[Matrix4.M20]) && MathUtils.isZero(values[Matrix4.M21]))
            Math.abs(values[Matrix4.M22])
        else
            Math.sqrt(scaleZSquared.toDouble()).toFloat()

    /** Constructs an identity matrix  */
    constructor() {
        values[M00] = 1f
        values[M11] = 1f
        values[M22] = 1f
        values[M33] = 1f
    }

    /** Constructs a matrix from the given matrix.
     *
     * @param matrix The matrix to copy. (This matrix is not modified)
     */
    constructor(matrix: Matrix4) {
        this.set(matrix)
    }

    /** Constructs a matrix from the given float array. The array must have at least 16 elements; the first 16 will be copied.
     * @param values The float array to copy. Remember that this matrix is in [column major](http://en.wikipedia.org/wiki/Row-major_order) order. (The float array is not modified)
     */
    constructor(values: FloatArray) {
        this.set(values)
    }

    /** Constructs a rotation matrix from the given [Quaternion].
     * @param quaternion The quaternion to be copied. (The quaternion is not modified)
     */
    constructor(quaternion: Quaternion) {
        this.set(quaternion)
    }

    /** Construct a matrix from the given translation, rotation and scale.
     * @param position The translation
     * @param rotation The rotation, must be normalized
     * @param scale The scale
     */
    constructor(position: Vector3, rotation: Quaternion, scale: Vector3) {
        set(position, rotation, scale)
    }

    /** Sets the matrix to the given matrix.
     *
     * @param matrix The matrix that is to be copied. (The given matrix is not modified)
     * @return This matrix for the purpose of chaining methods together.
     */
    fun set(matrix: Matrix4): Matrix4 {
        return this.set(matrix.values)
    }

    /** Sets the matrix to the given matrix as a float array. The float array must have at least 16 elements; the first 16 will be
     * copied.
     *
     * @param values The matrix, in float form, that is to be copied. Remember that this matrix is in [column major](http://en.wikipedia.org/wiki/Row-major_order) order.
     * @return This matrix for the purpose of chaining methods together.
     */
    fun set(values: FloatArray): Matrix4 {
        System.arraycopy(values, 0, this.values, 0, this.values.size)
        return this
    }

    /** Sets the matrix to a rotation matrix representing the quaternion.
     *
     * @param quaternion The quaternion that is to be used to set this matrix.
     * @return This matrix for the purpose of chaining methods together.
     */
    fun set(quaternion: Quaternion): Matrix4 {
        return set(quaternion.x, quaternion.y, quaternion.z, quaternion.w)
    }

    /** Sets the matrix to a rotation matrix representing the quaternion.
     *
     * @param quaternionX The X component of the quaternion that is to be used to set this matrix.
     * @param quaternionY The Y component of the quaternion that is to be used to set this matrix.
     * @param quaternionZ The Z component of the quaternion that is to be used to set this matrix.
     * @param quaternionW The W component of the quaternion that is to be used to set this matrix.
     * @return This matrix for the purpose of chaining methods together.
     */
    operator fun set(quaternionX: Float, quaternionY: Float, quaternionZ: Float, quaternionW: Float): Matrix4 {
        return set(0f, 0f, 0f, quaternionX, quaternionY, quaternionZ, quaternionW)
    }

    /** Set this matrix to the specified translation and rotation.
     * @param position The translation
     * @param orientation The rotation, must be normalized
     * @return This matrix for chaining
     */
    operator fun set(position: Vector3, orientation: Quaternion): Matrix4 {
        return set(position.x, position.y, position.z, orientation.x, orientation.y, orientation.z, orientation.w)
    }

    /** Sets the matrix to a rotation matrix representing the translation and quaternion.
     *
     * @param translationX The X component of the translation that is to be used to set this matrix.
     * @param translationY The Y component of the translation that is to be used to set this matrix.
     * @param translationZ The Z component of the translation that is to be used to set this matrix.
     * @param quaternionX The X component of the quaternion that is to be used to set this matrix.
     * @param quaternionY The Y component of the quaternion that is to be used to set this matrix.
     * @param quaternionZ The Z component of the quaternion that is to be used to set this matrix.
     * @param quaternionW The W component of the quaternion that is to be used to set this matrix.
     * @return This matrix for the purpose of chaining methods together.
     */
    operator fun set(translationX: Float, translationY: Float, translationZ: Float, quaternionX: Float, quaternionY: Float,
                     quaternionZ: Float, quaternionW: Float): Matrix4 {
        val xs = quaternionX * 2f
        val ys = quaternionY * 2f
        val zs = quaternionZ * 2f
        val wx = quaternionW * xs
        val wy = quaternionW * ys
        val wz = quaternionW * zs
        val xx = quaternionX * xs
        val xy = quaternionX * ys
        val xz = quaternionX * zs
        val yy = quaternionY * ys
        val yz = quaternionY * zs
        val zz = quaternionZ * zs

        values[M00] = 1.0f - (yy + zz)
        values[M01] = xy - wz
        values[M02] = xz + wy
        values[M03] = translationX

        values[M10] = xy + wz
        values[M11] = 1.0f - (xx + zz)
        values[M12] = yz - wx
        values[M13] = translationY

        values[M20] = xz - wy
        values[M21] = yz + wx
        values[M22] = 1.0f - (xx + yy)
        values[M23] = translationZ

        values[M30] = 0f
        values[M31] = 0f
        values[M32] = 0f
        values[M33] = 1.0f
        return this
    }

    /** Set this matrix to the specified translation, rotation and scale.
     * @param position The translation
     * @param orientation The rotation, must be normalized
     * @param scale The scale
     * @return This matrix for chaining
     */
    operator fun set(position: Vector3, orientation: Quaternion, scale: Vector3): Matrix4 {
        return set(position.x, position.y, position.z, orientation.x, orientation.y, orientation.z, orientation.w, scale.x,
                scale.y, scale.z)
    }

    /** Sets the matrix to a rotation matrix representing the translation and quaternion.
     *
     * @param translationX The X component of the translation that is to be used to set this matrix.
     * @param translationY The Y component of the translation that is to be used to set this matrix.
     * @param translationZ The Z component of the translation that is to be used to set this matrix.
     * @param quaternionX The X component of the quaternion that is to be used to set this matrix.
     * @param quaternionY The Y component of the quaternion that is to be used to set this matrix.
     * @param quaternionZ The Z component of the quaternion that is to be used to set this matrix.
     * @param quaternionW The W component of the quaternion that is to be used to set this matrix.
     * @param scaleX The X component of the scaling that is to be used to set this matrix.
     * @param scaleY The Y component of the scaling that is to be used to set this matrix.
     * @param scaleZ The Z component of the scaling that is to be used to set this matrix.
     * @return This matrix for the purpose of chaining methods together.
     */
    operator fun set(translationX: Float, translationY: Float, translationZ: Float, quaternionX: Float, quaternionY: Float,
                     quaternionZ: Float, quaternionW: Float, scaleX: Float, scaleY: Float, scaleZ: Float): Matrix4 {
        val xs = quaternionX * 2f
        val ys = quaternionY * 2f
        val zs = quaternionZ * 2f
        val wx = quaternionW * xs
        val wy = quaternionW * ys
        val wz = quaternionW * zs
        val xx = quaternionX * xs
        val xy = quaternionX * ys
        val xz = quaternionX * zs
        val yy = quaternionY * ys
        val yz = quaternionY * zs
        val zz = quaternionZ * zs

        values[M00] = scaleX * (1.0f - (yy + zz))
        values[M01] = scaleY * (xy - wz)
        values[M02] = scaleZ * (xz + wy)
        values[M03] = translationX

        values[M10] = scaleX * (xy + wz)
        values[M11] = scaleY * (1.0f - (xx + zz))
        values[M12] = scaleZ * (yz - wx)
        values[M13] = translationY

        values[M20] = scaleX * (xz - wy)
        values[M21] = scaleY * (yz + wx)
        values[M22] = scaleZ * (1.0f - (xx + yy))
        values[M23] = translationZ

        values[M30] = 0f
        values[M31] = 0f
        values[M32] = 0f
        values[M33] = 1.0f
        return this
    }

    /** Sets the four columns of the matrix which correspond to the x-, y- and z-axis of the vector space this matrix creates as
     * well as the 4th column representing the translation of any point that is multiplied by this matrix.
     *
     * @param xAxis The x-axis.
     * @param yAxis The y-axis.
     * @param zAxis The z-axis.
     * @param pos The translation vector.
     */
    operator fun set(xAxis: Vector3, yAxis: Vector3, zAxis: Vector3, pos: Vector3): Matrix4 {
        values[M00] = xAxis.x
        values[M01] = xAxis.y
        values[M02] = xAxis.z
        values[M10] = yAxis.x
        values[M11] = yAxis.y
        values[M12] = yAxis.z
        values[M20] = zAxis.x
        values[M21] = zAxis.y
        values[M22] = zAxis.z
        values[M03] = pos.x
        values[M13] = pos.y
        values[M23] = pos.z
        values[M30] = 0f
        values[M31] = 0f
        values[M32] = 0f
        values[M33] = 1f
        return this
    }

    /** @return a copy of this matrix
     */
    fun cpy(): Matrix4 {
        return Matrix4(this)
    }

    /** Adds a translational component to the matrix in the 4th column. The other columns are untouched.
     *
     * @param vector The translation vector to add to the current matrix. (This vector is not modified)
     * @return This matrix for the purpose of chaining methods together.
     */
    fun trn(vector: Vector3): Matrix4 {
        values[M03] += vector.x
        values[M13] += vector.y
        values[M23] += vector.z
        return this
    }

    /** Adds a translational component to the matrix in the 4th column. The other columns are untouched.
     *
     * @param x The x-component of the translation vector.
     * @param y The y-component of the translation vector.
     * @param z The z-component of the translation vector.
     * @return This matrix for the purpose of chaining methods together.
     */
    fun trn(x: Float, y: Float, z: Float): Matrix4 {
        values[M03] += x
        values[M13] += y
        values[M23] += z
        return this
    }

    /** Postmultiplies this matrix with the given matrix, storing the result in this matrix. For example:
     *
     * <pre>
     * A.mul(B) results in A := AB.
    </pre> *
     *
     * @param matrix The other matrix to multiply by.
     * @return This matrix for the purpose of chaining operations together.
     */
    fun mul(matrix: Matrix4): Matrix4 {
        mul(values, matrix.values)
        return this
    }

    /** Premultiplies this matrix with the given matrix, storing the result in this matrix. For example:
     *
     * <pre>
     * A.mulLeft(B) results in A := BA.
    </pre> *
     *
     * @param matrix The other matrix to multiply by.
     * @return This matrix for the purpose of chaining operations together.
     */
    fun mulLeft(matrix: Matrix4): Matrix4 {
        tmpMat.set(matrix)
        mul(tmpMat.values, this.values)
        return set(tmpMat)
    }

    /** Transposes the matrix.
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    fun tra(): Matrix4 {
        tmp[M00] = values[M00]
        tmp[M01] = values[M10]
        tmp[M02] = values[M20]
        tmp[M03] = values[M30]
        tmp[M10] = values[M01]
        tmp[M11] = values[M11]
        tmp[M12] = values[M21]
        tmp[M13] = values[M31]
        tmp[M20] = values[M02]
        tmp[M21] = values[M12]
        tmp[M22] = values[M22]
        tmp[M23] = values[M32]
        tmp[M30] = values[M03]
        tmp[M31] = values[M13]
        tmp[M32] = values[M23]
        tmp[M33] = values[M33]
        return set(tmp)
    }

    /** Sets the matrix to an identity matrix.
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    fun idt(): Matrix4 {
        values[M00] = 1f
        values[M01] = 0f
        values[M02] = 0f
        values[M03] = 0f
        values[M10] = 0f
        values[M11] = 1f
        values[M12] = 0f
        values[M13] = 0f
        values[M20] = 0f
        values[M21] = 0f
        values[M22] = 1f
        values[M23] = 0f
        values[M30] = 0f
        values[M31] = 0f
        values[M32] = 0f
        values[M33] = 1f
        return this
    }

    /** Inverts the matrix. Stores the result in this matrix.
     *
     * @return This matrix for the purpose of chaining methods together.
     * @throws RuntimeException if the matrix is singular (not invertible)
     */
    fun inv(): Matrix4 {
        val l_det = ((values[M30] * values[M21] * values[M12] * values[M03] - values[M20] * values[M31] * values[M12] * values[M03] - (values[M30] * values[M11]
                * values[M22] * values[M03]) + values[M10] * values[M31] * values[M22] * values[M03] + values[M20] * values[M11] * values[M32] * values[M03] - (values[M10]
                * values[M21] * values[M32] * values[M03]) - values[M30] * values[M21] * values[M02] * values[M13] + values[M20] * values[M31] * values[M02] * values[M13]
                + values[M30] * values[M01] * values[M22] * values[M13]) - values[M00] * values[M31] * values[M22] * values[M13] - (values[M20] * values[M01] * values[M32]
                * values[M13]) + values[M00] * values[M21] * values[M32] * values[M13] + values[M30] * values[M11] * values[M02] * values[M23] - (values[M10] * values[M31]
                * values[M02] * values[M23]) - values[M30] * values[M01] * values[M12] * values[M23] + values[M00] * values[M31] * values[M12] * values[M23] + (values[M10]
                * values[M01] * values[M32] * values[M23]) - values[M00] * values[M11] * values[M32] * values[M23] - values[M20] * values[M11] * values[M02] * values[M33]
                + values[M10] * values[M21] * values[M02] * values[M33] + values[M20] * values[M01] * values[M12] * values[M33]) - (values[M00] * values[M21] * values[M12]
                * values[M33]) - values[M10] * values[M01] * values[M22] * values[M33] + values[M00] * values[M11] * values[M22] * values[M33]
        if (l_det == 0f) throw RuntimeException("non-invertible matrix")
        val inv_det = 1.0f / l_det
        tmp[M00] = values[M12] * values[M23] * values[M31] - values[M13] * values[M22] * values[M31] + values[M13] * values[M21] * values[M32] - (values[M11]
                * values[M23] * values[M32]) - values[M12] * values[M21] * values[M33] + values[M11] * values[M22] * values[M33]
        tmp[M01] = values[M03] * values[M22] * values[M31] - values[M02] * values[M23] * values[M31] - values[M03] * values[M21] * values[M32] + (values[M01]
                * values[M23] * values[M32]) + values[M02] * values[M21] * values[M33] - values[M01] * values[M22] * values[M33]
        tmp[M02] = values[M02] * values[M13] * values[M31] - values[M03] * values[M12] * values[M31] + values[M03] * values[M11] * values[M32] - (values[M01]
                * values[M13] * values[M32]) - values[M02] * values[M11] * values[M33] + values[M01] * values[M12] * values[M33]
        tmp[M03] = values[M03] * values[M12] * values[M21] - values[M02] * values[M13] * values[M21] - values[M03] * values[M11] * values[M22] + (values[M01]
                * values[M13] * values[M22]) + values[M02] * values[M11] * values[M23] - values[M01] * values[M12] * values[M23]
        tmp[M10] = values[M13] * values[M22] * values[M30] - values[M12] * values[M23] * values[M30] - values[M13] * values[M20] * values[M32] + (values[M10]
                * values[M23] * values[M32]) + values[M12] * values[M20] * values[M33] - values[M10] * values[M22] * values[M33]
        tmp[M11] = values[M02] * values[M23] * values[M30] - values[M03] * values[M22] * values[M30] + values[M03] * values[M20] * values[M32] - (values[M00]
                * values[M23] * values[M32]) - values[M02] * values[M20] * values[M33] + values[M00] * values[M22] * values[M33]
        tmp[M12] = values[M03] * values[M12] * values[M30] - values[M02] * values[M13] * values[M30] - values[M03] * values[M10] * values[M32] + (values[M00]
                * values[M13] * values[M32]) + values[M02] * values[M10] * values[M33] - values[M00] * values[M12] * values[M33]
        tmp[M13] = values[M02] * values[M13] * values[M20] - values[M03] * values[M12] * values[M20] + values[M03] * values[M10] * values[M22] - (values[M00]
                * values[M13] * values[M22]) - values[M02] * values[M10] * values[M23] + values[M00] * values[M12] * values[M23]
        tmp[M20] = values[M11] * values[M23] * values[M30] - values[M13] * values[M21] * values[M30] + values[M13] * values[M20] * values[M31] - (values[M10]
                * values[M23] * values[M31]) - values[M11] * values[M20] * values[M33] + values[M10] * values[M21] * values[M33]
        tmp[M21] = values[M03] * values[M21] * values[M30] - values[M01] * values[M23] * values[M30] - values[M03] * values[M20] * values[M31] + (values[M00]
                * values[M23] * values[M31]) + values[M01] * values[M20] * values[M33] - values[M00] * values[M21] * values[M33]
        tmp[M22] = values[M01] * values[M13] * values[M30] - values[M03] * values[M11] * values[M30] + values[M03] * values[M10] * values[M31] - (values[M00]
                * values[M13] * values[M31]) - values[M01] * values[M10] * values[M33] + values[M00] * values[M11] * values[M33]
        tmp[M23] = values[M03] * values[M11] * values[M20] - values[M01] * values[M13] * values[M20] - values[M03] * values[M10] * values[M21] + (values[M00]
                * values[M13] * values[M21]) + values[M01] * values[M10] * values[M23] - values[M00] * values[M11] * values[M23]
        tmp[M30] = values[M12] * values[M21] * values[M30] - values[M11] * values[M22] * values[M30] - values[M12] * values[M20] * values[M31] + (values[M10]
                * values[M22] * values[M31]) + values[M11] * values[M20] * values[M32] - values[M10] * values[M21] * values[M32]
        tmp[M31] = values[M01] * values[M22] * values[M30] - values[M02] * values[M21] * values[M30] + values[M02] * values[M20] * values[M31] - (values[M00]
                * values[M22] * values[M31]) - values[M01] * values[M20] * values[M32] + values[M00] * values[M21] * values[M32]
        tmp[M32] = values[M02] * values[M11] * values[M30] - values[M01] * values[M12] * values[M30] - values[M02] * values[M10] * values[M31] + (values[M00]
                * values[M12] * values[M31]) + values[M01] * values[M10] * values[M32] - values[M00] * values[M11] * values[M32]
        tmp[M33] = values[M01] * values[M12] * values[M20] - values[M02] * values[M11] * values[M20] + values[M02] * values[M10] * values[M21] - (values[M00]
                * values[M12] * values[M21]) - values[M01] * values[M10] * values[M22] + values[M00] * values[M11] * values[M22]
        values[M00] = tmp[M00] * inv_det
        values[M01] = tmp[M01] * inv_det
        values[M02] = tmp[M02] * inv_det
        values[M03] = tmp[M03] * inv_det
        values[M10] = tmp[M10] * inv_det
        values[M11] = tmp[M11] * inv_det
        values[M12] = tmp[M12] * inv_det
        values[M13] = tmp[M13] * inv_det
        values[M20] = tmp[M20] * inv_det
        values[M21] = tmp[M21] * inv_det
        values[M22] = tmp[M22] * inv_det
        values[M23] = tmp[M23] * inv_det
        values[M30] = tmp[M30] * inv_det
        values[M31] = tmp[M31] * inv_det
        values[M32] = tmp[M32] * inv_det
        values[M33] = tmp[M33] * inv_det
        return this
    }

    /** @return The determinant of this matrix
     */
    fun det(): Float {
        return ((values[M30] * values[M21] * values[M12] * values[M03] - values[M20] * values[M31] * values[M12] * values[M03] - (values[M30] * values[M11]
                * values[M22] * values[M03]) + values[M10] * values[M31] * values[M22] * values[M03] + values[M20] * values[M11] * values[M32] * values[M03] - (values[M10]
                * values[M21] * values[M32] * values[M03]) - values[M30] * values[M21] * values[M02] * values[M13] + values[M20] * values[M31] * values[M02] * values[M13]
                + values[M30] * values[M01] * values[M22] * values[M13]) - values[M00] * values[M31] * values[M22] * values[M13] - (values[M20] * values[M01] * values[M32]
                * values[M13]) + values[M00] * values[M21] * values[M32] * values[M13] + values[M30] * values[M11] * values[M02] * values[M23] - (values[M10] * values[M31]
                * values[M02] * values[M23]) - values[M30] * values[M01] * values[M12] * values[M23] + values[M00] * values[M31] * values[M12] * values[M23] + (values[M10]
                * values[M01] * values[M32] * values[M23]) - values[M00] * values[M11] * values[M32] * values[M23] - values[M20] * values[M11] * values[M02] * values[M33]
                + values[M10] * values[M21] * values[M02] * values[M33] + values[M20] * values[M01] * values[M12] * values[M33]) - (values[M00] * values[M21] * values[M12]
                * values[M33]) - values[M10] * values[M01] * values[M22] * values[M33] + values[M00] * values[M11] * values[M22] * values[M33]
    }

    /** @return The determinant of the 3x3 upper left matrix
     */
    fun det3x3(): Float {
        return values[M00] * values[M11] * values[M22] + values[M01] * values[M12] * values[M20] + values[M02] * values[M10] * values[M21] - (values[M00]
                * values[M12] * values[M21]) - values[M01] * values[M10] * values[M22] - values[M02] * values[M11] * values[M20]
    }

    /** Sets the matrix to a projection matrix with a near- and far plane, a field of view in degrees and an aspect ratio. Note that
     * the field of view specified is the angle in degrees for the height, the field of view for the width will be calculated
     * according to the aspect ratio.
     *
     * @param near The near plane
     * @param far The far plane
     * @param fovy The field of view of the height in degrees
     * @param aspectRatio The "width over height" aspect ratio
     * @return This matrix for the purpose of chaining methods together.
     */
    fun setToProjection(near: Float, far: Float, fovy: Float, aspectRatio: Float): Matrix4 {
        idt()
        val l_fd = (1.0 / Math.tan(fovy * (Math.PI / 180) / 2.0)).toFloat()
        val l_a1 = (far + near) / (near - far)
        val l_a2 = 2f * far * near / (near - far)
        values[M00] = l_fd / aspectRatio
        values[M10] = 0f
        values[M20] = 0f
        values[M30] = 0f
        values[M01] = 0f
        values[M11] = l_fd
        values[M21] = 0f
        values[M31] = 0f
        values[M02] = 0f
        values[M12] = 0f
        values[M22] = l_a1
        values[M32] = -1f
        values[M03] = 0f
        values[M13] = 0f
        values[M23] = l_a2
        values[M33] = 0f

        return this
    }

    /** Sets the matrix to a projection matrix with a near/far plane, and left, bottom, right and top specifying the points on the
     * near plane that are mapped to the lower left and upper right corners of the viewport. This allows to create projection
     * matrix with off-center vanishing point.
     *
     * @param left
     * @param right
     * @param bottom
     * @param top
     * @param near The near plane
     * @param far The far plane
     * @return This matrix for the purpose of chaining methods together.
     */
    fun setToProjection(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): Matrix4 {
        val x = 2.0f * near / (right - left)
        val y = 2.0f * near / (top - bottom)
        val a = (right + left) / (right - left)
        val b = (top + bottom) / (top - bottom)
        val l_a1 = (far + near) / (near - far)
        val l_a2 = 2f * far * near / (near - far)
        values[M00] = x
        values[M10] = 0f
        values[M20] = 0f
        values[M30] = 0f
        values[M01] = 0f
        values[M11] = y
        values[M21] = 0f
        values[M31] = 0f
        values[M02] = a
        values[M12] = b
        values[M22] = l_a1
        values[M32] = -1f
        values[M03] = 0f
        values[M13] = 0f
        values[M23] = l_a2
        values[M33] = 0f

        return this
    }

    /** Sets this matrix to an orthographic projection matrix with the origin at (x,y) extending by width and height. The near plane
     * is set to 0, the far plane is set to 1.
     *
     * @param x The x-coordinate of the origin
     * @param y The y-coordinate of the origin
     * @param width The width
     * @param height The height
     * @return This matrix for the purpose of chaining methods together.
     */
    fun setToOrtho2D(x: Float, y: Float, width: Float, height: Float): Matrix4 {
        setToOrtho(x, x + width, y, y + height, 0f, 1f)
        return this
    }

    /** Sets this matrix to an orthographic projection matrix with the origin at (x,y) extending by width and height, having a near
     * and far plane.
     *
     * @param x The x-coordinate of the origin
     * @param y The y-coordinate of the origin
     * @param width The width
     * @param height The height
     * @param near The near plane
     * @param far The far plane
     * @return This matrix for the purpose of chaining methods together.
     */
    fun setToOrtho2D(x: Float, y: Float, width: Float, height: Float, near: Float, far: Float): Matrix4 {
        setToOrtho(x, x + width, y, y + height, near, far)
        return this
    }

    /** Sets the matrix to an orthographic projection like glOrtho (http://www.opengl.org/sdk/docs/man/xhtml/glOrtho.xml) following
     * the OpenGL equivalent
     *
     * @param left The left clipping plane
     * @param right The right clipping plane
     * @param bottom The bottom clipping plane
     * @param top The top clipping plane
     * @param near The near clipping plane
     * @param far The far clipping plane
     * @return This matrix for the purpose of chaining methods together.
     */
    fun setToOrtho(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): Matrix4 {

        this.idt()
        val x_orth = 2 / (right - left)
        val y_orth = 2 / (top - bottom)
        val z_orth = -2 / (far - near)

        val tx = -(right + left) / (right - left)
        val ty = -(top + bottom) / (top - bottom)
        val tz = -(far + near) / (far - near)

        values[M00] = x_orth
        values[M10] = 0f
        values[M20] = 0f
        values[M30] = 0f
        values[M01] = 0f
        values[M11] = y_orth
        values[M21] = 0f
        values[M31] = 0f
        values[M02] = 0f
        values[M12] = 0f
        values[M22] = z_orth
        values[M32] = 0f
        values[M03] = tx
        values[M13] = ty
        values[M23] = tz
        values[M33] = 1f

        return this
    }

    /** Sets the 4th column to the translation vector.
     *
     * @param vector The translation vector
     * @return This matrix for the purpose of chaining methods together.
     */
    fun setTranslation(vector: Vector3): Matrix4 {
        values[M03] = vector.x
        values[M13] = vector.y
        values[M23] = vector.z
        return this
    }

    /** Sets the 4th column to the translation vector.
     *
     * @param x The X coordinate of the translation vector
     * @param y The Y coordinate of the translation vector
     * @param z The Z coordinate of the translation vector
     * @return This matrix for the purpose of chaining methods together.
     */
    fun setTranslation(x: Float, y: Float, z: Float): Matrix4 {
        values[M03] = x
        values[M13] = y
        values[M23] = z
        return this
    }

    /** Sets this matrix to a translation matrix, overwriting it first by an identity matrix and then setting the 4th column to the
     * translation vector.
     *
     * @param vector The translation vector
     * @return This matrix for the purpose of chaining methods together.
     */
    fun setToTranslation(vector: Vector3): Matrix4 {
        idt()
        values[M03] = vector.x
        values[M13] = vector.y
        values[M23] = vector.z
        return this
    }

    /** Sets this matrix to a translation matrix, overwriting it first by an identity matrix and then setting the 4th column to the
     * translation vector.
     *
     * @param x The x-component of the translation vector.
     * @param y The y-component of the translation vector.
     * @param z The z-component of the translation vector.
     * @return This matrix for the purpose of chaining methods together.
     */
    fun setToTranslation(x: Float, y: Float, z: Float): Matrix4 {
        idt()
        values[M03] = x
        values[M13] = y
        values[M23] = z
        return this
    }

    /** Sets this matrix to a translation and scaling matrix by first overwriting it with an identity and then setting the
     * translation vector in the 4th column and the scaling vector in the diagonal.
     *
     * @param translation The translation vector
     * @param scaling The scaling vector
     * @return This matrix for the purpose of chaining methods together.
     */
    fun setToTranslationAndScaling(translation: Vector3, scaling: Vector3): Matrix4 {
        idt()
        values[M03] = translation.x
        values[M13] = translation.y
        values[M23] = translation.z
        values[M00] = scaling.x
        values[M11] = scaling.y
        values[M22] = scaling.z
        return this
    }

    /** Sets this matrix to a translation and scaling matrix by first overwriting it with an identity and then setting the
     * translation vector in the 4th column and the scaling vector in the diagonal.
     *
     * @param translationX The x-component of the translation vector
     * @param translationY The y-component of the translation vector
     * @param translationZ The z-component of the translation vector
     * @param scalingX The x-component of the scaling vector
     * @param scalingY The x-component of the scaling vector
     * @param scalingZ The x-component of the scaling vector
     * @return This matrix for the purpose of chaining methods together.
     */
    fun setToTranslationAndScaling(translationX: Float, translationY: Float, translationZ: Float, scalingX: Float,
                                   scalingY: Float, scalingZ: Float): Matrix4 {
        idt()
        values[M03] = translationX
        values[M13] = translationY
        values[M23] = translationZ
        values[M00] = scalingX
        values[M11] = scalingY
        values[M22] = scalingZ
        return this
    }

    /** Sets the matrix to a rotation matrix around the given axis.
     *
     * @param axis The axis
     * @param degrees The angle in degrees
     * @return This matrix for the purpose of chaining methods together.
     */
    fun setToRotation(axis: Vector3, degrees: Float): Matrix4 {
        if (degrees == 0f) {
            idt()
            return this
        }
        return set(quat.set(axis, degrees))
    }

    /** Sets the matrix to a rotation matrix around the given axis.
     *
     * @param axis The axis
     * @param radians The angle in radians
     * @return This matrix for the purpose of chaining methods together.
     */
    fun setToRotationRad(axis: Vector3, radians: Float): Matrix4 {
        if (radians == 0f) {
            idt()
            return this
        }
        return set(quat.setFromAxisRad(axis, radians))
    }

    /** Sets the matrix to a rotation matrix around the given axis.
     *
     * @param axisX The x-component of the axis
     * @param axisY The y-component of the axis
     * @param axisZ The z-component of the axis
     * @param degrees The angle in degrees
     * @return This matrix for the purpose of chaining methods together.
     */
    fun setToRotation(axisX: Float, axisY: Float, axisZ: Float, degrees: Float): Matrix4 {
        if (degrees == 0f) {
            idt()
            return this
        }
        return set(quat.setFromAxis(axisX, axisY, axisZ, degrees))
    }

    /** Sets the matrix to a rotation matrix around the given axis.
     *
     * @param axisX The x-component of the axis
     * @param axisY The y-component of the axis
     * @param axisZ The z-component of the axis
     * @param radians The angle in radians
     * @return This matrix for the purpose of chaining methods together.
     */
    fun setToRotationRad(axisX: Float, axisY: Float, axisZ: Float, radians: Float): Matrix4 {
        if (radians == 0f) {
            idt()
            return this
        }
        return set(quat.setFromAxisRad(axisX, axisY, axisZ, radians))
    }

    /** Set the matrix to a rotation matrix between two vectors.
     * @param v1 The base vector
     * @param v2 The target vector
     * @return This matrix for the purpose of chaining methods together
     */
    fun setToRotation(v1: Vector3, v2: Vector3): Matrix4 {
        return set(quat.setFromCross(v1, v2))
    }

    /** Set the matrix to a rotation matrix between two vectors.
     * @param x1 The base vectors x value
     * @param y1 The base vectors y value
     * @param z1 The base vectors z value
     * @param x2 The target vector x value
     * @param y2 The target vector y value
     * @param z2 The target vector z value
     * @return This matrix for the purpose of chaining methods together
     */
    fun setToRotation(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Matrix4 {
        return set(quat.setFromCross(x1, y1, z1, x2, y2, z2))
    }

    /** Sets this matrix to a rotation matrix from the given euler angles.
     * @param yaw the yaw in degrees
     * @param pitch the pitch in degrees
     * @param roll the roll in degrees
     * @return This matrix
     */
    fun setFromEulerAngles(yaw: Float, pitch: Float, roll: Float): Matrix4 {
        quat.setEulerAngles(yaw, pitch, roll)
        return set(quat)
    }

    /** Sets this matrix to a rotation matrix from the given euler angles.
     * @param yaw the yaw in radians
     * @param pitch the pitch in radians
     * @param roll the roll in radians
     * @return This matrix
     */
    fun setFromEulerAnglesRad(yaw: Float, pitch: Float, roll: Float): Matrix4 {
        quat.setEulerAnglesRad(yaw, pitch, roll)
        return set(quat)
    }

    /** Sets this matrix to a scaling matrix
     *
     * @param vector The scaling vector
     * @return This matrix for chaining.
     */
    fun setToScaling(vector: Vector3): Matrix4 {
        idt()
        values[M00] = vector.x
        values[M11] = vector.y
        values[M22] = vector.z
        return this
    }

    /** Sets this matrix to a scaling matrix
     *
     * @param x The x-component of the scaling vector
     * @param y The y-component of the scaling vector
     * @param z The z-component of the scaling vector
     * @return This matrix for chaining.
     */
    fun setToScaling(x: Float, y: Float, z: Float): Matrix4 {
        idt()
        values[M00] = x
        values[M11] = y
        values[M22] = z
        return this
    }

    /** Sets the matrix to a look at matrix with a direction and an up vector. Multiply with a translation matrix to get a camera
     * model view matrix.
     *
     * @param direction The direction vector
     * @param up The up vector
     * @return This matrix for the purpose of chaining methods together.
     */
    fun setToLookAt(direction: Vector3, up: Vector3): Matrix4 {
        l_vez.set(direction).nor()
        l_vex.set(direction).crs(up).nor()
        l_vey.set(l_vex).crs(l_vez).nor()
        idt()
        values[M00] = l_vex.x
        values[M01] = l_vex.y
        values[M02] = l_vex.z
        values[M10] = l_vey.x
        values[M11] = l_vey.y
        values[M12] = l_vey.z
        values[M20] = -l_vez.x
        values[M21] = -l_vez.y
        values[M22] = -l_vez.z

        return this
    }

    /** Sets this matrix to a look at matrix with the given position, target and up vector.
     *
     * @param position the position
     * @param target the target
     * @param up the up vector
     * @return This matrix
     */
    fun setToLookAt(position: Vector3, target: Vector3, up: Vector3): Matrix4 {
        tmpVec.set(target).sub(position)
        setToLookAt(tmpVec, up)
        this.mul(tmpMat.setToTranslation(-position.x, -position.y, -position.z))

        return this
    }

    fun setToWorld(position: Vector3, forward: Vector3, up: Vector3): Matrix4 {
        tmpForward.set(forward).nor()
        right.set(tmpForward).crs(up).nor()
        tmpUp.set(right).crs(tmpForward).nor()

        this[right, tmpUp, tmpForward.scl(-1f)] = position
        return this
    }

    override fun toString(): String {
        return ("[" + values[M00] + "|" + values[M01] + "|" + values[M02] + "|" + values[M03] + "]\n" + "[" + values[M10] + "|" + values[M11] + "|"
                + values[M12] + "|" + values[M13] + "]\n" + "[" + values[M20] + "|" + values[M21] + "|" + values[M22] + "|" + values[M23] + "]\n" + "["
                + values[M30] + "|" + values[M31] + "|" + values[M32] + "|" + values[M33] + "]\n")
    }

    /** Linearly interpolates between this matrix and the given matrix mixing by alpha
     * @param matrix the matrix
     * @param alpha the alpha value in the range [0,1]
     * @return This matrix for the purpose of chaining methods together.
     */
    fun lerp(matrix: Matrix4, alpha: Float): Matrix4 {
        for (i in 0..15)
            this.values[i] = this.values[i] * (1 - alpha) + matrix.values[i] * alpha
        return this
    }

    /** Averages the given transform with this one and stores the result in this matrix. Translations and scales are lerped while
     * rotations are slerped.
     * @param other The other transform
     * @param w Weight of this transform; weight of the other transform is (1 - w)
     * @return This matrix for chaining
     */
    fun avg(other: Matrix4, w: Float): Matrix4 {
        getScale(tmpVec)
        other.getScale(tmpForward)

        getRotation(quat)
        other.getRotation(quat2)

        getTranslation(tmpUp)
        other.getTranslation(right)

        setToScaling(tmpVec.scl(w).add(tmpForward.scl(1 - w)))
        rotate(quat.slerp(quat2, 1 - w))
        setTranslation(tmpUp.scl(w).add(right.scl(1 - w)))

        return this
    }

    /** Averages the given transforms and stores the result in this matrix. Translations and scales are lerped while rotations are
     * slerped. Does not destroy the data contained in t.
     * @param t List of transforms
     * @return This matrix for chaining
     */
    fun avg(t: Array<Matrix4>): Matrix4 {
        val w = 1.0f / t.size

        tmpVec.set(t[0].getScale(tmpUp).scl(w))
        quat.set(t[0].getRotation(quat2).exp(w))
        tmpForward.set(t[0].getTranslation(tmpUp).scl(w))

        for (i in 1 until t.size) {
            tmpVec.add(t[i].getScale(tmpUp).scl(w))
            quat.mul(t[i].getRotation(quat2).exp(w))
            tmpForward.add(t[i].getTranslation(tmpUp).scl(w))
        }
        quat.nor()

        setToScaling(tmpVec)
        rotate(quat)
        setTranslation(tmpForward)

        return this
    }

    /** Averages the given transforms with the given weights and stores the result in this matrix. Translations and scales are
     * lerped while rotations are slerped. Does not destroy the data contained in t or w; Sum of w_i must be equal to 1, or
     * unexpected results will occur.
     * @param t List of transforms
     * @param w List of weights
     * @return This matrix for chaining
     */
    fun avg(t: Array<Matrix4>, w: FloatArray): Matrix4 {
        tmpVec.set(t[0].getScale(tmpUp).scl(w[0]))
        quat.set(t[0].getRotation(quat2).exp(w[0]))
        tmpForward.set(t[0].getTranslation(tmpUp).scl(w[0]))

        for (i in 1 until t.size) {
            tmpVec.add(t[i].getScale(tmpUp).scl(w[i]))
            quat.mul(t[i].getRotation(quat2).exp(w[i]))
            tmpForward.add(t[i].getTranslation(tmpUp).scl(w[i]))
        }
        quat.nor()

        setToScaling(tmpVec)
        rotate(quat)
        setTranslation(tmpForward)

        return this
    }

    /** Sets this matrix to the given 3x3 matrix. The third column of this matrix is set to (0,0,1,0).
     * @param mat the matrix
     */
    fun set(mat: Matrix3): Matrix4 {
        values[0] = mat.`val`[0]
        values[1] = mat.`val`[1]
        values[2] = mat.`val`[2]
        values[3] = 0f
        values[4] = mat.`val`[3]
        values[5] = mat.`val`[4]
        values[6] = mat.`val`[5]
        values[7] = 0f
        values[8] = 0f
        values[9] = 0f
        values[10] = 1f
        values[11] = 0f
        values[12] = mat.`val`[6]
        values[13] = mat.`val`[7]
        values[14] = 0f
        values[15] = mat.`val`[8]
        return this
    }

    /** Sets this matrix to the given affine matrix. The values are mapped as follows:
     *
     * <pre>
     * [  M00  M01   0   M02  ]
     * [  M10  M11   0   M12  ]
     * [   0    0    1    0   ]
     * [   0    0    0    1   ]
    </pre> *
     * @param affine the affine matrix
     * @return This matrix for chaining
     */
    fun set(affine: Affine2): Matrix4 {
        values[M00] = affine.m00
        values[M10] = affine.m10
        values[M20] = 0f
        values[M30] = 0f
        values[M01] = affine.m01
        values[M11] = affine.m11
        values[M21] = 0f
        values[M31] = 0f
        values[M02] = 0f
        values[M12] = 0f
        values[M22] = 1f
        values[M32] = 0f
        values[M03] = affine.m02
        values[M13] = affine.m12
        values[M23] = 0f
        values[M33] = 1f
        return this
    }

    /** Assumes that this matrix is a 2D affine transformation, copying only the relevant components. The values are mapped as
     * follows:
     *
     * <pre>
     * [  M00  M01   _   M02  ]
     * [  M10  M11   _   M12  ]
     * [   _    _    _    _   ]
     * [   _    _    _    _   ]
    </pre> *
     * @param affine the source matrix
     * @return This matrix for chaining
     */
    fun setAsAffine(affine: Affine2): Matrix4 {
        values[M00] = affine.m00
        values[M10] = affine.m10
        values[M01] = affine.m01
        values[M11] = affine.m11
        values[M03] = affine.m02
        values[M13] = affine.m12
        return this
    }

    /** Assumes that both matrices are 2D affine transformations, copying only the relevant components. The copied values are:
     *
     * <pre>
     * [  M00  M01   _   M03  ]
     * [  M10  M11   _   M13  ]
     * [   _    _    _    _   ]
     * [   _    _    _    _   ]
    </pre> *
     * @param mat the source matrix
     * @return This matrix for chaining
     */
    fun setAsAffine(mat: Matrix4): Matrix4 {
        values[M00] = mat.values[M00]
        values[M10] = mat.values[M10]
        values[M01] = mat.values[M01]
        values[M11] = mat.values[M11]
        values[M03] = mat.values[M03]
        values[M13] = mat.values[M13]
        return this
    }

    fun scl(scale: Vector3): Matrix4 {
        values[M00] *= scale.x
        values[M11] *= scale.y
        values[M22] *= scale.z
        return this
    }

    fun scl(x: Float, y: Float, z: Float): Matrix4 {
        values[M00] *= x
        values[M11] *= y
        values[M22] *= z
        return this
    }

    fun scl(scale: Float): Matrix4 {
        values[M00] *= scale
        values[M11] *= scale
        values[M22] *= scale
        return this
    }

    fun getTranslation(position: Vector3): Vector3 {
        position.x = values[M03]
        position.y = values[M13]
        position.z = values[M23]
        return position
    }

    /** Gets the rotation of this matrix.
     * @param rotation The [Quaternion] to receive the rotation
     * @param normalizeAxes True to normalize the axes, necessary when the matrix might also include scaling.
     * @return The provided [Quaternion] for chaining.
     */
    fun getRotation(rotation: Quaternion, normalizeAxes: Boolean): Quaternion {
        return rotation.setFromMatrix(normalizeAxes, this)
    }

    /** Gets the rotation of this matrix.
     * @param rotation The [Quaternion] to receive the rotation
     * @return The provided [Quaternion] for chaining.
     */
    fun getRotation(rotation: Quaternion): Quaternion {
        return rotation.setFromMatrix(this)
    }

    /** @param scale The vector which will receive the (non-negative) scale components on each axis.
     * @return The provided vector for chaining.
     */
    fun getScale(scale: Vector3): Vector3 {
        return scale.set(scaleX, scaleY, scaleZ)
    }

    /** removes the translational part and transposes the matrix.  */
    fun toNormalMatrix(): Matrix4 {
        values[M03] = 0f
        values[M13] = 0f
        values[M23] = 0f
        return inv().tra()
    }

    // @on
    /** Postmultiplies this matrix by a translation matrix. Postmultiplication is also used by OpenGL ES'
     * glTranslate/glRotate/glScale
     * @param translation
     * @return This matrix for the purpose of chaining methods together.
     */
    fun translate(translation: Vector3): Matrix4 {
        return translate(translation.x, translation.y, translation.z)
    }

    /** Postmultiplies this matrix by a translation matrix. Postmultiplication is also used by OpenGL ES' 1.x
     * glTranslate/glRotate/glScale.
     * @param x Translation in the x-axis.
     * @param y Translation in the y-axis.
     * @param z Translation in the z-axis.
     * @return This matrix for the purpose of chaining methods together.
     */
    fun translate(x: Float, y: Float, z: Float): Matrix4 {
        tmp[M00] = 1f
        tmp[M01] = 0f
        tmp[M02] = 0f
        tmp[M03] = x
        tmp[M10] = 0f
        tmp[M11] = 1f
        tmp[M12] = 0f
        tmp[M13] = y
        tmp[M20] = 0f
        tmp[M21] = 0f
        tmp[M22] = 1f
        tmp[M23] = z
        tmp[M30] = 0f
        tmp[M31] = 0f
        tmp[M32] = 0f
        tmp[M33] = 1f

        mul(values, tmp)
        return this
    }

    /** Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by OpenGL ES' 1.x
     * glTranslate/glRotate/glScale.
     *
     * @param axis The vector axis to rotate around.
     * @param degrees The angle in degrees.
     * @return This matrix for the purpose of chaining methods together.
     */
    fun rotate(axis: Vector3, degrees: Float): Matrix4 {
        if (degrees == 0f) return this
        quat.set(axis, degrees)
        return rotate(quat)
    }

    /** Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by OpenGL ES' 1.x
     * glTranslate/glRotate/glScale.
     *
     * @param axis The vector axis to rotate around.
     * @param radians The angle in radians.
     * @return This matrix for the purpose of chaining methods together.
     */
    fun rotateRad(axis: Vector3, radians: Float): Matrix4 {
        if (radians == 0f) return this
        quat.setFromAxisRad(axis, radians)
        return rotate(quat)
    }

    /** Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by OpenGL ES' 1.x
     * glTranslate/glRotate/glScale
     * @param axisX The x-axis component of the vector to rotate around.
     * @param axisY The y-axis component of the vector to rotate around.
     * @param axisZ The z-axis component of the vector to rotate around.
     * @param degrees The angle in degrees
     * @return This matrix for the purpose of chaining methods together.
     */
    fun rotate(axisX: Float, axisY: Float, axisZ: Float, degrees: Float): Matrix4 {
        if (degrees == 0f) return this
        quat.setFromAxis(axisX, axisY, axisZ, degrees)
        return rotate(quat)
    }

    /** Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by OpenGL ES' 1.x
     * glTranslate/glRotate/glScale
     * @param axisX The x-axis component of the vector to rotate around.
     * @param axisY The y-axis component of the vector to rotate around.
     * @param axisZ The z-axis component of the vector to rotate around.
     * @param radians The angle in radians
     * @return This matrix for the purpose of chaining methods together.
     */
    fun rotateRad(axisX: Float, axisY: Float, axisZ: Float, radians: Float): Matrix4 {
        if (radians == 0f) return this
        quat.setFromAxisRad(axisX, axisY, axisZ, radians)
        return rotate(quat)
    }

    /** Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by OpenGL ES' 1.x
     * glTranslate/glRotate/glScale.
     *
     * @param rotation
     * @return This matrix for the purpose of chaining methods together.
     */
    fun rotate(rotation: Quaternion): Matrix4 {
        rotation.toMatrix(tmp)
        mul(values, tmp)
        return this
    }

    /** Postmultiplies this matrix by the rotation between two vectors.
     * @param v1 The base vector
     * @param v2 The target vector
     * @return This matrix for the purpose of chaining methods together
     */
    fun rotate(v1: Vector3, v2: Vector3): Matrix4 {
        return rotate(quat.setFromCross(v1, v2))
    }

    /** Post-multiplies this matrix by a rotation toward a direction.
     * @param direction direction to rotate toward
     * @param up up vector
     * @return This matrix for chaining
     */
    fun rotateTowardDirection(direction: Vector3, up: Vector3): Matrix4 {
        l_vez.set(direction).nor()
        l_vex.set(direction).crs(up).nor()
        l_vey.set(l_vex).crs(l_vez).nor()
        tmp[M00] = l_vex.x
        tmp[M10] = l_vex.y
        tmp[M20] = l_vex.z
        tmp[M30] = 0f
        tmp[M01] = l_vey.x
        tmp[M11] = l_vey.y
        tmp[M21] = l_vey.z
        tmp[M31] = 0f
        tmp[M02] = -l_vez.x
        tmp[M12] = -l_vez.y
        tmp[M22] = -l_vez.z
        tmp[M32] = 0f
        tmp[M03] = 0f
        tmp[M13] = 0f
        tmp[M23] = 0f
        tmp[M33] = 1f
        mul(values, tmp)
        return this
    }

    /** Post-multiplies this matrix by a rotation toward a target.
     * @param target the target to rotate to
     * @param up the up vector
     * @return This matrix for chaining
     */
    fun rotateTowardTarget(target: Vector3, up: Vector3): Matrix4 {
        tmpVec.set(target.x - values[M03], target.y - values[M13], target.z - values[M23])
        return rotateTowardDirection(tmpVec, up)
    }

    /** Postmultiplies this matrix with a scale matrix. Postmultiplication is also used by OpenGL ES' 1.x
     * glTranslate/glRotate/glScale.
     * @param scaleX The scale in the x-axis.
     * @param scaleY The scale in the y-axis.
     * @param scaleZ The scale in the z-axis.
     * @return This matrix for the purpose of chaining methods together.
     */
    fun scale(scaleX: Float, scaleY: Float, scaleZ: Float): Matrix4 {
        tmp[M00] = scaleX
        tmp[M01] = 0f
        tmp[M02] = 0f
        tmp[M03] = 0f
        tmp[M10] = 0f
        tmp[M11] = scaleY
        tmp[M12] = 0f
        tmp[M13] = 0f
        tmp[M20] = 0f
        tmp[M21] = 0f
        tmp[M22] = scaleZ
        tmp[M23] = 0f
        tmp[M30] = 0f
        tmp[M31] = 0f
        tmp[M32] = 0f
        tmp[M33] = 1f

        mul(values, tmp)
        return this
    }

    /** Copies the 4x3 upper-left sub-matrix into float array. The destination array is supposed to be a column major matrix.
     * @param dst the destination matrix
     */
    fun extract4x3Matrix(dst: FloatArray) {
        dst[0] = values[M00]
        dst[1] = values[M10]
        dst[2] = values[M20]
        dst[3] = values[M01]
        dst[4] = values[M11]
        dst[5] = values[M21]
        dst[6] = values[M02]
        dst[7] = values[M12]
        dst[8] = values[M22]
        dst[9] = values[M03]
        dst[10] = values[M13]
        dst[11] = values[M23]
    }

    /** @return True if this matrix has any rotation or scaling, false otherwise
     */
    fun hasRotationOrScaling(): Boolean {
        return !(MathUtils.isEqual(values[M00], 1f) && MathUtils.isEqual(values[M11], 1f) && MathUtils.isEqual(values[M22], 1f)
                && MathUtils.isZero(values[M01]) && MathUtils.isZero(values[M02]) && MathUtils.isZero(values[M10]) && MathUtils.isZero(values[M12])
                && MathUtils.isZero(values[M20]) && MathUtils.isZero(values[M21]))
    }

    companion object {
        private const val serialVersionUID = -2717655254359579617L

        /** XX: Typically the unrotated X component for scaling, also the cosine of the angle when rotated on the Y and/or Z axis. On
         * Vector3 multiplication this value is multiplied with the source X component and added to the target X component.  */
        const val M00 = 0

        /** XY: Typically the negative sine of the angle when rotated on the Z axis. On Vector3 multiplication this value is multiplied
         * with the source Y component and added to the target X component.  */
        const val M01 = 4

        /** XZ: Typically the sine of the angle when rotated on the Y axis. On Vector3 multiplication this value is multiplied with the
         * source Z component and added to the target X component.  */
        const val M02 = 8

        /** XW: Typically the translation of the X component. On Vector3 multiplication this value is added to the target X component.  */
        const val M03 = 12

        /** YX: Typically the sine of the angle when rotated on the Z axis. On Vector3 multiplication this value is multiplied with the
         * source X component and added to the target Y component.  */
        const val M10 = 1

        /** YY: Typically the unrotated Y component for scaling, also the cosine of the angle when rotated on the X and/or Z axis. On
         * Vector3 multiplication this value is multiplied with the source Y component and added to the target Y component.  */
        const val M11 = 5

        /** YZ: Typically the negative sine of the angle when rotated on the X axis. On Vector3 multiplication this value is multiplied
         * with the source Z component and added to the target Y component.  */
        const val M12 = 9

        /** YW: Typically the translation of the Y component. On Vector3 multiplication this value is added to the target Y component.  */
        const val M13 = 13

        /** ZX: Typically the negative sine of the angle when rotated on the Y axis. On Vector3 multiplication this value is multiplied
         * with the source X component and added to the target Z component.  */
        const val M20 = 2

        /** ZY: Typical the sine of the angle when rotated on the X axis. On Vector3 multiplication this value is multiplied with the
         * source Y component and added to the target Z component.  */
        const val M21 = 6

        /** ZZ: Typically the unrotated Z component for scaling, also the cosine of the angle when rotated on the X and/or Y axis. On
         * Vector3 multiplication this value is multiplied with the source Z component and added to the target Z component.  */
        const val M22 = 10

        /** ZW: Typically the translation of the Z component. On Vector3 multiplication this value is added to the target Z component.  */
        const val M23 = 14

        /** WX: Typically the value zero. On Vector3 multiplication this value is ignored.  */
        const val M30 = 3

        /** WY: Typically the value zero. On Vector3 multiplication this value is ignored.  */
        const val M31 = 7

        /** WZ: Typically the value zero. On Vector3 multiplication this value is ignored.  */
        const val M32 = 11

        /** WW: Typically the value one. On Vector3 multiplication this value is ignored.  */
        const val M33 = 15

        private val tmp = FloatArray(16)

        internal var quat = Quaternion()
        internal var quat2 = Quaternion()

        internal val l_vez = Vector3()
        internal val l_vex = Vector3()
        internal val l_vey = Vector3()

        internal val tmpVec = Vector3()
        internal val tmpMat = Matrix4()

        internal val right = Vector3()
        internal val tmpForward = Vector3()
        internal val tmpUp = Vector3()

        // @off
        /*JNI
	#include <memory.h>
	#include <stdio.h>
	#include <string.h>
	
	#define M00 0
	#define M01 4
	#define M02 8
	#define M03 12
	#define M10 1
	#define M11 5
	#define M12 9
	#define M13 13
	#define M20 2
	#define M21 6
	#define M22 10
	#define M23 14
	#define M30 3
	#define M31 7
	#define M32 11
	#define M33 15
	
	static inline void matrix4_mul(float* mata, float* matb) {
		float tmp[16];
		tmp[M00] = mata[M00] * matb[M00] + mata[M01] * matb[M10] + mata[M02] * matb[M20] + mata[M03] * matb[M30];
		tmp[M01] = mata[M00] * matb[M01] + mata[M01] * matb[M11] + mata[M02] * matb[M21] + mata[M03] * matb[M31];
		tmp[M02] = mata[M00] * matb[M02] + mata[M01] * matb[M12] + mata[M02] * matb[M22] + mata[M03] * matb[M32];
		tmp[M03] = mata[M00] * matb[M03] + mata[M01] * matb[M13] + mata[M02] * matb[M23] + mata[M03] * matb[M33];
		tmp[M10] = mata[M10] * matb[M00] + mata[M11] * matb[M10] + mata[M12] * matb[M20] + mata[M13] * matb[M30];
		tmp[M11] = mata[M10] * matb[M01] + mata[M11] * matb[M11] + mata[M12] * matb[M21] + mata[M13] * matb[M31];
		tmp[M12] = mata[M10] * matb[M02] + mata[M11] * matb[M12] + mata[M12] * matb[M22] + mata[M13] * matb[M32];
		tmp[M13] = mata[M10] * matb[M03] + mata[M11] * matb[M13] + mata[M12] * matb[M23] + mata[M13] * matb[M33];
		tmp[M20] = mata[M20] * matb[M00] + mata[M21] * matb[M10] + mata[M22] * matb[M20] + mata[M23] * matb[M30];
		tmp[M21] = mata[M20] * matb[M01] + mata[M21] * matb[M11] + mata[M22] * matb[M21] + mata[M23] * matb[M31];
		tmp[M22] = mata[M20] * matb[M02] + mata[M21] * matb[M12] + mata[M22] * matb[M22] + mata[M23] * matb[M32];
		tmp[M23] = mata[M20] * matb[M03] + mata[M21] * matb[M13] + mata[M22] * matb[M23] + mata[M23] * matb[M33];
		tmp[M30] = mata[M30] * matb[M00] + mata[M31] * matb[M10] + mata[M32] * matb[M20] + mata[M33] * matb[M30];
		tmp[M31] = mata[M30] * matb[M01] + mata[M31] * matb[M11] + mata[M32] * matb[M21] + mata[M33] * matb[M31];
		tmp[M32] = mata[M30] * matb[M02] + mata[M31] * matb[M12] + mata[M32] * matb[M22] + mata[M33] * matb[M32];
		tmp[M33] = mata[M30] * matb[M03] + mata[M31] * matb[M13] + mata[M32] * matb[M23] + mata[M33] * matb[M33];
		memcpy(mata, tmp, sizeof(float) *  16);
	}
	
	static inline float matrix4_det(float* val) {
		return val[M30] * val[M21] * val[M12] * val[M03] - val[M20] * val[M31] * val[M12] * val[M03] - val[M30] * val[M11]
				* val[M22] * val[M03] + val[M10] * val[M31] * val[M22] * val[M03] + val[M20] * val[M11] * val[M32] * val[M03] - val[M10]
				* val[M21] * val[M32] * val[M03] - val[M30] * val[M21] * val[M02] * val[M13] + val[M20] * val[M31] * val[M02] * val[M13]
				+ val[M30] * val[M01] * val[M22] * val[M13] - val[M00] * val[M31] * val[M22] * val[M13] - val[M20] * val[M01] * val[M32]
				* val[M13] + val[M00] * val[M21] * val[M32] * val[M13] + val[M30] * val[M11] * val[M02] * val[M23] - val[M10] * val[M31]
				* val[M02] * val[M23] - val[M30] * val[M01] * val[M12] * val[M23] + val[M00] * val[M31] * val[M12] * val[M23] + val[M10]
				* val[M01] * val[M32] * val[M23] - val[M00] * val[M11] * val[M32] * val[M23] - val[M20] * val[M11] * val[M02] * val[M33]
				+ val[M10] * val[M21] * val[M02] * val[M33] + val[M20] * val[M01] * val[M12] * val[M33] - val[M00] * val[M21] * val[M12]
				* val[M33] - val[M10] * val[M01] * val[M22] * val[M33] + val[M00] * val[M11] * val[M22] * val[M33];
	}
	
	static inline bool matrix4_inv(float* val) {
		float tmp[16];
		float l_det = matrix4_det(val);
		if (l_det == 0) return false;
		tmp[M00] = val[M12] * val[M23] * val[M31] - val[M13] * val[M22] * val[M31] + val[M13] * val[M21] * val[M32] - val[M11]
			* val[M23] * val[M32] - val[M12] * val[M21] * val[M33] + val[M11] * val[M22] * val[M33];
		tmp[M01] = val[M03] * val[M22] * val[M31] - val[M02] * val[M23] * val[M31] - val[M03] * val[M21] * val[M32] + val[M01]
			* val[M23] * val[M32] + val[M02] * val[M21] * val[M33] - val[M01] * val[M22] * val[M33];
		tmp[M02] = val[M02] * val[M13] * val[M31] - val[M03] * val[M12] * val[M31] + val[M03] * val[M11] * val[M32] - val[M01]
			* val[M13] * val[M32] - val[M02] * val[M11] * val[M33] + val[M01] * val[M12] * val[M33];
		tmp[M03] = val[M03] * val[M12] * val[M21] - val[M02] * val[M13] * val[M21] - val[M03] * val[M11] * val[M22] + val[M01]
			* val[M13] * val[M22] + val[M02] * val[M11] * val[M23] - val[M01] * val[M12] * val[M23];
		tmp[M10] = val[M13] * val[M22] * val[M30] - val[M12] * val[M23] * val[M30] - val[M13] * val[M20] * val[M32] + val[M10]
			* val[M23] * val[M32] + val[M12] * val[M20] * val[M33] - val[M10] * val[M22] * val[M33];
		tmp[M11] = val[M02] * val[M23] * val[M30] - val[M03] * val[M22] * val[M30] + val[M03] * val[M20] * val[M32] - val[M00]
			* val[M23] * val[M32] - val[M02] * val[M20] * val[M33] + val[M00] * val[M22] * val[M33];
		tmp[M12] = val[M03] * val[M12] * val[M30] - val[M02] * val[M13] * val[M30] - val[M03] * val[M10] * val[M32] + val[M00]
			* val[M13] * val[M32] + val[M02] * val[M10] * val[M33] - val[M00] * val[M12] * val[M33];
		tmp[M13] = val[M02] * val[M13] * val[M20] - val[M03] * val[M12] * val[M20] + val[M03] * val[M10] * val[M22] - val[M00]
			* val[M13] * val[M22] - val[M02] * val[M10] * val[M23] + val[M00] * val[M12] * val[M23];
		tmp[M20] = val[M11] * val[M23] * val[M30] - val[M13] * val[M21] * val[M30] + val[M13] * val[M20] * val[M31] - val[M10]
			* val[M23] * val[M31] - val[M11] * val[M20] * val[M33] + val[M10] * val[M21] * val[M33];
		tmp[M21] = val[M03] * val[M21] * val[M30] - val[M01] * val[M23] * val[M30] - val[M03] * val[M20] * val[M31] + val[M00]
			* val[M23] * val[M31] + val[M01] * val[M20] * val[M33] - val[M00] * val[M21] * val[M33];
		tmp[M22] = val[M01] * val[M13] * val[M30] - val[M03] * val[M11] * val[M30] + val[M03] * val[M10] * val[M31] - val[M00]
			* val[M13] * val[M31] - val[M01] * val[M10] * val[M33] + val[M00] * val[M11] * val[M33];
		tmp[M23] = val[M03] * val[M11] * val[M20] - val[M01] * val[M13] * val[M20] - val[M03] * val[M10] * val[M21] + val[M00]
			* val[M13] * val[M21] + val[M01] * val[M10] * val[M23] - val[M00] * val[M11] * val[M23];
		tmp[M30] = val[M12] * val[M21] * val[M30] - val[M11] * val[M22] * val[M30] - val[M12] * val[M20] * val[M31] + val[M10]
			* val[M22] * val[M31] + val[M11] * val[M20] * val[M32] - val[M10] * val[M21] * val[M32];
		tmp[M31] = val[M01] * val[M22] * val[M30] - val[M02] * val[M21] * val[M30] + val[M02] * val[M20] * val[M31] - val[M00]
			* val[M22] * val[M31] - val[M01] * val[M20] * val[M32] + val[M00] * val[M21] * val[M32];
		tmp[M32] = val[M02] * val[M11] * val[M30] - val[M01] * val[M12] * val[M30] - val[M02] * val[M10] * val[M31] + val[M00]
			* val[M12] * val[M31] + val[M01] * val[M10] * val[M32] - val[M00] * val[M11] * val[M32];
		tmp[M33] = val[M01] * val[M12] * val[M20] - val[M02] * val[M11] * val[M20] + val[M02] * val[M10] * val[M21] - val[M00]
			* val[M12] * val[M21] - val[M01] * val[M10] * val[M22] + val[M00] * val[M11] * val[M22];

		float inv_det = 1.0f / l_det;
		val[M00] = tmp[M00] * inv_det;
		val[M01] = tmp[M01] * inv_det;
		val[M02] = tmp[M02] * inv_det;
		val[M03] = tmp[M03] * inv_det;
		val[M10] = tmp[M10] * inv_det;
		val[M11] = tmp[M11] * inv_det;
		val[M12] = tmp[M12] * inv_det;
		val[M13] = tmp[M13] * inv_det;
		val[M20] = tmp[M20] * inv_det;
		val[M21] = tmp[M21] * inv_det;
		val[M22] = tmp[M22] * inv_det;
		val[M23] = tmp[M23] * inv_det;
		val[M30] = tmp[M30] * inv_det;
		val[M31] = tmp[M31] * inv_det;
		val[M32] = tmp[M32] * inv_det;
		val[M33] = tmp[M33] * inv_det;
		return true;
	}

	static inline void matrix4_mulVec(float* mat, float* vec) {
		float x = vec[0] * mat[M00] + vec[1] * mat[M01] + vec[2] * mat[M02] + mat[M03];
		float y = vec[0] * mat[M10] + vec[1] * mat[M11] + vec[2] * mat[M12] + mat[M13];
		float z = vec[0] * mat[M20] + vec[1] * mat[M21] + vec[2] * mat[M22] + mat[M23];
		vec[0] = x;
		vec[1] = y;
		vec[2] = z;
	}
	
	static inline void matrix4_proj(float* mat, float* vec) {
		float inv_w = 1.0f / (vec[0] * mat[M30] + vec[1] * mat[M31] + vec[2] * mat[M32] + mat[M33]);
		float x = (vec[0] * mat[M00] + vec[1] * mat[M01] + vec[2] * mat[M02] + mat[M03]) * inv_w;
		float y = (vec[0] * mat[M10] + vec[1] * mat[M11] + vec[2] * mat[M12] + mat[M13]) * inv_w; 
		float z = (vec[0] * mat[M20] + vec[1] * mat[M21] + vec[2] * mat[M22] + mat[M23]) * inv_w;
		vec[0] = x;
		vec[1] = y;
		vec[2] = z;
	}
	
	static inline void matrix4_rot(float* mat, float* vec) {
		float x = vec[0] * mat[M00] + vec[1] * mat[M01] + vec[2] * mat[M02];
		float y = vec[0] * mat[M10] + vec[1] * mat[M11] + vec[2] * mat[M12];
		float z = vec[0] * mat[M20] + vec[1] * mat[M21] + vec[2] * mat[M22];
		vec[0] = x;
		vec[1] = y;
		vec[2] = z;
	}
	 */

        /** Multiplies the matrix mata with matrix matb, storing the result in mata. The arrays are assumed to hold 4x4 column major
         * matrices as you can get from [Matrix4.val]. This is the same as [Matrix4.mul].
         *
         * @param mata the first matrix.
         * @param matb the second matrix.
         */
        external fun mul(mata: FloatArray, matb: FloatArray) /*-{ }-*/  /*
		matrix4_mul(mata, matb);
	*/

        /** Multiplies the vector with the given matrix. The matrix array is assumed to hold a 4x4 column major matrix as you can get
         * from [Matrix4.val]. The vector array is assumed to hold a 3-component vector, with x being the first element, y being
         * the second and z being the last component. The result is stored in the vector array. This is the same as
         * [Vector3.mul].
         * @param mat the matrix
         * @param vec the vector.
         */
        external fun mulVec(mat: FloatArray, vec: FloatArray) /*-{ }-*/  /*
		matrix4_mulVec(mat, vec);
	*/

        /** Multiplies the vectors with the given matrix. The matrix array is assumed to hold a 4x4 column major matrix as you can get
         * from [Matrix4.val]. The vectors array is assumed to hold 3-component vectors. Offset specifies the offset into the
         * array where the x-component of the first vector is located. The numVecs parameter specifies the number of vectors stored in
         * the vectors array. The stride parameter specifies the number of floats between subsequent vectors and must be >= 3. This is
         * the same as [Vector3.mul] applied to multiple vectors.
         *
         * @param mat the matrix
         * @param vecs the vectors
         * @param offset the offset into the vectors array
         * @param numVecs the number of vectors
         * @param stride the stride between vectors in floats
         */
        external fun mulVec(mat: FloatArray, vecs: FloatArray, offset: Int, numVecs: Int, stride: Int) /*-{ }-*/  /*
		float* vecPtr = vecs + offset;
		for(int i = 0; i < numVecs; i++) {
			matrix4_mulVec(mat, vecPtr);
			vecPtr += stride;
		}
	*/

        /** Multiplies the vector with the given matrix, performing a division by w. The matrix array is assumed to hold a 4x4 column
         * major matrix as you can get from [Matrix4.val]. The vector array is assumed to hold a 3-component vector, with x being
         * the first element, y being the second and z being the last component. The result is stored in the vector array. This is the
         * same as [Vector3.prj].
         * @param mat the matrix
         * @param vec the vector.
         */
        external fun prj(mat: FloatArray, vec: FloatArray) /*-{ }-*/  /*
		matrix4_proj(mat, vec);
	*/

        /** Multiplies the vectors with the given matrix, , performing a division by w. The matrix array is assumed to hold a 4x4 column
         * major matrix as you can get from [Matrix4.val]. The vectors array is assumed to hold 3-component vectors. Offset
         * specifies the offset into the array where the x-component of the first vector is located. The numVecs parameter specifies
         * the number of vectors stored in the vectors array. The stride parameter specifies the number of floats between subsequent
         * vectors and must be >= 3. This is the same as [Vector3.prj] applied to multiple vectors.
         *
         * @param mat the matrix
         * @param vecs the vectors
         * @param offset the offset into the vectors array
         * @param numVecs the number of vectors
         * @param stride the stride between vectors in floats
         */
        external fun prj(mat: FloatArray, vecs: FloatArray, offset: Int, numVecs: Int, stride: Int) /*-{ }-*/  /*
		float* vecPtr = vecs + offset;
		for(int i = 0; i < numVecs; i++) {
			matrix4_proj(mat, vecPtr);
			vecPtr += stride;
		}
	*/

        /** Multiplies the vector with the top most 3x3 sub-matrix of the given matrix. The matrix array is assumed to hold a 4x4 column
         * major matrix as you can get from [Matrix4.val]. The vector array is assumed to hold a 3-component vector, with x being
         * the first element, y being the second and z being the last component. The result is stored in the vector array. This is the
         * same as [Vector3.rot].
         * @param mat the matrix
         * @param vec the vector.
         */
        external fun rot(mat: FloatArray, vec: FloatArray) /*-{ }-*/  /*
		matrix4_rot(mat, vec);
	*/

        /** Multiplies the vectors with the top most 3x3 sub-matrix of the given matrix. The matrix array is assumed to hold a 4x4
         * column major matrix as you can get from [Matrix4.val]. The vectors array is assumed to hold 3-component vectors.
         * Offset specifies the offset into the array where the x-component of the first vector is located. The numVecs parameter
         * specifies the number of vectors stored in the vectors array. The stride parameter specifies the number of floats between
         * subsequent vectors and must be >= 3. This is the same as [Vector3.rot] applied to multiple vectors.
         *
         * @param mat the matrix
         * @param vecs the vectors
         * @param offset the offset into the vectors array
         * @param numVecs the number of vectors
         * @param stride the stride between vectors in floats
         */
        external fun rot(mat: FloatArray, vecs: FloatArray, offset: Int, numVecs: Int, stride: Int) /*-{ }-*/  /*
		float* vecPtr = vecs + offset;
		for(int i = 0; i < numVecs; i++) {
			matrix4_rot(mat, vecPtr);
			vecPtr += stride;
		}
	*/

        /** Computes the inverse of the given matrix. The matrix array is assumed to hold a 4x4 column major matrix as you can get from
         * [Matrix4.val].
         * @param values the matrix values.
         * @return false in case the inverse could not be calculated, true otherwise.
         */
        external fun inv(values: FloatArray) /*-{ }-*/: Boolean  /*
		return matrix4_inv(values);
	*/

        /** Computes the determinante of the given matrix. The matrix array is assumed to hold a 4x4 column major matrix as you can get
         * from [Matrix4.val].
         * @param values the matrix values.
         * @return the determinante.
         */
        external fun det(values: FloatArray) /*-{ }-*/: Float  /*
		return matrix4_det(values);
	*/
    }
}
