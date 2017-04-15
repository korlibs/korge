package com.brashmonkey.spriter

/**
 * Represents a 2D rectangle with left, top, right and bottom bounds.
 * A rectangle is responsible for calculating its own size and checking if a point is inside it or if it is intersecting with another rectangle.
 * @author Trixt0r
 */
/**
 * Creates a rectangle with the given bounds.
 * @param left left bounding
 * *
 * @param top top bounding
 * *
 * @param right right bounding
 * *
 * @param bottom bottom bounding
 */
class Rectangle(var left: Float = 0f, var top: Float = 0f, var right: Float = 0f, var bottom: Float = 0f) {

	/**
	 * Belongs to the bounds of this rectangle.
	 */

	/**
	 * The size of this rectangle.
	 */
	val size: Dimension

	init {
		this.set(left, top, right, bottom)
		this.size = Dimension(0f, 0f)
		this.calculateSize()
	}

	/**
	 * Creates a rectangle with the bounds of the given rectangle.
	 * @param rect rectangle containing the bounds.
	 */
	constructor(rect: Rectangle) : this(rect.left, rect.top, rect.right, rect.bottom) {
	}

	/**
	 * Returns whether the given point (x,y) is inside this rectangle.
	 * @param x the x coordinate
	 * *
	 * @param y the y coordinate
	 * *
	 * @return `true` if (x,y) is inside
	 */
	fun isInside(x: Float, y: Float): Boolean {
		return x >= this.left && x <= this.right && y <= this.top && y >= this.bottom
	}

	/**
	 * Returns whether the given point is inside this rectangle.
	 * @param point the point
	 * *
	 * @return `true` if the point is inside
	 */
	fun isInside(point: Point): Boolean {
		return isInside(point.x, point.y)
	}

	/**
	 * Calculates the size of this rectangle.
	 */
	fun calculateSize() {
		this.size[right - left] = top - bottom
	}

	/**
	 * Sets the bounds of this rectangle to the bounds of the given rectangle.
	 * @param rect rectangle containing the bounds.
	 */
	fun set(rect: Rectangle?) {
		if (rect == null) return
		this.bottom = rect.bottom
		this.left = rect.left
		this.right = rect.right
		this.top = rect.top
		this.calculateSize()
	}

	/**
	 * Sets the bounds of this rectangle to the given bounds.
	 * @param left left bounding
	 * *
	 * @param top top bounding
	 * *
	 * @param right right bounding
	 * *
	 * @param bottom bottom bounding
	 */
	 fun set(left: Float, top: Float, right: Float, bottom: Float) {
		this.left = left
		this.top = top
		this.right = right
		this.bottom = bottom
	}

	companion object {

		/**
		 * Returns whether the given two rectangles are intersecting.
		 * @param rect1 the first rectangle
		 * *
		 * @param rect2 the second rectangle
		 * *
		 * @return `true` if the rectangles are intersecting
		 */
		@JvmStatic fun areIntersecting(rect1: Rectangle, rect2: Rectangle): Boolean {
			return rect1.isInside(rect2.left, rect2.top) || rect1.isInside(rect2.right, rect2.top)
				|| rect1.isInside(rect2.left, rect2.bottom) || rect1.isInside(rect2.right, rect2.bottom)
		}

		/**
		 * Creates a bigger rectangle of the given two and saves it in the target.
		 * @param rect1 the first rectangle
		 * *
		 * @param rect2 the second rectangle
		 * *
		 * @param target the target to save the new bounds.
		 */
		@JvmStatic fun setBiggerRectangle(rect1: Rectangle, rect2: Rectangle, target: Rectangle) {
			target.left = Math.min(rect1.left, rect2.left)
			target.bottom = Math.min(rect1.bottom, rect2.bottom)
			target.right = Math.max(rect1.right, rect2.right)
			target.top = Math.max(rect1.top, rect2.top)
		}
	}

}
