package com.brashmonkey.spriter

import com.brashmonkey.spriter.Entity.ObjectInfo
import com.brashmonkey.spriter.Timeline.Key

/**
 * Represents a time line in a Spriter SCML file.
 * A time line holds an [.id], a [.name] and at least one [Key].
 * @author Trixt0r
 */
class Timeline internal constructor(@JvmField val id: Int, @JvmField val name: String, @JvmField val objectInfo: ObjectInfo, keys: Int) {

	@JvmField val keys: Array<Key> = Array<Key>(keys) { Key.DUMMY }
	private var keyPointer = 0

	internal fun addKey(key: Key) {
		this.keys[keyPointer++] = key
	}

	/**
	 * Returns a [Key] at the given index
	 * @param index the index of the key.
	 * *
	 * @return the key with the given index.
	 * *
	 * @throws IndexOutOfBoundsException if the index is out of range
	 */
	fun getKey(index: Int): Key {
		return this.keys[index]
	}

	override fun toString(): String {
		var toReturn = javaClass.simpleName + "|[id:" + id + ", name: " + name + ", object_info: " + objectInfo
		for (key in keys)
			toReturn += "\n" + key
		toReturn += "]"
		return toReturn
	}

	/**
	 * Represents a time line key in a Spriter SCML file.
	 * A key holds an [.id], a [.time], a [.spin], an [.object] and a [.curve].
	 * @author Trixt0r
	 */
	class Key(@JvmField val id: Int, @JvmField var time: Int, @JvmField val spin: Int, @JvmField val curve: Curve) {
		@JvmField var active: Boolean = false
		private var `object`: Object? = null

		@JvmOverloads constructor(id: Int, time: Int = 0, spin: Int = 1) : this(id, time, 1, Curve()) {}

		fun setObject(`object`: Object?) {
			if (`object` == null) throw IllegalArgumentException("object can not be null!")
			this.`object` = `object`
		}

		fun `object`(): Object? {
			return this.`object`
		}

		override fun toString(): String {
			return javaClass.simpleName + "|[id: " + id + ", time: " + time + ", spin: " + spin + "\ncurve: " + curve + "\nobject:" + `object` + "]"
		}

		/**
		 * Represents a bone in a Spriter SCML file.
		 * A bone holds a [.position], [.scale], an [.angle] and a [.pivot].
		 * Bones are the only objects which can be used as a parent for other tweenable objects.
		 * @author Trixt0r
		 */
		open class Bone @JvmOverloads constructor(position: Point = Point(), scale: Point = Point(1f, 1f), pivot: Point = Point(0f, 1f), @JvmField var angle: Float = 0f) {
			@JvmField val position: Point = Point(position)
			@JvmField val scale: Point = Point(scale)
			@JvmField val pivot: Point = Point(pivot)

			constructor(bone: Bone) : this(bone.position, bone.scale, bone.pivot, bone.angle) {}

			/**
			 * Returns whether this instance is a Spriter object or a bone.
			 * @return true if this instance is a Spriter bone
			 */
			val isBone: Boolean get() = this !is Object

			/**
			 * Sets the values of this bone to the values of the given bone
			 * @param bone the bone
			 */
			fun set(bone: Bone) {
				this[bone.position, bone.angle, bone.scale] = bone.pivot
			}

			/**
			 * Sets the given values for this bone.
			 * @param x the new position in x direction
			 * *
			 * @param y the new position in y direction
			 * *
			 * @param angle the new angle
			 * *
			 * @param scaleX the new scale in x direction
			 * *
			 * @param scaleY the new scale in y direction
			 * *
			 * @param pivotX the new pivot in x direction
			 * *
			 * @param pivotY the new pivot in y direction
			 */
			operator fun set(x: Float, y: Float, angle: Float, scaleX: Float, scaleY: Float, pivotX: Float, pivotY: Float) {
				this.angle = angle
				this.position.set(x, y)
				this.scale.set(scaleX, scaleY)
				this.pivot.set(pivotX, pivotY)
			}

