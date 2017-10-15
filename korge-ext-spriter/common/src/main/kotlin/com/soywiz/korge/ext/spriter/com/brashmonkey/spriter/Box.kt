package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter

import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Entity.ObjectInfo
import kotlin.math.max
import kotlin.math.min

/**
 * Represents a box, which consists of four points: top-left, top-right, bottom-left and bottom-right.
 * A box is responsible for checking collisions and calculating a bounding box for a [Timeline.Key.Bone].
 * @author Trixt0r
 */
class Box {
	val points: Array<Point> = Array(4) { Point(0f, 0f) }
	/**
	 * Creates a new box with no witdh and height.
	 */
	private val rect: Rectangle = Rectangle(0f, 0f, 0f, 0f)

	/**
	 * Calculates its four points for the given bone or object with the given info.
	 * @param boneOrObject the bone or object
	 * *
	 * @param info the info
	 * *
	 * @throws NullPointerException if info or boneOrObject is `null`
	 */
	fun calcFor(boneOrObject: Timeline.Key.Bone, info: ObjectInfo) {
		val width = info.size.width * boneOrObject.scale.x
		val height = info.size.height * boneOrObject.scale.y

		val pivotX = width * boneOrObject.pivot.x
		val pivotY = height * boneOrObject.pivot.y

		this.points[0].set(-pivotX, -pivotY)
		this.points[1].set(width - pivotX, -pivotY)
		this.points[2].set(-pivotX, height - pivotY)
		this.points[3].set(width - pivotX, height - pivotY)

		for (i in 0..3)
			this.points[i].rotate(boneOrObject._angle)
		for (i in 0..3)
			this.points[i].translate(boneOrObject.position)
	}

	/**
	 * Returns whether the given coordinates lie inside the box of the given bone or object.
	 * @param boneOrObject the bone or object
	 * *
	 * @param info the object info of the given bone or object
	 * *
	 * @param x the x coordinate
	 * *
	 * @param y the y coordinate
	 * *
	 * @return `true` if the given point lies in the box
	 * *
	 * @throws NullPointerException if info or boneOrObject is `null`
	 */
	fun collides(boneOrObject: Timeline.Key.Bone, info: ObjectInfo, x: Float, y: Float): Boolean {
		val width = info.size.width * boneOrObject.scale.x
		val height = info.size.height * boneOrObject.scale.y

		val pivotX = width * boneOrObject.pivot.x
		val pivotY = height * boneOrObject.pivot.y

		val point = Point(x - boneOrObject.position.x, y - boneOrObject.position.y)
		point.rotate(-boneOrObject._angle)

		return point.x >= -pivotX && point.x <= width - pivotX && point.y >= -pivotY && point.y <= height - pivotY
	}

	/**
	 * Returns whether this box is inside the given rectangle.
	 * @param rect the rectangle
	 * *
	 * @return  `true` if one of the four points is inside the rectangle
	 */
	fun isInside(rect: Rectangle): Boolean {
		var inside = false
		for (p in points)
			inside = inside or rect.isInside(p)
		return inside
	}

	/**
	 * Returns a bounding box for this box.
	 * @return the bounding box
	 */
	val boundingRect: Rectangle
		get() {
			this.rect.set(points[0].x, points[0].y, points[0].x, points[0].y)
			this.rect.left = min(min(min(min(points[0].x, points[1].x), points[2].x), points[3].x), this.rect.left)
			this.rect.right = max(max(max(max(points[0].x, points[1].x), points[2].x), points[3].x), this.rect.right)
			this.rect.top = max(max(max(max(points[0].y, points[1].y), points[2].y), points[3].y), this.rect.top)
			this.rect.bottom = min(min(min(min(points[0].y, points[1].y), points[2].y), points[3].y), this.rect.bottom)
			return this.rect
		}

}
