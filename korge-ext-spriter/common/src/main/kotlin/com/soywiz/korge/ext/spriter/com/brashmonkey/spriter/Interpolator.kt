package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter

import com.soywiz.korio.JvmStatic


/**
 * Utility class for various interpolation techniques, Spriter is using.
 * @author Trixt0r
 */
object Interpolator {

	@JvmStatic
	fun linear(a: Float, b: Float, t: Float): Float {
		return a + (b - a) * t
	}

	@JvmStatic
	fun linearAngle(a: Float, b: Float, t: Float): Float {
		return a + Calculator.angleDifference(b, a) * t
	}

	@JvmStatic
	fun quadratic(a: Float, b: Float, c: Float, t: Float): Float {
		return linear(linear(a, b, t), linear(b, c, t), t)
	}

	@JvmStatic
	fun quadraticAngle(a: Float, b: Float, c: Float, t: Float): Float {
		return linearAngle(linearAngle(a, b, t), linearAngle(b, c, t), t)
	}

	@JvmStatic
	fun cubic(a: Float, b: Float, c: Float, d: Float, t: Float): Float {
		return linear(quadratic(a, b, c, t), quadratic(b, c, d, t), t)
	}

	@JvmStatic
	fun cubicAngle(a: Float, b: Float, c: Float, d: Float, t: Float): Float {
		return linearAngle(quadraticAngle(a, b, c, t), quadraticAngle(b, c, d, t), t)
	}

	@JvmStatic
	fun quartic(a: Float, b: Float, c: Float, d: Float, e: Float, t: Float): Float {
		return linear(cubic(a, b, c, d, t), cubic(b, c, d, e, t), t)
	}

	@JvmStatic
	fun quarticAngle(a: Float, b: Float, c: Float, d: Float, e: Float, t: Float): Float {
		return linearAngle(cubicAngle(a, b, c, d, t), cubicAngle(b, c, d, e, t), t)
	}

	@JvmStatic
	fun quintic(a: Float, b: Float, c: Float, d: Float, e: Float, f: Float, t: Float): Float {
		return linear(quartic(a, b, c, d, e, t), quartic(b, c, d, e, f, t), t)
	}

	@JvmStatic
	fun quinticAngle(a: Float, b: Float, c: Float, d: Float, e: Float, f: Float, t: Float): Float {
		return linearAngle(quarticAngle(a, b, c, d, e, t), quarticAngle(b, c, d, e, f, t), t)
	}

	@JvmStatic
	fun bezier(t: Float, x1: Float, x2: Float, x3: Float, x4: Float): Float {
		return bezier0(t) * x1 + bezier1(t) * x2 + bezier2(t) * x3 + bezier3(t) * x4
	}

	private fun bezier0(t: Float): Float {
		val temp = t * t
		return -temp * t + 3 * temp - 3 * t + 1
	}

	private fun bezier1(t: Float): Float {
		val temp = t * t
		return 3f * t * temp - 6 * temp + 3 * t
	}

	private fun bezier2(t: Float): Float {
		val temp = t * t
		return -3f * temp * t + 3 * temp
	}

	private fun bezier3(t: Float): Float {
		return t * t * t
	}

}
