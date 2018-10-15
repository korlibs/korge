package com.dragonbones.geom

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
 * - 2D Transform.
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 2D 变换。
 * @version DragonBones 3.0
 * @language zh_CN
 */
class Transform
/**
 * @private
 */(
	/**
	 * - 水平位移。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */

	/**
	 * - Horizontal translate.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	override var x: Float = 0f,
	/**
	 * - 垂直位移。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	/**
	 * - Vertical translate.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	override var y: Float = 0f,
	/**
	 * - 倾斜。 （以弧度为单位）
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	/**
	 * - Skew. (In radians)
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	var skew: Float = 0f,
	/**
	 * - 旋转。 （以弧度为单位）
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	/**
	 * - rotation. (In radians)
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	var rotation: Float = 0f,
	/**
	 * - 水平缩放。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	/**
	 * - Horizontal Scaling.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	var scaleX: Float = 1f,
	/**
	 * - 垂直缩放。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	/**
	 * - Vertical scaling.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	var scaleY: Float = 1f
) : XY {
	companion object {
		/**
		 * @private
		 */
		val PI: Float = kotlin.math.PI.toFloat()
		/**
		 * @private
		 */
		val PI_D: Float = PI * 2f
		/**
		 * @private
		 */
		val PI_H: Float = PI / 2f
		/**
		 * @private
		 */
		val PI_Q: Float = PI / 4f
		/**
		 * @private
		 */
		val RAD_DEG: Float = 180f / PI
		/**
		 * @private
		 */
		val DEG_RAD: Float = PI / 180f

		/**
		 * @private
		 */
		fun normalizeRadian(value: Double): Double {
			var value = (value + PI) % (PI * 2.0)
			value += if (value > 0f) -PI else PI

			return value
		}
	}

	override fun toString(): String {
		return "[object dragonBones.Transform] x:" + this.x + " y:" + this.y + " skewX:" + this.skew * 180f / PI + " skewY:" + this.rotation * 180f / PI + " scaleX:" + this.scaleX + " scaleY:" + this.scaleY
	}

	fun setTo(x: Float, y: Float, skew: Float, rotation: Float, scaleX: Float, scaleY: Float): Transform {
		this.x = x
		this.y = y
		this.skew = skew
		this.rotation = rotation
		this.scaleX = scaleX
		this.scaleY = scaleY

		if (x.isNaN() || y.isNaN() || skew.isNaN() || rotation.isNaN() || scaleX.isNaN() || scaleY.isNaN()) {
			error("WARNING! NaN detected in Transform")
		}

		return this
	}

	/**
	 * @private
	 */
	fun copyFrom(value: Transform): Transform = value.apply { this@Transform.setTo(x, y, skew, rotation, scaleX, scaleY) }

	/**
	 * @private
	 */
	fun identity(): Transform = setTo(0f, 0f, 0f, 0f, 1f, 1f)

	/**
	 * @private
	 */
	fun add(value: Transform): Transform = setTo(
		x + value.x,
		y + value.y,
		skew + value.skew,
		rotation + value.rotation,
		scaleX * value.scaleX,
		scaleY * value.scaleY
	)

	/**
	 * @private
	 */
	fun minus(value: Transform): Transform = setTo(
		x - value.x,
		y - value.y,
		skew - value.skew,
		rotation - value.rotation,
		scaleX / value.scaleX,
		scaleY / value.scaleY
	)

	/**
	 * @private
	 */
	fun fromMatrix(matrix: Matrix): Transform {
		val backupScaleX = this.scaleX
		val backupScaleY = this.scaleY
		val PI_Q = Transform.PI_Q

		this.x = matrix.tx
		this.y = matrix.ty
		this.rotation = atan(matrix.b / matrix.a)
		var skewX = atan(-matrix.c / matrix.d)

		this.scaleX = if (this.rotation > -PI_Q && this.rotation < PI_Q) matrix.a / cos(this.rotation) else matrix.b / sin(this.rotation)
		this.scaleY = if (skewX > -PI_Q && skewX < PI_Q) matrix.d / cos(skewX) else -matrix.c / sin(skewX)

		if (backupScaleX >= 0f && this.scaleX < 0f) {
			this.scaleX = -this.scaleX
			this.rotation = (this.rotation - PI).toFloat()
		}

		if (backupScaleY >= 0f && this.scaleY < 0f) {
			this.scaleY = -this.scaleY
			skewX -= PI
		}

		this.skew = skewX - this.rotation

		return this
	}

	/**
	 * @private
	 */
	fun toMatrix(matrix: Matrix): Transform {
		if (this.rotation == 0f) {
			matrix.a = 1f
			matrix.b = 0f
		}
		else {
			matrix.a = cos(this.rotation)
			matrix.b = sin(this.rotation)
		}

		if (this.skew == 0f) {
			matrix.c = -matrix.b
			matrix.d = matrix.a
		}
		else {
			matrix.c = -sin(this.skew + this.rotation)
			matrix.d = cos(this.skew + this.rotation)
		}

		if (this.scaleX != 1f) {
			matrix.a *= this.scaleX
			matrix.b *= this.scaleX
		}

		if (this.scaleY != 1f) {
			matrix.c *= this.scaleY
			matrix.d *= this.scaleY
		}

		matrix.tx = this.x
		matrix.ty = this.y

		return this
	}

	fun toMatrix2d(matrix: Matrix2d): Transform {
		if (this.rotation == 0f) {
			matrix.a = 1.0
			matrix.b = 0.0
		}
		else {
			matrix.a = cos(this.rotation).toDouble()
			matrix.b = sin(this.rotation).toDouble()
		}

		if (this.skew == 0f) {
			matrix.c = -matrix.b
			matrix.d = matrix.a
		}
		else {
			matrix.c = (-sin(this.skew + this.rotation)).toDouble()
			matrix.d = cos(this.skew + this.rotation).toDouble()
		}

		if (this.scaleX != 1f) {
			matrix.a *= this.scaleX
			matrix.b *= this.scaleX
		}

		if (this.scaleY != 1f) {
			matrix.c *= this.scaleY
			matrix.d *= this.scaleY
		}

		matrix.tx = this.x.toDouble()
		matrix.ty = this.y.toDouble()

		return this
	}
}
