package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter

import com.soywiz.korio.*
import com.soywiz.korma.geom.*
import kotlin.math.*

/**
 * A utility class which provides methods to calculate Spriter specific issues,
 * like linear interpolation and rotation around a parent object.
 * Other interpolation types are coming with the next releases of Spriter.

 * @author Trixt0r
 */

object Calculator {

	const val PI = kotlin.math.PI.toFloat()
	const val NO_SOLUTION = -1f

	/**
	 * Calculates the smallest difference between angle a and b.
	 * @param a first angle (in degrees)
	 * *
	 * @param b second angle (in degrees)
	 * *
	 * @return Smallest difference between a and b (between 180� and -180�).
	 */
	fun angleDifference(a: Float, b: Float): Float {
		return ((a - b) % 360 + 540) % 360 - 180
	}

	/**
	 * @param x1 x coordinate of first point.
	 * *
	 * @param y1 y coordinate of first point.
	 * *
	 * @param x2 x coordinate of second point.
	 * *
	 * @param y2 y coordinate of second point.
	 * *
	 * @return Angle between the two given points.
	 */
	fun angleBetween(x1: Float, y1: Float, x2: Float, y2: Float): Float {
		return Angle.degreesToRadians(atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())).toFloat()
	}

	/**
	 * @param x1 x coordinate of first point.
	 * *
	 * @param y1 y coordinate of first point.
	 * *
	 * @param x2 x coordinate of second point.
	 * *
	 * @param y2 y coordinate of second point.
	 * *
	 * @return Distance between the two given points.
	 */
	fun distanceBetween(x1: Float, y1: Float, x2: Float, y2: Float): Float {
		val xDiff = x2 - x1
		val yDiff = y2 - y1
		return sqrt(xDiff * xDiff + yDiff * yDiff)
	}

	/**
	 * Solves the equation a*x^3 + b*x^2 + c*x +d = 0.
	 * @param a
	 * *
	 * @param b
	 * *
	 * @param c
	 * *
	 * @param d
	 * *
	 * @return the solution of the cubic function if it belongs [0, 1], [.NO_SOLUTION] otherwise.
	 */
	fun solveCubic(a: Float, b: Float, c: Float, d: Float): Float {
		var b = b
		var c = c
		var d = d
		if (a == 0f) return solveQuadratic(b, c, d)
		if (d == 0f) return 0f

		b /= a
		c /= a
		d /= a
		val squaredB = squared(b)
		var q = (3f * c - squaredB) / 9f
		val r = (-27f * d + b * (9f * c - 2f * squaredB)) / 54f
		val disc = cubed(q) + squared(r)
		val term1 = b / 3f

		if (disc > 0) {
			val sqrtDisc = sqrt(disc)
			var s = r + sqrtDisc
			s = if (s < 0) -cubicRoot(-s) else cubicRoot(s)
			var t = r - sqrtDisc
			t = if (t < 0) -cubicRoot(-t) else cubicRoot(t)

			val result = -term1 + s + t
			if (result in 0.0..1.0) return result
		} else if (disc == 0f) {
			val r13 = if (r < 0) -cubicRoot(-r) else cubicRoot(r)

			var result = -term1 + 2f * r13
			if (result in 0.0..1.0) return result

			result = -(r13 + term1)
			if (result in 0.0..1.0) return result
		} else {
			q = -q
			var dum1 = q * q * q
			dum1 = acos(r / sqrt(dum1))
			val r13 = 2f * sqrt(q)

			var result = -term1 + r13 * cos(dum1 / 3f)
			if (result in 0.0..1.0) return result

			result = -term1 + r13 * cos((dum1 + 2f * PI) / 3f)
			if (result in 0.0..1.0) return result

			result = -term1 + r13 * cos((dum1 + 4f * PI) / 3f)
			if (result in 0.0..1.0) return result
		}

		return NO_SOLUTION
	}

	/**
	 * Solves the equation a*x^2 + b*x + c = 0
	 * @param a
	 * *
	 * @param b
	 * *
	 * @param c
	 * *
	 * @return the solution for the quadratic function if it belongs [0, 1], [.NO_SOLUTION] otherwise.
	 */
	fun solveQuadratic(a: Float, b: Float, c: Float): Float {
		val squaredB = squared(b)
		val twoA = 2 * a
		val fourAC = 4f * a * c
		val sqrt = sqrt(squaredB - fourAC)
		var result = (-b + sqrt) / twoA
		if (result >= 0 && result <= 1) return result

		result = (-b - sqrt) / twoA
		if (result >= 0 && result <= 1) return result

		return NO_SOLUTION
	}

	/**
	 * Returns the square of the given value.
	 * @param f the value
	 * *
	 * @return the square of the value
	 */
	fun squared(f: Float): Float {
		return f * f
	}

	/**
	 * Returns the cubed value of the given one.
	 * @param f the value
	 * *
	 * @return the cubed value
	 */
	fun cubed(f: Float): Float {
		return f * f * f
	}

	/**
	 * Returns the cubic root of the given value.
	 * @param f the value
	 * *
	 * @return the cubic root
	 */
	fun cubicRoot(f: Float): Float {
		return f.toDouble().pow((1f / 3f).toDouble()).toFloat()
	}

	/**
	 * Returns the square root of the given value.
	 * @param x the value
	 * *
	 * @return the square root
	 */
	fun sqrt(x: Float): Float {
		return kotlin.math.sqrt(x.toDouble()).toFloat()
	}

	/**
	 * Returns the arc cosine at the given value.
	 * @param x the value
	 * *
	 * @return the arc cosine
	 */
	fun acos(x: Float): Float {
		return kotlin.math.acos(x.toDouble()).toFloat()
	}

	private val SIN_BITS = 14 // 16KB. Adjust for accuracy.
	private val SIN_MASK = (-1 shl SIN_BITS).inv()
	private val SIN_COUNT = SIN_MASK + 1

	private val radFull = PI * 2
	private val degFull = 360f
	private val radToIndex = SIN_COUNT / radFull
	private val degToIndex = SIN_COUNT / degFull

	/** multiply by this to convert from radians to degrees  */
	val radiansToDegrees = 180f / PI
	val radDeg = radiansToDegrees
	/** multiply by this to convert from degrees to radians  */
	val degreesToRadians = PI / 180
	val degRad = degreesToRadians

	private object Sin {
		internal val table = FloatArray(SIN_COUNT).apply {
			for (i in 0 until SIN_COUNT)
				this[i] = kotlin.math.sin(((i + 0.5f) / SIN_COUNT * radFull).toDouble()).toFloat()
			var i = 0
			while (i < 360) {
				this[(i * degToIndex).toInt() and SIN_MASK] =
						kotlin.math.sin((i * degreesToRadians).toDouble()).toFloat()
				i += 90
			}
		}
	}

	/** Returns the sine in radians from a lookup table.  */
	fun sin(radians: Float): Float {
		return Sin.table[(radians * radToIndex).toInt() and SIN_MASK]
	}

	/** Returns the cosine in radians from a lookup table.  */
	fun cos(radians: Float): Float {
		return Sin.table[((radians + PI / 2) * radToIndex).toInt() and SIN_MASK]
	}

	/** Returns the sine in radians from a lookup table.  */
	fun sinDeg(degrees: Float): Float {
		return Sin.table[(degrees * degToIndex).toInt() and SIN_MASK]
	}

	/** Returns the cosine in radians from a lookup table.  */
	fun cosDeg(degrees: Float): Float {
		return Sin.table[((degrees + 90) * degToIndex).toInt() and SIN_MASK]
	}

}
