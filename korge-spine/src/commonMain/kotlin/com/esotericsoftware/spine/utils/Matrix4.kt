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

package com.esotericsoftware.spine.utils

import com.soywiz.kmem.*
import kotlin.math.*

/** Encapsulates a [column major](http://en.wikipedia.org/wiki/Row-major_order#Column-major_order) 4 by 4 matrix. Like
 * the [Vector3] class it allows the chaining of methods by returning a reference to itself. For example:
 *
 * <pre>
 * Matrix4 mat = new Matrix4().trn(position).mul(camera.combined);
</pre> *
 *
 * @author badlogicgames@gmail.com
 */
class Matrix4 {

    val `val` = FloatArray(16)

    /** @return the backing float array
     */
    val values = `val`

    /** @return the squared scale factor on the X axis
     */
    val scaleXSquared: Float
        get() = values[M00] * values[M00] + values[M01] * values[M01] + values[M02] * values[M02]

    /** @return the squared scale factor on the Y axis
     */
    val scaleYSquared: Float
        get() = values[M10] * values[M10] + values[M11] * values[M11] + values[M12] * values[M12]

    /** @return the squared scale factor on the Z axis
     */
    val scaleZSquared: Float
        get() = values[M20] * values[M20] + values[M21] * values[M21] + values[M22] * values[M22]

    /** Returns true if the value is zero (using the default tolerance as upper bound)  */
    private fun isZero(value: Float): Boolean {
        return abs(value) <= MathUtils.FLOAT_ROUNDING_ERROR
    }


    /** @return the scale factor on the X axis (non-negative)
     */
    val scaleX: Float
        get() = if (isZero(values[M01]) && isZero(values[M02]))
            abs(values[M00])
        else
            sqrt(scaleXSquared.toDouble()).toFloat()

    /** @return the scale factor on the Y axis (non-negative)
     */
    val scaleY: Float
        get() = if (isZero(values[M10]) && isZero(values[M12]))
            abs(values[M11])
        else
            sqrt(scaleYSquared.toDouble()).toFloat()

    /** @return the scale factor on the X axis (non-negative)
     */
    val scaleZ: Float
        get() = if (isZero(values[M20]) && isZero(values[M21]))
            abs(values[M22])
        else
            sqrt(scaleZSquared.toDouble()).toFloat()

    /** Constructs an identity matrix  */
    constructor() {
        values[M00] = 1f
        values[M11] = 1f
        values[M22] = 1f
        values[M33] = 1f
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
        arraycopy(values, 0, this.values, 0, this.values.size)
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

    override fun toString(): String {
        return ("[" + values[M00] + "|" + values[M01] + "|" + values[M02] + "|" + values[M03] + "]\n" + "[" + values[M10] + "|" + values[M11] + "|"
                + values[M12] + "|" + values[M13] + "]\n" + "[" + values[M20] + "|" + values[M21] + "|" + values[M22] + "|" + values[M23] + "]\n" + "["
                + values[M30] + "|" + values[M31] + "|" + values[M32] + "|" + values[M33] + "]\n")
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


    // @on

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

    companion object {
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
        fun mul(mata: FloatArray, matb: FloatArray) {
            TODO()
        }

    }
}
