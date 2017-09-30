package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter

import com.soywiz.korio.lang.JvmOverloads

/**
 * Represents an entity of a Spriter SCML file.
 * An entity holds [Animation]s, an [.id], a [.name].
 * [.characterMaps] and [.objectInfos] may be empty.
 * @author Trixt0r
 */
class Entity internal constructor(val id: Int, val name: String, animations: Int, characterMaps: Int, objectInfos: Int) {
	private val animations: Array<Animation>
	private var animationPointer = 0
	private val namedAnimations: HashMap<String, Animation>
	private val characterMaps: Array<CharacterMap>
	private var charMapPointer = 0
	private val objectInfos: Array<ObjectInfo>
	private var objInfoPointer = 0

	init {
		this.animations = Array<Animation>(animations) { Animation.DUMMY }
		this.characterMaps = Array<CharacterMap>(characterMaps) { CharacterMap.DUMMY }
		this.objectInfos = Array<ObjectInfo>(objectInfos) { ObjectInfo.DUMMY }
		this.namedAnimations = HashMap<String, Animation>()
	}

	internal fun addAnimation(anim: Animation) {
		this.animations[animationPointer++] = anim
		this.namedAnimations.put(anim.name, anim)
	}

	/**
	 * Returns an [Animation] with the given index.
	 * @param index the index of the animation
	 * *
	 * @return the animation with the given index
	 * *
	 * @throws IndexOutOfBoundsException if the index is out of range
	 */
	fun getAnimation(index: Int): Animation {
		return this.animations[index]
	}

	/**
	 * Returns an [Animation] with the given name.
	 * @param name the name of the animation
	 * *
	 * @return the animation with the given name or null if no animation exists with the given name
	 */
	fun getAnimation(name: String): Animation? {
		return this.namedAnimations[name]
	}

	/**
	 * Returns the number of animations this entity holds.
	 * @return the number of animations
	 */
	fun animations(): Int {
		return this.animations.size
	}

	/**
	 * Returns whether this entity contains the given animation.
	 * @param anim the animation to check
	 * *
	 * @return true if the given animation is in this entity, false otherwise.
	 */
	fun containsAnimation(anim: Animation): Boolean {
		for (a in this.animations)
			if (a === anim) return true
		return false
	}

	/**
	 * Returns the animation with the most number of time lines in this entity.
	 * @return animation with the maximum amount of time lines.
	 */
	val animationWithMostTimelines: Animation
		get() {
			var maxAnim = getAnimation(0)
			for (anim in this.animations) {
				if (maxAnim.timelines() < anim.timelines()) maxAnim = anim
			}
			return maxAnim
		}

	/**
	 * Returns a [CharacterMap] with the given name.
	 * @param name name of the character map
	 * *
	 * @return the character map or null if no character map exists with the given name
	 */
	fun getCharacterMap(name: String): CharacterMap? {
		for (map in this.characterMaps)
			if (map.name == name) return map
		return null
	}

	internal fun addCharacterMap(map: CharacterMap) {
		this.characterMaps[charMapPointer++] = map
	}

	internal fun addInfo(info: ObjectInfo) {
		this.objectInfos[objInfoPointer++] = info
	}

	/**
	 * Returns an [ObjectInfo] with the given index.
	 * @param index the index of the object info
	 * *
	 * @return the object info
	 * *
	 * @throws IndexOutOfBoundsException if index is out of range
	 */
	fun getInfo(index: Int): ObjectInfo {
		return this.objectInfos[index]
	}

	/**
	 * Returns an [ObjectInfo] with the given name.
	 * @param name name of the object info
	 * *
	 * @return object info or null if no object info exists with the given name
	 */
	fun getInfo(name: String): ObjectInfo? {
		for (info in this.objectInfos)
			if (info.name == name) return info
		return null
	}

	/**
	 * Returns an [ObjectInfo] with the given name and the given [ObjectType] type.
	 * @param name the name of the object info
	 * *
	 * @param type the type if the object info
	 * *
	 * @return the object info or null if no object info exists with the given name and type
	 */
	fun getInfo(name: String, type: ObjectType): ObjectInfo? {
		val info = this.getInfo(name)
		if (info != null && info.type == type)
			return info
		else
			return null
	}

	/**
	 * Represents the object types Spriter supports.
	 * @author Trixt0r
	 */
	enum class ObjectType {
		Sprite, Bone, Box, Point, Skin;


		companion object {

			/**
			 * Returns the object type for the given name
			 * @param name the name of the type
			 * *
			 * @return the object type, Sprite is the default value
			 */
			fun getObjectInfoFor(name: String): ObjectType {
				if (name == "bone")
					return Bone
				else if (name == "skin")
					return Skin
				else if (name == "box")
					return Box
				else if (name == "point")
					return Point
				else
					return Sprite
			}
		}
	}

	/**
	 * Represents the object info in a Spriter SCML file.
	 * An object info holds a [.type] and a [.name].
	 * If the type is a Sprite it holds a list of frames. Otherwise it has a [.size] for debug drawing purposes.
	 * @author Trixt0r
	 */
	class ObjectInfo @JvmOverloads internal constructor(val name: String, val type: ObjectType, val size: Dimension, val frames: ArrayList<FileReference> = ArrayList<FileReference>()) {
		companion object {
			val DUMMY = ObjectInfo("", ObjectType.Bone, Dimension(0f, 0f))
		}

		internal constructor(name: String, type: ObjectType, frames: ArrayList<FileReference>) : this(name, type, Dimension(0f, 0f), frames) {}

		override fun toString(): String {
			return "$name: $type, size: $size|frames:\n$frames"
		}
	}

	/**
	 * Represents a Spriter SCML character map.
	 * A character map maps [FileReference]s to [FileReference]s.
	 * It holds an [CharacterMap.id] and a [CharacterMap.name].
	 * @author Trixt0r
	 */
	class CharacterMap(val id: Int, val name: String) : HashMap<FileReference, FileReference>() {
		companion object {
			val DUMMY = CharacterMap(0, "")
		}

		/**
		 * Returns the mapped reference for the given key.
		 * @param key the key of the reference
		 * *
		 * @return The mapped reference if the key is in this map, otherwise the given key itself is returned.
		 */
		override fun get(key: FileReference): FileReference? {
			if (!super.containsKey(key))
				return key
			else
				return super.get(key)
		}
	}

	override fun toString(): String {
		var toReturn = this::class.simpleName + "|[id: " + id + ", name: " + name + "]"
		toReturn += "Object infos:\n"
		for (info in this.objectInfos)
			toReturn += "\n" + info
		toReturn += "Character maps:\n"
		for (map in this.characterMaps)
			toReturn += "\n" + map
		toReturn += "Animations:\n"
		for (animaton in this.animations)
			toReturn += "\n" + animaton
		toReturn += "]"
		return toReturn
	}

	companion object {
		var DUMMY = Entity(0, "", 0, 0, 0)
	}

}