			/**
			 * Sets the given values for this bone.
			 * @param position the new position
			 * *
			 * @param angle the new angle
			 * *
			 * @param scale the new scale
			 * *
			 * @param pivot the new pivot
			 */
			operator fun set(position: Point, angle: Float, scale: Point, pivot: Point) {
				this[position.x, position.y, angle, scale.x, scale.y, pivot.x] = pivot.y
			}

			/**
			 * Maps this bone from it's parent's coordinate system to a global one.
			 * @param parent the parent bone of this bone
			 */
			fun unmap(parent: Bone) {
				this.angle *= Math.signum(parent.scale.x) * Math.signum(parent.scale.y)
				this.angle += parent.angle
				this.scale.scale(parent.scale)
				this.position.scale(parent.scale)
				this.position.rotate(parent.angle)
				this.position.translate(parent.position)
			}

			/**
			 * Maps this from it's global coordinate system to the parent's one.
			 * @param parent the parent bone of this bone
			 */
			fun map(parent: Bone) {
				this.position.translate(-parent.position.x, -parent.position.y)
				this.position.rotate(-parent.angle)
				this.position.scale(1f / parent.scale.x, 1f / parent.scale.y)
				this.scale.scale(1f / parent.scale.x, 1f / parent.scale.y)
				this.angle -= parent.angle
				this.angle *= Math.signum(parent.scale.x) * Math.signum(parent.scale.y)
			}

			override fun toString(): String {
				return javaClass.simpleName + "|position: " + position + ", scale: " + scale + ", angle: " + angle
			}
		}


		/**
		 * Represents an object in a Spriter SCML file.
		 * A file has the same properties as a bone with an alpha and file extension.
		 * @author Trixt0r
		 */
		class Object @JvmOverloads constructor(position: Point = Point(), scale: Point = Point(1f, 1f), pivot: Point = Point(0f, 1f), angle: Float = 0f, @JvmField var alpha: Float = 1f, @JvmField val ref: FileReference = FileReference(-1, -1)) : Bone(position, scale, pivot, angle) {

			constructor(`object`: Object) : this(`object`.position.copy(), `object`.scale.copy(), `object`.pivot.copy(), `object`.angle, `object`.alpha, `object`.ref) {}

			/**
			 * Sets the values of this object to the values of the given object.
			 * @param object the object
			 */
			fun set(`object`: Object) {
				this[`object`.position, `object`.angle, `object`.scale, `object`.pivot, `object`.alpha] = `object`.ref
			}

			/**
			 * Sets the given values for this object.
			 * @param x the new position in x direction
			 * *
			 * @param y the new position in y direction
			 * *
			 * @param angle the new angle
			 * *
			 * @param scaleX the new scale in x direction
			 * *
			 * @param scaleY the new scale in y direction
			 * *
			 * @param pivotX the new pivot in x direction
			 * *
			 * @param pivotY the new pivot in y direction
			 * *
			 * @param alpha the new alpha value
			 * *
			 * @param folder the new folder index
			 * *
			 * @param file the new file index
			 */
			operator fun set(x: Float, y: Float, angle: Float, scaleX: Float, scaleY: Float, pivotX: Float, pivotY: Float, alpha: Float, folder: Int, file: Int) {
				super.set(x, y, angle, scaleX, scaleY, pivotX, pivotY)
				this.alpha = alpha
				this.ref.folder = folder
				this.ref.file = file
			}

			/**
			 * Sets the given values for this object.
			 * @param position the new position
			 * *
			 * @param angle the new angle
			 * *
			 * @param scale the new scale
			 * *
			 * @param pivot the new pivot
			 * *
			 * @param alpha the new alpha value
			 * *
			 * @param fileRef the new file reference
			 */
			operator fun set(position: Point, angle: Float, scale: Point, pivot: Point, alpha: Float, fileRef: FileReference) {
				this[position.x, position.y, angle, scale.x, scale.y, pivot.x, pivot.y, alpha, fileRef.folder] = fileRef.file
			}

			override fun toString(): String {
				return super.toString() + ", pivot: " + pivot + ", alpha: " + alpha + ", reference: " + ref
			}

		}

		companion object {
			var DUMMY = Key(0)
		}
	}

	companion object {
		var DUMMY = Timeline(0, "", ObjectInfo.DUMMY, 0)
	}

}
