package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter

/**
 * Represents a dimension in a 2D space.
 * A dimension has a width and a height.
 * @author Trixt0r
 */
data class Dimension(
	var width: Float,
	var height: Float
) {
	/**
	 * Creates a new dimension with the given size.
	 * @param size the size
	 */
	constructor(size: Dimension) : this(size.width, size.height)

	/**
	 * Sets the size of this dimension to the given size.
	 * @param width the width of the dimension
	 * *
	 * @param height the height of the dimension
	 */
	fun set(width: Float, height: Float) {
		this.width = width
		this.height = height
	}

	/**
	 * Sets the size of this dimension to the given size.
	 * @param size the size
	 */
	fun set(size: Dimension) {
		this.set(size.width, size.height)
	}

	override fun toString(): String = "[${width}x$height]"

}
