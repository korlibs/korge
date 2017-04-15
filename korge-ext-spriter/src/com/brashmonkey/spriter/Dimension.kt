package com.brashmonkey.spriter

/**
 * Represents a dimension in a 2D space.
 * A dimension has a width and a height.
 * @author Trixt0r
 */
class Dimension {

	var width: Float = 0.toFloat()
	var height: Float = 0.toFloat()

	/**
	 * Creates a new dimension with the given size.
	 * @param width the width of the dimension
	 * *
	 * @param height the height of the dimension
	 */
	constructor(width: Float, height: Float) {
		this[width] = height
	}

	/**
	 * Creates a new dimension with the given size.
	 * @param size the size
	 */
	constructor(size: Dimension) {
		this.set(size)
	}

	/**
	 * Sets the size of this dimension to the given size.
	 * @param width the width of the dimension
	 * *
	 * @param height the height of the dimension
	 */
	operator fun set(width: Float, height: Float) {
		this.width = width
		this.height = height
	}

	/**
	 * Sets the size of this dimension to the given size.
	 * @param size the size
	 */
	fun set(size: Dimension) {
		this[size.width] = size.height
	}

	override fun toString(): String {
		return "[" + width + "x" + height + "]"
	}

}
