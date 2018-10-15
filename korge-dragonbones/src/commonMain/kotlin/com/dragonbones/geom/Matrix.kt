package com.dragonbones.geom

import com.soywiz.kds.*
import com.soywiz.korma.*
import kotlin.math.*

/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2012-2018 DragonBones team and other contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/**
 * - 2D Transform matrix.
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 2D 转换矩阵。
 * @version DragonBones 3.0
 * @language zh_CN
 */
class Matrix
/**
 * @private
 */(
	/**
	 * - 缩放或旋转图像时影响像素沿 x 轴定位的值。
	 * @default 1.0
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var a: Float = 1f,
	/**
	 * - 旋转或倾斜图像时影响像素沿 y 轴定位的值。
	 * @default 0.0
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var b: Float = 0f,
	/**
	 * - 旋转或倾斜图像时影响像素沿 x 轴定位的值。
	 * @default 0.0
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var c: Float = 0f,
	/**
	 * - 缩放或旋转图像时影响像素沿 y 轴定位的值。
	 * @default 1.0
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var d: Float = 1f,
	/**
	 * - 沿 x 轴平移每个点的距离。
	 * @default 0.0
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var tx: Float = 0f,
	/**
	 * - 沿 y 轴平移每个点的距离。
	 * @default 0.0
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var ty: Float = 0f
) {
	/**
	 * - The value that affects the positioning of pixels along the x axis when scaling or rotating an image.
	 * @default 1.0
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - The value that affects the positioning of pixels along the y axis when rotating or skewing an image.
	 * @default 0.0
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - The value that affects the positioning of pixels along the x axis when rotating or skewing an image.
	 * @default 0.0
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - The value that affects the positioning of pixels along the y axis when scaling or rotating an image.
	 * @default 1.0
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - The distance by which to translate each point along the x axis.
	 * @default 0.0
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - The distance by which to translate each point along the y axis.
	 * @default 0.0
	 * @version DragonBones 3.0
	 * @language en_US
	 */

	override fun toString(): String {
		return "[object dragonBones.Matrix] a:" + this.a + " b:" + this.b + " c:" + this.c + " d:" + this.d + " tx:" + this.tx + " ty:" + this.ty
	}

	/**
	 * @private
	 */
	fun copyFrom(value: Matrix): Matrix {
		this.a = value.a
		this.b = value.b
		this.c = value.c
		this.d = value.d
		this.tx = value.tx
		this.ty = value.ty

		return this
	}

	fun setTo(a: Float, b: Float, c: Float, d: Float, tx: Float, ty: Float) = this.apply {
		this.a = a
		this.b = b
		this.c = c
		this.d = d
		this.tx = tx
		this.ty = ty
	}

	/**
	 * @private
	 */
	fun copyFromArray(value: FloatArray, offset: Int = 0): Matrix = setTo(
		value[offset + 0], value[offset + 1], value[offset + 2],
		value[offset + 3], value[offset + 4], value[offset + 5]
	)

	fun copyFromArray(value: DoubleArray, offset: Int = 0): Matrix = setTo(
		value[offset + 0].toFloat(), value[offset + 1].toFloat(), value[offset + 2].toFloat(),
		value[offset + 3].toFloat(), value[offset + 4].toFloat(), value[offset + 5].toFloat()
	)

	/**
	 * - Convert to unit matrix.
	 * The resulting matrix has the following properties: a=1, b=0, c=0, d=1, tx=0, ty=0.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 转换为单位矩阵。
	 * 该矩阵具有以下属性：a=1、b=0、c=0、d=1、tx=0、ty=0。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun identity(): Matrix {
		this.a = 1f
		this.b = 0f
		this.d = 1f
		this.c = 0f
		this.tx = 0f
		this.ty = 0f

		return this
	}
	/**
	 * - Multiplies the current matrix with another matrix.
	 * @param value - The matrix that needs to be multiplied.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 将当前矩阵与另一个矩阵相乘。
	 * @param value - 需要相乘的矩阵。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun concat(value: Matrix): Matrix {
		var aA = this.a * value.a
		var bA = 0f
		var cA = 0f
		var dA = this.d * value.d
		var txA = this.tx * value.a + value.tx
		var tyA = this.ty * value.d + value.ty

		if (this.b != 0f || this.c != 0f) {
			aA += this.b * value.c
			bA += this.b * value.d
			cA += this.c * value.a
			dA += this.c * value.b
		}

		if (value.b != 0f || value.c != 0f) {
			bA += this.a * value.b
			cA += this.d * value.c
			txA += this.ty * value.c
			tyA += this.tx * value.b
		}

		this.a = aA
		this.b = bA
		this.c = cA
		this.d = dA
		this.tx = txA
		this.ty = tyA

		return this
	}
	/**
	 * - Convert to inverse matrix.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 转换为逆矩阵。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun invert(): Matrix {
		var aA = this.a
		var bA = this.b
		var cA = this.c
		var dA = this.d
		val txA = this.tx
		val tyA = this.ty

		if (bA == 0f && cA == 0f) {
			this.b = 0f
			this.c = 0f
			if (aA == 0f || dA == 0f) {
				this.a = 0f
				this.b = 0f
				this.tx = 0f
				this.ty = 0f
			} else {
				aA = 1f / aA
				dA = 1f / dA
				this.a = aA
				this.d = dA
				this.tx = -aA * txA
				this.ty = -dA * tyA
			}

			return this
		}

		var determinant = aA * dA - bA * cA
		if (determinant == 0f) {
			this.a = 1f
			this.b = 0f
			this.c = 0f
			this.d = 1f
			this.tx = 0f
			this.ty = 0f

			return this
		}

		determinant = 1f / determinant
		val k = dA * determinant
		this.a = k
		bA = -bA * determinant
		this.b = bA
		cA = -cA * determinant
		this.c = cA
		dA = aA * determinant
		this.d = dA
		this.tx = -(k * txA + cA * tyA)
		this.ty = -(bA * txA + dA * tyA)

		return this
	}
	/**
	 * - Apply a matrix transformation to a specific point.
	 * @param x - X coordinate.
	 * @param y - Y coordinate.
	 * @param result - The point after the transformation is applied.
	 * @param delta - Whether to ignore tx, ty's conversion to point.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 将矩阵转换应用于特定点。
	 * @param x - 横坐标。
	 * @param y - 纵坐标。
	 * @param result - 应用转换之后的点。
	 * @param delta - 是否忽略 tx，ty 对点的转换。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	fun transformPoint(x: Float, y: Float, result: XY, delta: Boolean = false): Unit {
		result.x = this.a * x + this.c * y
		result.y = this.b * x + this.d * y

		if (!delta) {
			result.x += this.tx
			result.y += this.ty
		}
	}

	fun transformPoint(x: Double, y: Double, result: XY, delta: Boolean = false): Unit = transformPoint(x.toFloat(), y.toFloat(), result, delta)

	fun transformX(x: Float, y: Float): Float = (this.a * x + this.c * y + this.tx)
	fun transformY(x: Float, y: Float): Float = (this.b * x + this.d * y + this.ty)

	/**
	 * @private
	 */
	fun transformRectangle(rectangle: Rectangle, delta: Boolean = false): Unit {
		val a = this.a
		val b = this.b
		val c = this.c
		val d = this.d
		val tx = if (delta) 0f else this.tx
		val ty = if (delta) 0f else this.ty

		val x = rectangle.x
		val y = rectangle.y
		val xMax = x + rectangle.width
		val yMax = y + rectangle.height

		var x0 = a * x + c * y + tx
		var y0 = b * x + d * y + ty
		var x1 = a * xMax + c * y + tx
		var y1 = b * xMax + d * y + ty
		var x2 = a * xMax + c * yMax + tx
		var y2 = b * xMax + d * yMax + ty
		var x3 = a * x + c * yMax + tx
		var y3 = b * x + d * yMax + ty

		var tmp = 0.0

		if (x0 > x1) {
			tmp = x0
			x0 = x1
			x1 = tmp
		}
		if (x2 > x3) {
			tmp = x2
			x2 = x3
			x3 = tmp
		}

		rectangle.x = floor(if (x0 < x2) x0 else x2)
		rectangle.width = ceil((if (x1 > x3) x1 else x3) - rectangle.x)

		if (y0 > y1) {
			tmp = y0
			y0 = y1
			y1 = tmp
		}
		if (y2 > y3) {
			tmp = y2
			y2 = y3
			y3 = tmp
		}

		rectangle.y = floor(if (y0 < y2) y0 else y2)
		rectangle.height = ceil((if (y1 > y3) y1 else y3) - rectangle.y)
	}

	fun toMatrix2d(m: Matrix2d) {
		m.a = this.a.toDouble()
		m.b = this.b.toDouble()
		m.c = this.c.toDouble()
		m.d = this.d.toDouble()
		m.tx = this.tx.toDouble()
		m.ty = this.ty.toDouble()
	}
}
