package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter

import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Entity.CharacterMap
import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Entity.ObjectInfo
import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Mainline.Key.BoneRef
import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Mainline.Key.ObjectRef
import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Player.PlayerListener
import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Timeline.Key.Bone
import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Timeline.Key.Object
import kotlin.math.sign

/**
 * A Player instance is responsible for updating an [Animation] properly.
 * With the [.update] method an instance of this class will increase its current time
 * and update the current set animation ([.setAnimation]).
 * A Player can be positioned with [.setPivot], scaled with [.setScale],
 * flipped with [.flip] and rotated [.setAngle].
 * A Player has various methods for runtime object manipulation such as [.setBone] or [.setObject].
 * Events like the ending of an animation can be observed with the [PlayerListener] interface.
 * Character maps can be changed on the fly, just by assigning a character maps to [.characterMaps], setting it to `null` will remove the current character map.

 * @author Trixt0r
 */
@Suppress("unused", "MemberVisibilityCanBePrivate", "PropertyName")
open class Player
/**
 * Creates a [Player] instance with the given entity.
 * @param entity the entity this player will animate
 */
	(entity: Entity) {

	var _entity: Entity = Entity.DUMMY
	var _animation: Animation = Animation.DUMMY
	var _time: Int = 0
	var speed: Int = 15
	var tweenedKeys: Array<Timeline.Key> = emptyArray()
	var unmappedTweenedKeys: Array<Timeline.Key> = emptyArray()
	private var tempTweenedKeys: Array<Timeline.Key>? = null
	private var tempUnmappedTweenedKeys: Array<Timeline.Key>? = null
	private val listeners: MutableList<PlayerListener> = ArrayList()
	val attachments: List<Attachment> = ArrayList()
	var root = Bone(Point(0f, 0f))
	private val position = Point(0f, 0f)
	private val pivot = Point(0f, 0f)
	private val objToTimeline = HashMap<Object, Timeline.Key>()
	private var angle: Float = 0.toFloat()
	private var dirty = true
	var characterMaps: Array<CharacterMap> = emptyArray()
	private val rect: Rectangle = Rectangle(0f, 0f, 0f, 0f)
	val prevBBox: Box = Box()
	private val boneIterator: BoneIterator = BoneIterator()
	private val objectIterator: ObjectIterator = ObjectIterator()
	/**
	 * Returns the current main line key based on the current [.time].
	 * @return the current main line key
	 */
	var currentKey: Mainline.Key? = null
		private set
	private var prevKey: Mainline.Key? = null
	var copyObjects = true

	init {
		this.setEntity(entity)
	}

	/**
	 * Updates this player.
	 * This means the current time gets increased by [.speed] and is applied to the current animation.
	 */
	open fun update() {
		for (i in listeners.indices) {
			listeners[i].preProcess(this)
		}
		if (dirty) this.updateRoot()
		this._animation.update(_time, root)
		this.currentKey = this._animation.currentKey
		if (prevKey != currentKey) {
			for (i in listeners.indices) {
				listeners[i].mainlineKeyChanged(prevKey, currentKey)
			}
			prevKey = currentKey
		}
		if (copyObjects) {
			tweenedKeys = tempTweenedKeys!!
			unmappedTweenedKeys = tempUnmappedTweenedKeys!!
			this.copyObjects()
		} else {
			tweenedKeys = _animation.tweenedKeys
			unmappedTweenedKeys = _animation.unmappedTweenedKeys
		}

		for (i in attachments.indices) {
			attachments[i].update()
		}

		for (i in listeners.indices) {
			listeners[i].postProcess(this)
		}
		this.increaseTime()
	}

	private fun copyObjects() {
		for (i in _animation.tweenedKeys.indices) {
			this.tweenedKeys[i].active = _animation.tweenedKeys[i].active
			this.unmappedTweenedKeys[i].active = _animation.unmappedTweenedKeys[i].active
			this.tweenedKeys[i].`object`().set(_animation.tweenedKeys[i].`object`())
			this.unmappedTweenedKeys[i].`object`().set(_animation.unmappedTweenedKeys[i].`object`())
		}
	}

	private fun increaseTime() {
		_time += speed
		if (_time > _animation.length) {
			_time -= _animation.length
			for (i in listeners.indices) {
				listeners[i].animationFinished(_animation)
			}
		}
		if (_time < 0) {
			for (i in listeners.indices) {
				listeners[i].animationFinished(_animation)
			}
			_time += _animation.length
		}
	}

	private fun updateRoot() {
		this.root._angle = angle
		this.root.position.set(pivot)
		this.root.position.rotate(angle)
		this.root.position.translate(position)
		dirty = false
	}

	/**
	 * Returns a time line bone at the given index.
	 * @param index the index of the bone
	 * *
	 * @return the bone with the given index.
	 */
	fun getBone(index: Int): Bone {
		return this.unmappedTweenedKeys[currentKey!!.getBoneRef(index)!!.timeline].`object`()
	}

	/**
	 * Returns a time line object at the given index.
	 * @param index the index of the object
	 * *
	 * @return the object with the given index.
	 */
	fun getObject(index: Int): Object {
		return this.unmappedTweenedKeys[currentKey!!.getObjectRef(index)!!.timeline].`object`()
	}

	/**
	 * Returns the index of a time line bone with the given name.
	 * @param name the name of the bone
	 * *
	 * @return the index of the bone or -1 if no bone exists with the given name
	 */
	fun getBoneIndex(name: String): Int {
		for (ref in currentKey!!.boneRefs)
			if (_animation.getTimeline(ref.timeline).name == name)
				return ref.id
		return -1
	}

	/**
	 * Returns a time line bone with the given name.
	 * @param name the name of the bone
	 * *
	 * @return the bone with the given name
	 * *
	 * @throws IndexOutOfBoundsException if no bone exists with the given name
	 * *
	 * @throws NullPointerException if no bone exists with the given name
	 */
	fun getBone(name: String): Bone {
		return this.unmappedTweenedKeys[_animation.getTimeline(name)!!.id].`object`()
	}

	/**
	 * Returns a bone reference for the given time line bone.
	 * @param bone the time line bone
	 * *
	 * @return the bone reference for the given bone
	 * *
	 * @throws NullPointerException if no reference for the given bone was found
	 */
	fun getBoneRef(bone: Bone): BoneRef? {
		return this.currentKey!!.getBoneRefTimeline(this.objToTimeline[bone]!!.id)
	}

	/**
	 * Returns the index of a time line object with the given name.
	 * @param name the name of the object
	 * *
	 * @return the index of the object or -1 if no object exists with the given name
	 */
	fun getObjectIndex(name: String): Int {
		for (ref in currentKey!!.objectRefs)
			if (_animation.getTimeline(ref.timeline).name == name)
				return ref.id
		return -1
	}

	/**
	 * Returns a time line object with the given name.
	 * @param name the name of the object
	 * *
	 * @return the object with the given name
	 * *
	 * @throws IndexOutOfBoundsException if no object exists with the given name
	 * *
	 * @throws NullPointerException if no object exists with the given name
	 */
	fun getObject(name: String): Object {
		return this.unmappedTweenedKeys[_animation.getTimeline(name)!!.id].`object`()
	}

	/**
	 * Returns a object reference for the given time line bone.
	 * @param object the time line object
	 * *
	 * @return the object reference for the given bone
	 * *
	 * @throws NullPointerException if no reference for the given object was found
	 */
	fun getObjectRef(`object`: Object): ObjectRef? {
		return this.currentKey!!.getObjectRefTimeline(this.objToTimeline[`object`]!!.id)
	}

	/**
	 * Returns the name for the given bone or object.
	 * @param boneOrObject the bone or object
	 * *
	 * @return the name of the bone or object
	 * *
	 * @throws NullPointerException if no name for the given bone or object was found
	 */
	fun getNameFor(boneOrObject: Bone): String {
		return this._animation.getTimeline(objToTimeline[boneOrObject]!!.id).name
	}

	/**
	 * Returns the object info for the given bone or object.
	 * @param boneOrObject the bone or object
	 * *
	 * @return the object info of the bone or object
	 * *
	 * @throws NullPointerException if no object info for the given bone or object was found
	 */
	fun getObjectInfoFor(boneOrObject: Bone): ObjectInfo {
		return this._animation.getTimeline(objToTimeline[boneOrObject]!!.id).objectInfo
	}

	/**
	 * Returns the time line key for the given bone or object
	 * @param boneOrObject the bone or object
	 * *
	 * @return the time line key of the bone or object, or null if no time line key was found
	 */
	fun getKeyFor(boneOrObject: Bone): Timeline.Key {
		return objToTimeline[boneOrObject]!!
	}

	/**
	 * Calculates and returns a [Box] for the given bone or object.
	 * @param boneOrObject the bone or object to calculate the bounding box for
	 * *
	 * @return the box for the given bone or object
	 * *
	 * @throws NullPointerException if no object info for the given bone or object exists
	 */
	fun getBox(boneOrObject: Bone): Box {
		val info = getObjectInfoFor(boneOrObject)
		this.prevBBox.calcFor(boneOrObject, info)
		return this.prevBBox
	}

	/**
	 * Returns whether the given point at x,y lies inside the box of the given bone or object.
	 * @param boneOrObject the bone or object
	 * *
	 * @param x the x value of the point
	 * *
	 * @param y the y value of the point
	 * *
	 * @return `true` if x,y lies inside the box of the given bone or object
	 * *
	 * @throws NullPointerException if no object info for the given bone or object exists
	 */
	fun collidesFor(boneOrObject: Bone, x: Float, y: Float): Boolean {
		val info = getObjectInfoFor(boneOrObject)
		this.prevBBox.calcFor(boneOrObject, info)
		return this.prevBBox.collides(boneOrObject, info, x, y)
	}

	/**
	 * Returns whether the given point lies inside the box of the given bone or object.
	 * @param boneOrObject the bone or object
	 * *
	 * @param point the point
	 * *
	 * @return `true` if the point lies inside the box of the given bone or object
	 * *
	 * @throws NullPointerException if no object info for the given bone or object exists
	 */
	fun collidesFor(boneOrObject: Bone, point: Point): Boolean {
		return this.collidesFor(boneOrObject, point.x, point.y)
	}

	/**
	 * Returns whether the given area collides with the box of the given bone or object.
	 * @param boneOrObject the bone or object
	 * *
	 * @param area the rectangular area
	 * *
	 * @return `true` if the area collides with the bone or object
	 */
	fun collidesFor(boneOrObject: Bone, area: Rectangle): Boolean {
		val info = getObjectInfoFor(boneOrObject)
		this.prevBBox.calcFor(boneOrObject, info)
		return this.prevBBox.isInside(area)
	}

	/**
	 * Sets the given values of the bone with the given name.
	 * @param name the name of the bone
	 * *
	 * @param x the new x value of the bone
	 * *
	 * @param y the new y value of the bone
	 * *
	 * @param angle the new angle of the bone
	 * *
	 * @param scaleX the new scale in x direction of the bone
	 * *
	 * @param scaleY the new scale in y direction of the bone
	 * *
	 * @throws SpriterException if no bone exists of the given name
	 */
	fun setBone(name: String, x: Float, y: Float, angle: Float, scaleX: Float, scaleY: Float) {
		val index = getBoneIndex(name)
		if (index == -1) throw SpriterException("No bone found of name \"$name\"")
		val ref = currentKey!!.getBoneRef(index)
		val bone = getBone(index)
		bone[x, y, angle, scaleX, scaleY, 0f] = 0f
		unmapObjects(ref)
	}

	/**
	 * Sets the given values of the bone with the given name.
	 * @param name the name of the bone
	 * *
	 * @param position the new position of the bone
	 * *
	 * @param angle the new angle of the bone
	 * *
	 * @param scale the new scale of the bone
	 * *
	 * @throws SpriterException if no bone exists of the given name
	 */
	fun setBone(name: String, position: Point, angle: Float, scale: Point) {
		this.setBone(name, position.x, position.y, angle, scale.x, scale.y)
	}

	/**
	 * Sets the given values of the bone with the given name.
	 * @param name the name of the bone
	 * *
	 * @param x the new x value of the bone
	 * *
	 * @param y the new y value of the bone
	 * *
	 * @param angle the new angle of the bone
	 * *
	 * @throws SpriterException if no bone exists of the given name
	 */
	fun setBone(name: String, x: Float, y: Float, angle: Float) {
		val b = getBone(name)
		setBone(name, x, y, angle, b.scale.x, b.scale.y)
	}

	/**
	 * Sets the given values of the bone with the given name.
	 * @param name the name of the bone
	 * *
	 * @param position the new position of the bone
	 * *
	 * @param angle the new angle of the bone
	 * *
	 * @throws SpriterException if no bone exists of the given name
	 */
	fun setBone(name: String, position: Point, angle: Float) {
		val b = getBone(name)
		setBone(name, position.x, position.y, angle, b.scale.x, b.scale.y)
	}

	/**
	 * Sets the position of the bone with the given name.
	 * @param name the name of the bone
	 * *
	 * @param x the new x value of the bone
	 * *
	 * @param y the new y value of the bone
	 * *
	 * @throws SpriterException if no bone exists of the given name
	 */
	fun setBone(name: String, x: Float, y: Float) {
		val b = getBone(name)
		setBone(name, x, y, b._angle)
	}

	/**
	 * Sets the position of the bone with the given name.
	 * @param name the name of the bone
	 * *
	 * @param position the new position of the bone
	 * *
	 * @throws SpriterException if no bone exists of the given name
	 */
	fun setBone(name: String, position: Point) {
		setBone(name, position.x, position.y)
	}

	/**
	 * Sets the angle of the bone with the given name
	 * @param name the name of the bone
	 * *
	 * @param angle the new angle of the bone
	 * *
	 * @throws SpriterException if no bone exists of the given name
	 */
	fun setBone(name: String, angle: Float) {
		val b = getBone(name)
		setBone(name, b.position.x, b.position.y, angle)
	}

	/**
	 * Sets the values of the bone with the given name to the values of the given bone
	 * @param name the name of the bone
	 * *
	 * @param bone the bone with the new values
	 * *
	 * @throws SpriterException if no bone exists of the given name
	 */
	fun setBone(name: String, bone: Bone) {
		setBone(name, bone.position, bone._angle, bone.scale)
	}

	/**
	 * Sets the given values of the object with the given name.
	 * @param name the name of the object
	 * *
	 * @param x the new position in x direction of the object
	 * *
	 * @param y the new position in y direction of the object
	 * *
	 * @param angle the new angle of the object
	 * *
	 * @param scaleX the new scale in x direction of the object
	 * *
	 * @param scaleY the new scale in y direction of the object
	 * *
	 * @param pivotX the new pivot in x direction of the object
	 * *
	 * @param pivotY the new pivot in y direction of the object
	 * *
	 * @param alpha the new alpha value of the object
	 * *
	 * @param folder the new folder index of the object
	 * *
	 * @param file the new file index of the object
	 * *
	 * @throws SpriterException if no object exists of the given name
	 */
	fun setObject(
		name: String,
		x: Float,
		y: Float,
		angle: Float,
		scaleX: Float,
		scaleY: Float,
		pivotX: Float,
		pivotY: Float,
		alpha: Float,
		folder: Int,
		file: Int
	) {
		val index = getObjectIndex(name)
		if (index == -1) throw SpriterException("No object found for name \"$name\"")
		val ref = currentKey!!.getObjectRef(index)
		val `object` = getObject(index)
		`object`[x, y, angle, scaleX, scaleY, pivotX, pivotY, alpha, folder] = file
		unmapObjects(ref)
	}

	/**
	 * Sets the given values of the object with the given name.
	 * @param name the name of the object
	 * *
	 * @param position the new position of the object
	 * *
	 * @param angle the new angle of the object
	 * *
	 * @param scale the new scale of the object
	 * *
	 * @param pivot the new pivot of the object
	 * *
	 * @param alpha the new alpha value of the object
	 * *
	 * @param ref the new file reference of the object
	 * *
	 * @throws SpriterException if no object exists of the given name
	 */
	fun setObject(
		name: String,
		position: Point,
		angle: Float,
		scale: Point,
		pivot: Point,
		alpha: Float,
		ref: FileReference
	) {
		this.setObject(
			name,
			position.x,
			position.y,
			angle,
			scale.x,
			scale.y,
			pivot.x,
			pivot.y,
			alpha,
			ref.folder,
			ref.file
		)
	}

	/**
	 * Sets the given values of the object with the given name.
	 * @param name the name of the object
	 * *
	 * @param x the new position in x direction of the object
	 * *
	 * @param y the new position in y direction of the object
	 * *
	 * @param angle the new angle of the object
	 * *
	 * @param scaleX the new scale in x direction of the object
	 * *
	 * @param scaleY the new scale in y direction of the object
	 * *
	 * @throws SpriterException if no object exists of the given name
	 */
	fun setObject(name: String, x: Float, y: Float, angle: Float, scaleX: Float, scaleY: Float) {
		val b = getObject(name)
		setObject(name, x, y, angle, scaleX, scaleY, b.pivot.x, b.pivot.y, b.alpha, b.ref.folder, b.ref.file)
	}

	/**
	 * Sets the given values of the object with the given name.
	 * @param name the name of the object
	 * *
	 * @param x the new position in x direction of the object
	 * *
	 * @param y the new position in y direction of the object
	 * *
	 * @param angle the new angle of the object
	 * *
	 * @throws SpriterException if no object exists of the given name
	 */
	fun setObject(name: String, x: Float, y: Float, angle: Float) {
		val b = getObject(name)
		setObject(name, x, y, angle, b.scale.x, b.scale.y)
	}

	/**
	 * Sets the given values of the object with the given name.
	 * @param name the name of the object
	 * *
	 * @param position the new position of the object
	 * *
	 * @param angle the new angle of the object
	 * *
	 * @throws SpriterException if no object exists of the given name
	 */
	fun setObject(name: String, position: Point, angle: Float) {
		val b = getObject(name)
		setObject(name, position.x, position.y, angle, b.scale.x, b.scale.y)
	}

	/**
	 * Sets the position of the object with the given name.
	 * @param name the name of the object
	 * *
	 * @param x the new position in x direction of the object
	 * *
	 * @param y the new position in y direction of the object
	 * *
	 * @throws SpriterException if no object exists of the given name
	 */
	fun setObject(name: String, x: Float, y: Float) {
		val b = getObject(name)
		setObject(name, x, y, b._angle)
	}

	/**
	 * Sets the position of the object with the given name.
	 * @param name the name of the object
	 * *
	 * @param position the new position of the object
	 * *
	 * @throws SpriterException if no object exists of the given name
	 */
	fun setObject(name: String, position: Point) {
		setObject(name, position.x, position.y)
	}

	/**
	 * Sets the position of the object with the given name.
	 * @param name the name of the object
	 * *
	 * @param angle the new angle of the object
	 * *
	 * @throws SpriterException if no object exists of the given name
	 */
	fun setObject(name: String, angle: Float) {
		val b = getObject(name)
		setObject(name, b.position.x, b.position.y, angle)
	}

	/**
	 * Sets the position of the object with the given name.
	 * @param name the name of the object
	 * *
	 * @param alpha the new alpha value of the object
	 * *
	 * @param folder the new folder index of the object
	 * *
	 * @param file the new file index of the object
	 * *
	 * @throws SpriterException if no object exists of the given name
	 */
	fun setObject(name: String, alpha: Float, folder: Int, file: Int) {
		val b = getObject(name)
		setObject(
			name,
			b.position.x,
			b.position.y,
			b._angle,
			b.scale.x,
			b.scale.y,
			b.pivot.x,
			b.pivot.y,
			alpha,
			folder,
			file
		)
	}

	/**
	 * Sets the values of the object with the given name to the values of the given object.
	 * @param name the name of the object
	 * *
	 * @param object the object with the new values
	 * *
	 * @throws SpriterException if no object exists of the given name
	 */
	fun setObject(name: String, `object`: Object) {
		setObject(
			name,
			`object`.position,
			`object`._angle,
			`object`.scale,
			`object`.pivot,
			`object`.alpha,
			`object`.ref
		)
	}

	/**
	 * Maps all object from the parent's coordinate system to the global coordinate system.
	 * @param base the root bone to start at. Set it to `null` to traverse the whole bone hierarchy.
	 */
	fun unmapObjects(base: BoneRef?) {
		val start = if (base == null) -1 else base.id - 1
		for (i in start + 1 until currentKey!!.boneRefs.size) {
			val ref = currentKey!!.getBoneRef(i)!!
			if (ref.parent !== base && base !== null) continue
			val parent = if (ref.parent == null) this.root else this.unmappedTweenedKeys[ref.parent.timeline].`object`()
			unmappedTweenedKeys[ref.timeline].`object`().set(tweenedKeys[ref.timeline].`object`())
			unmappedTweenedKeys[ref.timeline].`object`().unmap(parent)
			unmapObjects(ref)
		}
		for (ref in currentKey!!.objectRefs) {
			if (ref.parent !== base && base !== null) continue
			val parent =
				if (ref.parent == null) this.root else this.unmappedTweenedKeys[ref.parent.timeline].`object`()
			unmappedTweenedKeys[ref.timeline].`object`().set(tweenedKeys[ref.timeline].`object`())
			unmappedTweenedKeys[ref.timeline].`object`().unmap(parent)
		}
	}

	/**
	 * Sets the entity for this player instance.
	 * The animation will be switched to the first one of the new entity.
	 * @param entity the new entity
	 * *
	 * @throws SpriterException if the entity is `null`
	 */
	open fun setEntity(entity: Entity?) {
		if (entity == null) throw SpriterException("entity can not be null!")
		this._entity = entity
		val maxAnims = entity.animationWithMostTimelines.timelines()
		tweenedKeys = Array(maxAnims) { Timeline.Key.DUMMY }
		unmappedTweenedKeys = Array(maxAnims) { Timeline.Key.DUMMY }
		for (i in 0 until maxAnims) {
			val key = Timeline.Key(i)
			val keyU = Timeline.Key(i)
			key.setObject(Object(Point(0f, 0f)))
			keyU.setObject(Object(Point(0f, 0f)))
			tweenedKeys[i] = key
			unmappedTweenedKeys[i] = keyU
			this.objToTimeline[keyU.`object`()] = keyU
		}
		this.tempTweenedKeys = tweenedKeys
		this.tempUnmappedTweenedKeys = unmappedTweenedKeys
		this.setAnimation(entity.getAnimation(0))
	}

	/**
	 * Returns the current set entity.
	 * @return the current entity
	 */
	fun getEntity(): Entity {
		return this._entity
	}

	/**
	 * Sets the animation of this player.
	 * @param animation the new animation
	 * *
	 * @throws SpriterException if the animation is `null` or the current animation is not a member of the current set entity
	 */
	open fun setAnimation(animation: Animation?) {
		val prevAnim = this._animation
		if (animation === this._animation) return
		if (animation == null) throw SpriterException("animation can not be null!")
		if (!this._entity.containsAnimation(animation) && animation.id != -1) throw SpriterException("animation has to be in the same entity as the current set one!")
		if (animation !== this._animation) _time = 0
		this._animation = animation
		val tempTime = this._time
		this._time = 0
		this.update()
		this._time = tempTime
		for (i in listeners.indices) {
			listeners[i].animationChanged(prevAnim, animation)
		}
	}

	/**
	 * Sets the animation of this player to the one with the given name.
	 * @param name the name of the animation
	 * *
	 * @throws SpriterException if no animation exists with the given name
	 */
	fun setAnimation(name: String) {
		this.setAnimation(_entity.getAnimation(name))
	}

	/**
	 * Sets the animation of this player to the one with the given index.
	 * @param index the index of the animation
	 * *
	 * @throws IndexOutOfBoundsException if the index is out of range
	 */
	fun setAnimation(index: Int) {
		this.setAnimation(_entity.getAnimation(index))
	}

	/**
	 * Returns the current set animation.
	 * @return the current animation
	 */
	fun getAnimation(): Animation {
		return this._animation
	}

	/**
	 * Returns a bounding box for this player.
	 * The bounding box is calculated for all bones and object starting from the given root.
	 * @param root the starting root. Set it to null to calculate the bounding box for the whole player
	 * *
	 * @return the bounding box
	 */
	fun getBoundingRectangle(root: BoneRef?): Rectangle {
		val boneRoot = if (root == null) this.root else this.unmappedTweenedKeys[root.timeline].`object`()
		this.rect.set(boneRoot.position.x, boneRoot.position.y, boneRoot.position.x, boneRoot.position.y)
		this.calcBoundingRectangle(root)
		this.rect.calculateSize()
		return this.rect
	}

	/**
	 * Returns a bounding box for this player.
	 * The bounding box is calculated for all bones and object starting from the given root.
	 * @param root the starting root. Set it to null to calculate the bounding box for the whole player
	 * *
	 * @return the bounding box
	 */
	fun getBoudingRectangle(root: Bone?): Rectangle {
		return this.getBoundingRectangle(if (root == null) null else getBoneRef(root))
	}

	private fun calcBoundingRectangle(root: BoneRef?) {
		for (ref in currentKey!!.boneRefs) {
			if (ref.parent !== root && root !== null) continue
			val bone = this.unmappedTweenedKeys[ref.timeline].`object`()
			this.prevBBox.calcFor(bone, _animation.getTimeline(ref.timeline).objectInfo)
			Rectangle.setBiggerRectangle(rect, this.prevBBox.boundingRect, rect)
			this.calcBoundingRectangle(ref)
		}
		for (ref in currentKey!!.objectRefs) {
			if (ref.parent !== root) continue
			val bone = this.unmappedTweenedKeys[ref.timeline].`object`()
			this.prevBBox.calcFor(bone, _animation.getTimeline(ref.timeline).objectInfo)
			Rectangle.setBiggerRectangle(rect, this.prevBBox.boundingRect, rect)
		}
	}

	/**
	 * Returns the current time.
	 * The player will make sure that the current time is always between 0 and [Animation.length].
	 * @return the current time
	 */
	fun getTime(): Int {
		return _time
	}

	/**
	 * Sets the time for the current time.
	 * The player will make sure that the new time will not exceed the time bounds of the current animation.
	 * @param time the new time
	 * *
	 * @return this player to enable chained operations
	 */
	fun setTime(time: Int): Player {
		this._time = time
		val prevSpeed = this.speed
		this.speed = 0
		this.increaseTime()
		this.speed = prevSpeed
		return this
	}

	/**
	 * Sets the scale of this player to the given one.
	 * Only uniform scaling is supported.
	 * @param scale the new scale. 1f means 100% scale.
	 * *
	 * @return this player to enable chained operations
	 */
	fun setScale(scale: Float): Player {
		this.root.scale.set(scale * flippedX(), scale * flippedY())
		return this
	}

	/**
	 * Scales this player based on the current set scale.
	 * @param scale the scaling factor. 1f means no scale.
	 * *
	 * @return this player to enable chained operations
	 */
	fun scale(scale: Float): Player {
		this.root.scale.scale(scale, scale)
		return this
	}

	/**
	 * Returns the current scale.
	 * @return the current scale
	 */
	val scale: Float
		get() = root.scale.x

	/**
	 * Flips this player around the x and y axis.
	 * @param x whether to flip the player around the x axis
	 * *
	 * @param y whether to flip the player around the y axis
	 * *
	 * @return this player to enable chained operations
	 */
	fun flip(x: Boolean, y: Boolean): Player {
		if (x) this.flipX()
		if (y) this.flipY()
		return this
	}

	/**
	 * Flips the player around the x axis.
	 * @return this player to enable chained operations
	 */
	fun flipX(): Player {
		this.root.scale.x = this.root.scale.x * -1
		return this
	}

	/**
	 * Flips the player around the y axis.
	 * @return this player to enable chained operations
	 */
	fun flipY(): Player {
		this.root.scale.y = this.root.scale.y * -1
		return this
	}

	/**
	 * Returns whether this player is flipped around the x axis.
	 * @return 1 if this player is not flipped, -1 if it is flipped
	 */
	fun flippedX(): Int {
		return sign(root.scale.x).toInt()
	}

	/**
	 * Returns whether this player is flipped around the y axis.
	 * @return 1 if this player is not flipped, -1 if it is flipped
	 */
	fun flippedY(): Int {
		return sign(root.scale.y).toInt()
	}

	/**
	 * Sets the position of this player to the given coordinates.
	 * @param x the new position in x direction
	 * *
	 * @param y the new position in y direction
	 * *
	 * @return this player to enable chained operations
	 */
	fun setPosition(x: Float, y: Float): Player {
		this.dirty = true
		this.position.set(x, y)
		return this
	}

	/**
	 * Sets the position of the player to the given one.
	 * @param position the new position
	 * *
	 * @return this player to enable chained operations
	 */
	fun setPosition(position: Point): Player {
		return this.setPosition(position.x, position.y)
	}

	/**
	 * Adds the given coordinates to the current position of this player.
	 * @param x the amount in x direction
	 * *
	 * @param y the amount in y direction
	 * *
	 * @return this player to enable chained operations
	 */
	fun translatePosition(x: Float, y: Float): Player {
		return this.setPosition(position.x + x, position.y + y)
	}

	/**
	 * Adds the given amount to the current position of this player.
	 * @param amount the amount to add
	 * *
	 * @return this player to enable chained operations
	 */
	fun translate(amount: Point): Player {
		return this.translatePosition(amount.x, amount.y)
	}

	/**
	 * Returns the current position in x direction.
	 * @return the current position in x direction
	 */
	val x: Float
		get() = position.x

	/**
	 * Returns the current position in y direction.
	 * @return the current position in y direction
	 */
	val y: Float
		get() = position.y

	/**
	 * Sets the angle of this player to the given angle.
	 * @param angle the angle in degrees
	 * *
	 * @return this player to enable chained operations
	 */
	fun setAngle(angle: Float): Player {
		this.dirty = true
		this.angle = angle
		return this
	}

	/**
	 * Rotates this player by the given angle.
	 * @param angle the angle in degrees
	 * *
	 * @return this player to enable chained operations
	 */
	fun rotate(angle: Float): Player {
		return this.setAngle(angle + this.angle)
	}

	/**
	 * Returns the current set angle.
	 * @return the current angle
	 */
	fun getAngle(): Float {
		return this.angle
	}

	/**
	 * Sets the pivot, i.e. origin, of this player.
	 * A pivot at (0,0) means that the origin of the played animation will have the same one as in Spriter.
	 * @param x the new pivot in x direction
	 * *
	 * @param y the new pivot in y direction
	 * *
	 * @return this player to enable chained operations
	 */
	fun setPivot(x: Float, y: Float): Player {
		this.dirty = true
		this.pivot.set(x, y)
		return this
	}

	/**
	 * Sets the pivot, i.e. origin, of this player.
	 * A pivot at (0,0) means that the origin of the played animation will have the same one as in Spriter.
	 * @param pivot the new pivot
	 * *
	 * @return this player to enable chained operations
	 */
	fun setPivot(pivot: Point): Player {
		return this.setPivot(pivot.x, pivot.y)
	}

	/**
	 * Translates the current set pivot position by the given amount.
	 * @param x the amount in x direction
	 * *
	 * @param y the amount in y direction
	 * *
	 * @return this player to enable chained operations
	 */
	fun translatePivot(x: Float, y: Float): Player {
		return this.setPivot(pivot.x + x, pivot.y + y)
	}

	/**
	 * Adds the given amount to the current set pivot position.
	 * @param amount the amount to add
	 * *
	 * @return this player to enable chained operations
	 */
	fun translatePivot(amount: Point): Player {
		return this.translatePivot(amount.x, amount.y)
	}

	/**
	 * Returns the current set pivot in x direction.
	 * @return the pivot in x direction
	 */
	val pivotX: Float
		get() = pivot.x

	/**
	 * Returns the current set pivot in y direction.
	 * @return the pivot in y direction
	 */
	val pivotY: Float
		get() = pivot.y

	/**
	 * Appends a listener to the listeners list of this player.
	 * @param listener the listener to add
	 */
	fun addListener(listener: PlayerListener) {
		this.listeners.add(listener)
	}

	/**
	 * Removes a listener from  the listeners list of this player.
	 * @param listener the listener to remove
	 */
	fun removeListener(listener: PlayerListener) {
		this.listeners.remove(listener)
	}

	/**
	 * Returns an iterator to iterate over all time line bones in the current animation.
	 * @return the bone iterator
	 */
	fun boneIterator(): Iterator<Bone> {
		return this.boneIterator(this.currentKey!!.boneRefs[0])
	}

	/**
	 * Returns an iterator to iterate over all time line bones in the current animation starting at a given root.
	 * @param start the bone reference to start at
	 * *
	 * @return the bone iterator
	 */
	fun boneIterator(start: BoneRef): Iterator<Bone> {
		this.boneIterator.index = start.id
		return this.boneIterator
	}

	/**
	 * Returns an iterator to iterate over all time line objects in the current animation.
	 * @return the object iterator
	 */
	fun objectIterator(): Iterator<Object> {
		return this.objectIterator(this.currentKey!!.objectRefs[0])
	}

	/**
	 * Returns an iterator to iterate over all time line objects in the current animation starting at a given root.
	 * @param start the object reference to start at
	 * *
	 * @return the object iterator
	 */
	fun objectIterator(start: ObjectRef): Iterator<Object> {
		this.objectIterator.index = start.id
		return this.objectIterator
	}

	/**
	 * An iterator to iterate over all time line objects in the current animation.
	 * @author Trixt0r
	 */
	inner class ObjectIterator : MutableIterator<Object> {
		var index = 0

		override fun hasNext(): Boolean {
			return index < currentKey!!.objectRefs.size
		}


		override fun next(): Object {
			return unmappedTweenedKeys[currentKey!!.objectRefs[index++].timeline].`object`()
		}


		override fun remove() {
			throw SpriterException("remove() is not supported by this iterator!")
		}

	}

	/**
	 * An iterator to iterate over all time line bones in the current animation.
	 * @author Trixt0r
	 */
	inner class BoneIterator : MutableIterator<Bone> {
		var index = 0

		override fun hasNext(): Boolean {
			return index < currentKey!!.boneRefs.size
		}

		override fun next(): Bone {
			return unmappedTweenedKeys[currentKey!!.boneRefs[index++].timeline].`object`()
		}

		override fun remove() {
			throw SpriterException("remove() is not supported by this iterator!")
		}
	}

	/**
	 * A listener to listen for specific events which can occur during the runtime of a [Player] instance.
	 * @author Trixt0r
	 */
	interface PlayerListener {

		/**
		 * Gets called if the current animation has reached it's end or it's beginning (depends on the current set [Player.speed]).
		 * @param animation the animation which finished.
		 */
		fun animationFinished(animation: Animation)

		/**
		 * Gets called if the animation of the player gets changed.
		 * If [Player.setAnimation] gets called and the new animation is the same as the previous one, this method will not be called.
		 * @param oldAnim the old animation
		 * *
		 * @param newAnim the new animation
		 */
		fun animationChanged(oldAnim: Animation, newAnim: Animation)

		/**
		 * Gets called before a player updates the current animation.
		 * @param player the player which is calling this method.
		 */
		fun preProcess(player: Player)

		/**
		 * Gets called after a player updated the current animation.
		 * @param player the player which is calling this method.
		 */
		fun postProcess(player: Player)

		/**
		 * Gets called if the mainline key gets changed.
		 * If [Player.speed] is big enough it can happen that mainline keys between the previous and the new mainline key will be ignored.
		 * @param prevKey the previous mainline key
		 * *
		 * @param newKey the new mainline key
		 */
		fun mainlineKeyChanged(prevKey: Mainline.Key?, newKey: Mainline.Key?)
	}

	/**
	 * An attachment is an abstract object which can be attached to a [Player] object.
	 * An attachment extends a [Bone] which means that [Bone.position], [Bone.scale] and [Bone._angle] can be set to change the relative position to its [Attachment.parent]
	 * The [Player] object will make sure that the attachment will be transformed relative to its [Attachment.parent].
	 * @author Trixt0r
	 */
	abstract class Attachment
	/**
	 * Creates a new attachment
	 * @param parent the parent of this attachment
	 */
		(parent: Bone) : Bone() {

		/**
		 * Returns the current set parent.
		 * @return the parent
		 */
		/**
		 * Sets the parent of this attachment.
		 * *
		 * @throws SpriterException if parent is `null`
		 */
		var parent: Bone? = parent
			set(parent) {
				if (parent == null) throw SpriterException("The parent cannot be null!")
				field = parent
			}
		private val positionTemp: Point = Point()
		private val scaleTemp: Point = Point()
		private var angleTemp: Float = 0.toFloat()

		fun update() {
			//Save relative positions
			this.positionTemp.set(super.position)
			this.scaleTemp.set(super.scale)
			this.angleTemp = super._angle

			super.unmap(this.parent!!)
			this.setPosition(super.position.x, super.position.y)
			this.setScale(super.scale.x, super.scale.y)
			this.setAngle(super._angle)

			//Load realtive positions
			super.position.set(this.positionTemp)
			super.scale.set(this.scaleTemp)
			super._angle = this.angleTemp
		}

		/**
		 * Sets the position to the given coordinates.
		 * @param x the x coordinate
		 * *
		 * @param y the y coordinate
		 */
		protected abstract fun setPosition(x: Float, y: Float)

		/**
		 * Sets the scale to the given scale.
		 * @param xscale the scale in x direction
		 * *
		 * @param yscale the scale in y direction
		 */
		protected abstract fun setScale(xscale: Float, yscale: Float)

		/**
		 * Sets the angle to the given one.
		 * @param angle the angle in degrees
		 */
		protected abstract fun setAngle(angle: Float)
	}
}
