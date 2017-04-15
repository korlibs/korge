package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter

/**
 * An inverse kinematics objects which defines a constraint for a [IKResolver].

 * @author Trixt0r
 */
class IKObject
/**
 * Creates a new IKObject with the given constraints.
 * @param x x coordinate constraint
 * *
 * @param y y coordinate constraint
 * *
 * @param length the chain length constraint.
 * *
 * @param iterations the number of iterations.
 */
(x: Float, y: Float, length: Int, iterations: Int) : Point(x, y) {

	/**
	 * Returns the current set chain length.
	 * @return the chain length
	 */
	var chainLength: Int = 0; internal set
	internal var iterations: Int = 0

	init {
		this.setLength(length)
		this.setIterations(iterations)
	}

	/**
	 * Sets the chain length of this ik object.
	 * The chain length indicates how many parent bones should get affected, when a [IKResolver] resolves the constraints.
	 * @param chainLength the chain length
	 * *
	 * @return this ik object for chained operations
	 * *
	 * @throws SpriterException if the chain length is smaller than 0
	 */
	fun setLength(chainLength: Int): IKObject {
		if (chainLength < 0) throw SpriterException("The chain has to be at least 0!")
		this.chainLength = chainLength
		return this
	}

	/**
	 * Sets the number of iterations.
	 * The more iterations a [IKResolver] is asked to do, the more precise the result will be.
	 * @param iterations number of iterations
	 * *
	 * @return this ik object for chained operations
	 * *
	 * @throws SpriterException if the number of iterations is smaller than 0
	 */
	fun setIterations(iterations: Int): IKObject {
		if (iterations < 0) throw SpriterException("The number of iterations has to be at least 1!")
		this.iterations = iterations
		return this
	}

	/**
	 * Returns the current set number of iterations.
	 * @return the number of iterations
	 */
	fun getIterations(): Int {
		return this.iterations
	}

}
