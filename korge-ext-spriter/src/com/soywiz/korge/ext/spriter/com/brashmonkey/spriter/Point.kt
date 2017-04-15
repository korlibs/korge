package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter

/**
 * A utility class to keep the code short.
 * A point is essentially that what you would expect if you think about a point in a 2D space.
 * It holds an x and y value. You can [.translate], [.scale], [.rotate] and [.set] a point.
 * @author Trixt0r
 *
 * Creates a point at (x, y).
 * @param x the x coordinate
 * *
 * @param y the y coordinate
 */

open class Point(var x: Float = 0f, var y: Float = 0f) {
	/**
	 * Creates a point at the position of the given point.
	 * @param point the point to set this point at
	 */
	constructor(point: Point) : this(point.x, point.y)

	init {
		this.set(x, y)
	}

	/**
	 * Sets this point to the given coordinates.
	 * @param x the x coordinate
	 * *
	 * @param y the y coordinate
	 * *
	 * @return this point for chained operations
	 */
	fun set(x: Float, y: Float): Point = this.apply {
		this.x = x
		this.y = y
	}

	/**
	 * Adds the given amount to this point.
	 * @param x the amount in x direction to add
	 * *
	 * @param y the amount in y direction to add
	 * *
	 * @return this point for chained operations
	 */
	fun translate(x: Float, y: Float): Point = this.set(this.x + x, this.y + y)

	/**
	 * Scales this point by the given amount.
	 * @param x the scale amount in x direction
	 * *
	 * @param y the scale amount in y direction
	 * *
	 * @return this point for chained operations
	 */
	fun scale(x: Float, y: Float): Point = this.set(this.x * x, this.y * y)

	/**
	 * Sets this point to the given point.
	 * @param point the new coordinates
	 * *
	 * @return this point for chained operations
	 */
	fun set(point: Point): Point = this.set(point.x, point.y)

	/**
	 * Adds the given amount to this point.
	 * @param amount the amount to add
	 * *
	 * @return this point for chained operations
	 */
	fun translate(amount: Point): Point = this.translate(amount.x, amount.y)

	/**
	 * Scales this point by the given amount.
	 * @param amount the amount to scale
	 * *
	 * @return this point for chained operations
	 */
	fun scale(amount: Point): Point = this.scale(amount.x, amount.y)

	/**
	 * Rotates this point around (0,0) by the given amount of degrees.
	 * @param degrees the angle to rotate this point
	 * *
	 * @return this point for chained operations
	 */
	fun rotate(degrees: Float): Point {
		if (x != 0f || y != 0f) {
			val cos = Calculator.cosDeg(degrees)
			val sin = Calculator.sinDeg(degrees)

			val xx = x * cos - y * sin
			val yy = x * sin + y * cos

			this.x = xx
			this.y = yy
		}
		return this
	}

	/**
	 * Returns a copy of this point with the current set values.
	 * @return a copy of this point
	 */
	fun copy(): Point = Point(x, y)

	override fun toString(): String = "[$x,$y]"

}
/**
 * Creates a point at (0,0).
 */
