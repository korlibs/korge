package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter

/**
 * Represents a curve in a Spriter SCML file.
 * An instance of this class is responsible for tweening given data.
 * The most important method of this class is [.tween].
 * Curves can be changed with sub curves [Curve.subCurve].
 * @author Trixt0r
 */
class Curve
/**
 * Creates a new curve with the given type and sub cuve.
 * @param type the curve type
 * *
 * @param subCurve the sub curve. Can be `null`
 */
@JvmOverloads constructor(type: Type = Curve.Type.Linear,
						  /**
						   * The sub curve of this curve, which can be `null`.
						   */
						  var subCurve: Curve? = null) {

	/**
	 * Represents a curve type in a Spriter SCML file.
	 * @author Trixt0r
	 */
	enum class Type {
		Instant, Linear, Quadratic, Cubic, Quartic, Quintic, Bezier
	}

	/**
	 * Returns the type of this curve.
	 * @return the curve type
	 */
	/**
	 * Sets the type of this curve.
	 * @param type the curve type.
	 * *
	 * @throws SpriterException if the type is `null`
	 */
	var type: Type? = null
		set(type) {
			if (type == null) throw SpriterException("The type of a curve cannot be null!")
			field = type
		}
	/**
	 * The constraints of a curve which will affect a curve of the types different from [Type.Linear] and [Type.Instant].
	 */
	val constraints = Constraints(0f, 0f, 0f, 0f)

	init {
		this.type = type
	}


	private var lastCubicSolution = 0f
	/**
	 * Returns a new value based on the given values.
	 * Tweens the weight with the set sub curve.
	 * @param a the start value
	 * *
	 * @param b the end value
	 * *
	 * @param t the weight which lies between 0.0 and 1.0
	 * *
	 * @return tweened value
	 */
	fun tween(a: Float, b: Float, t: Float): Float {
		var t = t
		t = tweenSub(0f, 1f, t)
		when (this.type) {
			Type.Instant -> return a
			Type.Linear -> return Interpolator.linear(a, b, t)
			Type.Quadratic -> return Interpolator.quadratic(a, Interpolator.linear(a, b, constraints.c1), b, t)
			Type.Cubic -> return Interpolator.cubic(a, Interpolator.linear(a, b, constraints.c1), Interpolator.linear(a, b, constraints.c2), b, t)
			Type.Quartic -> return Interpolator.quartic(a, Interpolator.linear(a, b, constraints.c1), Interpolator.linear(a, b, constraints.c2), Interpolator.linear(a, b, constraints.c3), b, t)
			Type.Quintic -> return Interpolator.quintic(a, Interpolator.linear(a, b, constraints.c1), Interpolator.linear(a, b, constraints.c2), Interpolator.linear(a, b, constraints.c3), Interpolator.linear(a, b, constraints.c4), b, t)
			Type.Bezier -> {
				var cubicSolution = Calculator.solveCubic(3f * (constraints.c1 - constraints.c3) + 1f, 3f * (constraints.c3 - 2f * constraints.c1), 3f * constraints.c1, -t)
				if (cubicSolution == Calculator.NO_SOLUTION)
					cubicSolution = lastCubicSolution
				else
					lastCubicSolution = cubicSolution
				return Interpolator.linear(a, b, Interpolator.bezier(cubicSolution, 0f, constraints.c2, constraints.c4, 1f))
			}
			else -> return Interpolator.linear(a, b, t)
		}
	}

	/**
	 * Interpolates the given two points with the given weight and saves the result in the target point.
	 * @param a the start point
	 * *
	 * @param b the end point
	 * *
	 * @param t the weight which lies between 0.0 and 1.0
	 * *
	 * @param target the target point to save the result in
	 */
	fun tweenPoint(a: Point, b: Point, t: Float, target: Point) {
		target.set(this.tween(a.x, b.x, t), this.tween(a.y, b.y, t))
	}

	private fun tweenSub(a: Float, b: Float, t: Float): Float {
		if (this.subCurve != null)
			return subCurve!!.tween(a, b, t)
		else
			return t
	}

	/**
	 * Returns a tweened angle based on the given angles, weight and the spin.
	 * @param a the start angle
	 * *
	 * @param b the end angle
	 * *
	 * @param t the weight which lies between 0.0 and 1.0
	 * *
	 * @param spin the spin, which is either 0, 1 or -1
	 * *
	 * @return tweened angle
	 */
	fun tweenAngle(a: Float, b: Float, t: Float, spin: Int): Float {
		var b = b
		if (spin > 0) {
			if (b - a < 0)
				b += 360f
		} else if (spin < 0) {
			if (b - a > 0)
				b -= 360f
		} else
			return a

		return tween(a, b, t)
	}

	/**
	 * @see {@link .tween
	 */
	fun tweenAngle(a: Float, b: Float, t: Float): Float {
		var t = t
		t = tweenSub(0f, 1f, t)
		when (this.type) {
			Type.Instant -> return a
			Type.Linear -> return Interpolator.linearAngle(a, b, t)
			Type.Quadratic -> return Interpolator.quadraticAngle(a, Interpolator.linearAngle(a, b, constraints.c1), b, t)
			Type.Cubic -> return Interpolator.cubicAngle(a, Interpolator.linearAngle(a, b, constraints.c1), Interpolator.linearAngle(a, b, constraints.c2), b, t)
			Type.Quartic -> return Interpolator.quarticAngle(a, Interpolator.linearAngle(a, b, constraints.c1), Interpolator.linearAngle(a, b, constraints.c2), Interpolator.linearAngle(a, b, constraints.c3), b, t)
			Type.Quintic -> return Interpolator.quinticAngle(a, Interpolator.linearAngle(a, b, constraints.c1), Interpolator.linearAngle(a, b, constraints.c2), Interpolator.linearAngle(a, b, constraints.c3), Interpolator.linearAngle(a, b, constraints.c4), b, t)
			Type.Bezier -> {
				var cubicSolution = Calculator.solveCubic(3f * (constraints.c1 - constraints.c3) + 1f, 3f * (constraints.c3 - 2f * constraints.c1), 3f * constraints.c1, -t)
				if (cubicSolution == Calculator.NO_SOLUTION)
					cubicSolution = lastCubicSolution
				else
					lastCubicSolution = cubicSolution
				return Interpolator.linearAngle(a, b, Interpolator.bezier(cubicSolution, 0f, constraints.c2, constraints.c4, 1f))
			}
			else -> return Interpolator.linearAngle(a, b, t)
		}
	}

	override fun toString(): String {
		return javaClass.simpleName + "|[" + this.type + ":" + constraints + ", subCurve: " + subCurve + "]"
	}

	/**
	 * Represents constraints for a curve.
	 * Constraints are important for curves which have a order higher than 1.
	 * @author Trixt0r
	 */
	class Constraints(c1: Float, c2: Float, c3: Float, c4: Float) {
		var c1: Float = 0.toFloat()
		var c2: Float = 0.toFloat()
		var c3: Float = 0.toFloat()
		var c4: Float = 0.toFloat()

		init {
			this[c1, c2, c3] = c4
		}

		operator fun set(c1: Float, c2: Float, c3: Float, c4: Float) {
			this.c1 = c1
			this.c2 = c2
			this.c3 = c3
			this.c4 = c4
		}

		override fun toString(): String {
			return javaClass.simpleName + "| [c1:" + c1 + ", c2:" + c2 + ", c3:" + c3 + ", c4:" + c4 + "]"
		}
	}

	companion object {

		/**
		 * Returns a curve type based on the given curve name.
		 * @param name the name of the curve
		 * *
		 * @return the curve type. [Type.Linear] is returned as a default type.
		 */
		fun getType(name: String): Type {
			if (name == "instant")
				return Type.Instant
			else if (name == "quadratic")
				return Type.Quadratic
			else if (name == "cubic")
				return Type.Cubic
			else if (name == "quartic")
				return Type.Quartic
			else if (name == "quintic")
				return Type.Quintic
			else if (name == "bezier")
				return Type.Bezier
			else
				return Type.Linear
		}
	}

}
/**
 * Creates a new linear curve.
 */
/**
 * Creates a new curve with the given type.
 * @param type the curve type
 */
