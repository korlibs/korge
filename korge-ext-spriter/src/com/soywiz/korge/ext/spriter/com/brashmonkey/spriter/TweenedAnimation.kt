package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter

import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Mainline.Key.BoneRef
import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Mainline.Key.ObjectRef
import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Timeline.Key.Bone
import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Timeline.Key.Object

/**
 * A tweened animation is responsible for updating itself based on two given animations.
 * The values of the two given animations will get interpolated and save in this animation.
 * When tweening two animations, you have to make sure that they have the same structure.
 * The best result is achieved if bones of two different animations are named in the same way.
 * There are still issues with sprites, which are hard to resolve since Spriter does not save them in a useful order or naming convention.
 * @author Trixt0r
 */
class TweenedAnimation
/**
 * Creates a tweened animation based on the given entity.
 * @param entity the entity animations have to be part of
 */
(
	/**
	 * The entity the animations have be part of.
	 * Animations of two different entities can not be tweened.
	 */
	val entity: Entity) : Animation(Mainline(0), -1, "__interpolatedAnimation__", 0, true, entity.animationWithMostTimelines.timelines()) {

	/**
	 * The weight of the interpolation. 0.5f is the default value.
	 * Values closer to 0.0f mean the first animation will have more influence.
	 */
	var weight = .5f

	/**
	 * Indicates when a sprite should be switched form the first animation object to the second one.
	 * A value closer to 0.0f means that the sprites of the second animation will be drawn.
	 */
	var spriteThreshold = .5f

	/**
	 * The curve which will tween the animations.
	 * The default type of the curve is [Curve.Type.Linear].
	 */
	val curve: Curve
	/**
	 * Returns the first animation.
	 * @return the first animation
	 */
	var firstAnimation: Animation? = null
		private set
	/**
	 * Returns the second animation.
	 * @return the second animation
	 */
	var secondAnimation: Animation? = null
		private set

	/**
	 * The base animation an object or bone will get if it will not be tweened.
	 */
	var baseAnimation: Animation? = null
	internal var base: BoneRef? = null

	/**
	 * Indicates whether to tween sprites or not. Default value is `false`.
	 * Tweening sprites should be only enabled if they have exactly the same structure.
	 * If all animations are bone based and sprites only change their references it is not recommended to tween sprites.
	 */
	var tweenSprites = false

	init {
		this.curve = Curve()
		this.setUpTimelines()
	}

	override fun update(time: Int, root: Bone?) {
		super.currentKey = if (onFirstMainLine()) firstAnimation!!.currentKey else secondAnimation!!.currentKey
		for (timelineKey in this.unmappedTweenedKeys)
			timelineKey.active = false
		if (base != null) {//TODO: Sprites not working properly because of different timeline naming
			val currentAnim = if (onFirstMainLine()) firstAnimation else secondAnimation
			val baseAnim = if (baseAnimation == null) if (onFirstMainLine()) firstAnimation else secondAnimation else baseAnimation
			currentAnim!!
			baseAnim!!
			for (ref in currentKey.boneRefs) {
				val timeline = baseAnim.getSimilarTimeline(currentAnim.getTimeline(ref.timeline)) ?: continue
				val key: Timeline.Key
				val mappedKey: Timeline.Key
				key = baseAnim.tweenedKeys[timeline.id]
				mappedKey = baseAnim.unmappedTweenedKeys[timeline.id]
				this.tweenedKeys[ref.timeline].active = key.active
				this.tweenedKeys[ref.timeline].`object`()!!.set(key.`object`()!!)
				this.unmappedTweenedKeys[ref.timeline].active = mappedKey.active
				this.unmapTimelineObject(ref.timeline, false, if (ref.parent != null)
					this.unmappedTweenedKeys[ref.parent.timeline].`object`() as Bone
				else
					root as Bone)
			}
			/*for(ObjectRef ref: baseAnim.currentKey.objectRefs){
	        	Timeline timeline = baseAnim.getTimeline(ref.timeline);//getSimilarTimeline(ref, tempTimelines);
	        	if(timeline != null){
	        		//tempTimelines.addLast(timeline);
	        		Timeline.Key key = baseAnim.tweenedKeys[timeline.id];
	        		Timeline.Key mappedKey = baseAnim.mappedTweenedKeys[timeline.id];
	        		Object obj = (Object) key.object();

		    		this.tweenedKeys[ref.timeline].active = key.active;
		    		((Object)this.tweenedKeys[ref.timeline].object()).set(obj);
		    		this.mappedTweenedKeys[ref.timeline].active = mappedKey.active;
					this.unmapTimelineObject(ref.timeline, true,(ref.parent != null) ?
							this.mappedTweenedKeys[ref.parent.timeline].object(): root);
	        	}
	    	}*/
			//tempTimelines.clear();
		}

		this.tweenBoneRefs(base, root!!)
		for (ref in super.currentKey.objectRefs) {
			//if(ref.parent == base)
			this.update(ref, root!!, 0)
		}
	}

	private fun tweenBoneRefs(base: BoneRef?, root: Bone) {
		val startIndex = if (base == null) -1 else base.id - 1
		val length = super.currentKey.boneRefs.size
		for (i in startIndex + 1..length - 1) {
			val ref = currentKey.boneRefs[i]
			if (base === ref || ref.parent === base) this.update(ref, root, 0)
			if (base === ref.parent) this.tweenBoneRefs(ref, root)
		}
	}

	override fun update(ref: BoneRef, root: Bone, time: Int) {
		val isObject = ref is ObjectRef
		//Tween bone/object
		var bone1: Bone? = null
		var bone2: Bone? = null
		var tweenTarget: Bone? = null
		val t1 = if (onFirstMainLine()) firstAnimation!!.getTimeline(ref.timeline) else firstAnimation!!.getSimilarTimeline(secondAnimation!!.getTimeline(ref.timeline))
		val t2 = if (onFirstMainLine()) secondAnimation!!.getSimilarTimeline(t1!!) else secondAnimation!!.getTimeline(ref.timeline)
		val targetTimeline = super.getTimeline(if (onFirstMainLine()) t1!!.id else t2!!.id)
		if (t1 != null) bone1 = firstAnimation!!.tweenedKeys[t1.id].`object`()
		if (t2 != null) bone2 = secondAnimation!!.tweenedKeys[t2.id].`object`()
		if (targetTimeline != null) tweenTarget = this.tweenedKeys[targetTimeline.id].`object`()
		if (isObject && (t2 == null || !tweenSprites)) {
			if (!onFirstMainLine())
				bone1 = bone2
			else
				bone2 = bone1
		}
		if (bone2 != null && tweenTarget != null && bone1 != null) {
			if (isObject)
				this.tweenObject(bone1 as Object, bone2 as Object, tweenTarget as Object, this.weight, this.curve)
			else
				this.tweenBone(bone1, bone2, tweenTarget, this.weight, this.curve)
			this.unmappedTweenedKeys[targetTimeline.id].active = true
		}
		//Transform the bone relative to the parent bone or the root
		if (this.unmappedTweenedKeys[ref.timeline].active) {
			this.unmapTimelineObject(targetTimeline.id, isObject, if (ref.parent != null)
				this.unmappedTweenedKeys[ref.parent.timeline].`object`()!!
			else
				root)
		}
	}

	private fun tweenBone(bone1: Bone, bone2: Bone, target: Bone, t: Float, curve: Curve) {
		target._angle = curve.tweenAngle(bone1._angle, bone2._angle, t)
		curve.tweenPoint(bone1.position, bone2.position, t, target.position)
		curve.tweenPoint(bone1.scale, bone2.scale, t, target.scale)
		curve.tweenPoint(bone1.pivot, bone2.pivot, t, target.pivot)
	}

	private fun tweenObject(object1: Object, object2: Object, target: Object, t: Float, curve: Curve) {
		this.tweenBone(object1, object2, target, t, curve)
		target.alpha = curve.tweenAngle(object1.alpha, object2.alpha, t)
		target.ref.set(object1.ref)
	}

	/**
	 * Returns whether the current mainline key is the one from the first animation or from the second one.
	 * @return `true` if the mainline key is the one from the first animation
	 */
	fun onFirstMainLine(): Boolean {
		return this.weight < this.spriteThreshold
	}

	private fun setUpTimelines() {
		val maxAnim = this.entity.animationWithMostTimelines
		val max = maxAnim.timelines()
		for (i in 0..max - 1) {
			val t = Timeline(i, maxAnim.getTimeline(i).name, maxAnim.getTimeline(i).objectInfo, 1)
			addTimeline(t)
		}
		prepare()
	}

	/**
	 * Sets the animations to tween.
	 * @param animation1 the first animation
	 * *
	 * @param animation2 the second animation
	 * *
	 * @throws SpriterException if [.entity] does not contain one of the given animations.
	 */
	fun setAnimations(animation1: Animation, animation2: Animation) {
		val areInterpolated = animation1 is TweenedAnimation || animation2 is TweenedAnimation
		if (animation1 === firstAnimation && animation2 === secondAnimation) return
		if ((!this.entity.containsAnimation(animation1) || !this.entity.containsAnimation(animation2)) && !areInterpolated)
			throw SpriterException("Both animations have to be part of the same entity!")
		this.firstAnimation = animation1
		this.secondAnimation = animation2
	}

}
