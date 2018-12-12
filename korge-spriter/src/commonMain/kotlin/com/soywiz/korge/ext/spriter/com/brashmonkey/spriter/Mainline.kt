package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter

import com.soywiz.korio.*
import kotlin.math.*

/**
 * Represents a mainline in a Spriter SCML file.
 * A mainline holds only keys and occurs only once in an animation.
 * The mainline is responsible for telling which draw order the sprites have
 * and how the objects are related to each other, i.e. which bone is the root and which objects are the children.
 * @author Trixt0r
 */
class Mainline(keys: Int) {

	val keys: Array<Key>
	private var keyPointer = 0

	init {
		this.keys = Array<Key>(keys) { Key.DUMMY }
	}

	override fun toString(): String {
		var toReturn = "" + this::class + "|"
		for (key in keys)
			toReturn += "\n" + key
		toReturn += "]"
		return toReturn
	}

	fun addKey(key: Key) {
		this.keys[keyPointer++] = key
	}

	/**
	 * Returns a [Key] at the given index.
	 * @param index the index of the key
	 * *
	 * @return the key with the given index
	 * *
	 * @throws IndexOutOfBoundsException if index is out of range
	 */
	fun getKey(index: Int): Key {
		return this.keys[index]
	}

	/**
	 * Returns a [Key] before the given time.
	 * @param time the time a key has to be before
	 * *
	 * @return a key which has a time value before the given one.
	 * * The first key is returned if no key was found.
	 */
	fun getKeyBeforeTime(time: Int): Key {
		var found = this.keys[0]
		for (key in this.keys) {
			if (key.time <= time)
				found = key
			else
				break
		}
		return found
	}

	/**
	 * Represents a mainline key in a Spriter SCML file.
	 * A mainline key holds an [.id], a [.time], a [.curve]
	 * and lists of bone and object references which build a tree hierarchy.
	 * @author Trixt0r
	 */
	class Key(val id: Int, val time: Int, val curve: Curve, boneRefs: Int, objectRefs: Int) {
		val boneRefs: Array<BoneRef>
		val objectRefs: Array<ObjectRef>
		private var bonePointer = 0
		private var objectPointer = 0

		init {
			this.boneRefs = Array<BoneRef>(boneRefs) { BoneRef.DUMMY }
			this.objectRefs = Array<ObjectRef>(objectRefs) { ObjectRef.DUMMY }
		}

		/**
		 * Adds a bone reference to this key.
		 * @param ref the reference to add
		 */
		fun addBoneRef(ref: BoneRef) {
			this.boneRefs[bonePointer++] = ref
		}

		/**
		 * Adds a object reference to this key.
		 * @param ref the reference to add
		 */
		fun addObjectRef(ref: ObjectRef) {
			this.objectRefs[objectPointer++] = ref
		}

		/**
		 * Returns a [BoneRef] with the given index.
		 * @param index the index of the bone reference
		 * *
		 * @return the bone reference or null if no reference exists with the given index
		 */
		fun getBoneRef(index: Int): BoneRef? {
			if (index < 0 || index >= this.boneRefs.size)
				return null
			else
				return this.boneRefs[index]
		}

		/**
		 * Returns a [ObjectRef] with the given index.
		 * @param index the index of the object reference
		 * *
		 * @return the object reference or null if no reference exists with the given index
		 */
		fun getObjectRef(index: Int): ObjectRef? {
			if (index < 0 || index >= this.objectRefs.size)
				return null
			else
				return this.objectRefs[index]
		}

		/**
		 * Returns a [BoneRef] for the given reference.
		 * @param ref the reference to the reference in this key
		 * *
		 * @return a bone reference with the same time line as the given one
		 */
		fun getBoneRef(ref: BoneRef): BoneRef? {
			return getBoneRefTimeline(ref.timeline)
		}

		/**
		 * Returns a [BoneRef] with the given time line index.
		 * @param timeline the time line index
		 * *
		 * @return the bone reference with the given time line index or null if no reference exists with the given time line index
		 */
		fun getBoneRefTimeline(timeline: Int): BoneRef? {
			for (boneRef in this.boneRefs)
				if (boneRef.timeline == timeline) return boneRef
			return null
		}

		/**
		 * Returns an [ObjectRef] for the given reference.
		 * @param ref the reference to the reference in this key
		 * *
		 * @return an object reference with the same time line as the given one
		 */
		fun getObjectRef(ref: ObjectRef): ObjectRef? {
			return getObjectRefTimeline(ref.timeline)
		}

		/**
		 * Returns a [ObjectRef] with the given time line index.
		 * @param timeline the time line index
		 * *
		 * @return the object reference with the given time line index or null if no reference exists with the given time line index
		 */
		fun getObjectRefTimeline(timeline: Int): ObjectRef? {
			for (objRef in this.objectRefs)
				if (objRef.timeline == timeline) return objRef
			return null
		}

		override fun toString(): String {
			var toReturn = "" + this::class + "|[id:" + id + ", time: " + time + ", curve: [" + curve + "]"
			for (ref in boneRefs)
				toReturn += "\n" + ref
			for (ref in objectRefs)
				toReturn += "\n" + ref
			toReturn += "]"
			return toReturn
		}

		/**
		 * Represents a bone reference in a Spriter SCML file.
		 * A bone reference holds an [.id], a [.timeline] and a [.key].
		 * A bone reference may have a parent reference.
		 * @author Trixt0r
		 */
		open class BoneRef(val id: Int, val timeline: Int, val key: Int, val parent: BoneRef?) {
			companion object {
				val DUMMY = BoneRef(0, 0, 0, null)
			}

			override fun toString(): String {
				val parentId = parent?.id ?: -1
				return "" + this::class + "|id: " + id + ", parent:" + parentId + ", timeline: " + timeline + ", key: " + key
			}
		}

		/**
		 * Represents an object reference in a Spriter SCML file.
		 * An object reference extends a [BoneRef] with a [.zIndex],
		 * which indicates when the object has to be drawn.
		 * @author Trixt0r
		 */
		class ObjectRef(id: Int, timeline: Int, key: Int, parent: BoneRef?, val zIndex: Int) :
			BoneRef(id, timeline, key, parent), Comparable<ObjectRef> {
			companion object {
				val DUMMY = ObjectRef(0, 0, 0, null, 0)
			}

			override fun toString(): String {
				return super.toString() + ", z_index: " + zIndex
			}

			override fun compareTo(o: ObjectRef): Int {
				return sign((zIndex - o.zIndex).toFloat()).toInt()
			}
		}

		companion object {
			var DUMMY = Key(0, 0, Curve(), 0, 0)
		}
	}

	companion object {
		var DUMMY = Mainline(0)
	}

}
