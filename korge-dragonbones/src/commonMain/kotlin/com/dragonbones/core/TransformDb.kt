package com.dragonbones.core

import com.soywiz.korma.geom.*
import kotlin.math.*

/**
 * - 2D 变换。
 * @version DragonBones 3.0
 * @language zh_CN
 */
class TransformDb
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
	override var xf: Float = 0f,
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
	override var yf: Float = 0f,
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
) : XYf {
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
		return "[object dragonBones.Transform] x:" + this.xf + " y:" + this.yf + " skewX:" + this.skew * 180f / PI + " skewY:" + this.rotation * 180f / PI + " scaleX:" + this.scaleX + " scaleY:" + this.scaleY
	}

	fun setTo(x: Float, y: Float, skew: Float, rotation: Float, scaleX: Float, scaleY: Float): TransformDb {
		this.xf = x
		this.yf = y
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
	fun copyFrom(value: TransformDb): TransformDb = value.apply { this@TransformDb.setTo(xf, yf, skew, rotation, scaleX, scaleY) }

	/**
	 * @private
	 */
	fun identity(): TransformDb = setTo(0f, 0f, 0f, 0f, 1f, 1f)

	/**
	 * @private
	 */
	fun add(value: TransformDb): TransformDb = setTo(
		xf + value.xf,
		yf + value.yf,
		skew + value.skew,
		rotation + value.rotation,
		scaleX * value.scaleX,
		scaleY * value.scaleY
	)

	/**
	 * @private
	 */
	fun minus(value: TransformDb): TransformDb = setTo(
		xf - value.xf,
		yf - value.yf,
		skew - value.skew,
		rotation - value.rotation,
		scaleX / value.scaleX,
		scaleY / value.scaleY
	)

	/**
	 * @private
	 */
	fun fromMatrix(matrix: Matrix): TransformDb {
		val backupScaleX = this.scaleX
		val backupScaleY = this.scaleY
		val PI_Q = TransformDb.PI_Q

		this.xf = matrix.txf
		this.yf = matrix.tyf
		this.rotation = atan(matrix.bf / matrix.af)
		var skewX = atan(-matrix.cf / matrix.df)

		this.scaleX = if (this.rotation > -PI_Q && this.rotation < PI_Q) matrix.af / cos(this.rotation) else matrix.bf / sin(this.rotation)
		this.scaleY = if (skewX > -PI_Q && skewX < PI_Q) matrix.df / cos(skewX) else -matrix.cf / sin(skewX)

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
	fun toMatrix(matrix: Matrix): TransformDb {
		if (this.rotation == 0f) {
			matrix.af = 1f
			matrix.bf = 0f
		}
		else {
			matrix.af = cos(this.rotation)
			matrix.bf = sin(this.rotation)
		}

		if (this.skew == 0f) {
			matrix.cf = -matrix.bf
			matrix.df = matrix.af
		}
		else {
			matrix.cf = -sin(this.skew + this.rotation)
			matrix.df = cos(this.skew + this.rotation)
		}

		if (this.scaleX != 1f) {
			matrix.af *= this.scaleX
			matrix.bf *= this.scaleX
		}

		if (this.scaleY != 1f) {
			matrix.cf *= this.scaleY
			matrix.df *= this.scaleY
		}

		matrix.txf = this.xf
		matrix.tyf = this.yf

		return this
	}
}
