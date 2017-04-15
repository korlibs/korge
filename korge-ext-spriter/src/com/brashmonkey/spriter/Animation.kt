package com.brashmonkey.spriter

import java.util.HashMap

import com.brashmonkey.spriter.Mainline.Key
import com.brashmonkey.spriter.Mainline.Key.BoneRef
import com.brashmonkey.spriter.Mainline.Key.ObjectRef
import com.brashmonkey.spriter.Timeline.Key.Bone
import com.brashmonkey.spriter.Timeline.Key.Object

/**
 * Represents an animation of a Spriter SCML file.
 * An animation holds [Timeline]s and a [Mainline] to animate objects.
 * Furthermore it holds an [.id], a [.length], a [.name] and whether it is [.looping] or not.
 * @author Trixt0r
 */
open class Animation(@JvmField val mainline: Mainline, @JvmField val id: Int, @JvmField val name: String, @JvmField val length: Int, @JvmField val looping: Boolean, timelines: Int) {
	companion object {
		val DUMMY = Animation(Mainline.DUMMY, 0, "", 0, false, 0)
	}

	private val timelines: Array<Timeline> = Array<Timeline>(timelines) { Timeline.DUMMY }
	private var timelinePointer = 0
	private val nameToTimeline: HashMap<String, Timeline> = HashMap<String, Timeline>()
	@JvmField var currentKey: Key = Key.DUMMY
	@JvmField var tweenedKeys: Array<Timeline.Key> = emptyArray()
	@JvmField var unmappedTweenedKeys: Array<Timeline.Key> = emptyArray()
	private var prepared: Boolean = false

	/**
	 * Returns a [Timeline] with the given index.
	 * @param index the index of the timeline
	 * *
	 * @return the timeline with the given index
	 * *
	 * @throws IndexOutOfBoundsException if the index is out of range
	 */
	fun getTimeline(index: Int): Timeline {
		return this.timelines[index]
	}

	/**
	 * Returns a [Timeline] with the given name.
	 * @param name the name of the time line
	 * *
	 * @return the time line with the given name or null if no time line exists with the given name.
	 */
	fun getTimeline(name: String): Timeline? {
		return this.nameToTimeline[name]
	}

	fun addTimeline(timeline: Timeline) {
		this.timelines[timelinePointer++] = timeline
		this.nameToTimeline.put(timeline.name, timeline)
	}

	/**
	 * Returns the number of time lines this animation holds.
	 * @return the number of time lines
	 */
	fun timelines(): Int {
		return timelines.size
	}

	override fun toString(): String {
		var toReturn = javaClass.simpleName + "|[id: " + id + ", " + name + ", duration: " + length + ", is looping: " + looping
		toReturn += "Mainline:\n"
		toReturn += mainline
		toReturn += "Timelines\n"
		for (timeline in this.timelines)
			toReturn += timeline
		toReturn += "]"
		return toReturn
	}

	/**
	 * Updates the bone and object structure with the given time to the given root bone.
	 * @param time The time which has to be between 0 and [.length] to work properly.
	 * *
	 * @param root The root bone which is not allowed to be null. The whole animation runs relative to the root bone.
	 */
	open fun update(time: Int, root: Bone?) {
		if (!this.prepared) throw SpriterException("This animation is not ready yet to animate itself. Please call prepare()!")
		if (root == null) throw SpriterException("The root can not be null! Set a root bone to apply this animation relative to the root bone.")
		this.currentKey = mainline.getKeyBeforeTime(time)

		for (timelineKey in this.unmappedTweenedKeys)
			timelineKey.active = false
		for (ref in currentKey.boneRefs)
			this.update(ref, root, time)
		for (ref in currentKey.objectRefs)
			this.update(ref, root, time)
	}

	protected open fun update(ref: BoneRef, root: Bone, time: Int) {
		val isObject = ref is ObjectRef
		//Get the timelines, the refs pointing to
		val timeline = getTimeline(ref.timeline)
		val key = timeline.getKey(ref.key)
		var nextKey: Timeline.Key = timeline.getKey((ref.key + 1) % timeline.keys.size)
		val currentTime = key.time
		var nextTime = nextKey.time
		if (nextTime < currentTime) {
			if (!looping)
				nextKey = key
			else
				nextTime = length
		}
		//Normalize the time
		var t = (time - currentTime).toFloat() / (nextTime - currentTime).toFloat()
		if (java.lang.Float.isNaN(t) || java.lang.Float.isInfinite(t)) t = 1f
		if (currentKey.time > currentTime) {
			var tMid = (currentKey.time - currentTime).toFloat() / (nextTime - currentTime).toFloat()
			if (java.lang.Float.isNaN(tMid) || java.lang.Float.isInfinite(tMid)) tMid = 0f
			t = (time - currentKey.time).toFloat() / (nextTime - currentKey.time).toFloat()
			if (java.lang.Float.isNaN(t) || java.lang.Float.isInfinite(t)) t = 1f
			t = currentKey.curve.tween(tMid, 1f, t)
		} else
			t = currentKey.curve.tween(0f, 1f, t)
		//Tween bone/object
		val bone1 = key.`object`()
		val bone2 = nextKey.`object`()
		val tweenTarget = this.tweenedKeys[ref.timeline].`object`()
		if (isObject)
			this.tweenObject(bone1 as Object, bone2 as Object, tweenTarget as Object, t, key.curve, key.spin)
		else
			this.tweenBone(bone1 as Bone, bone2 as Bone, tweenTarget as Bone, t, key.curve, key.spin)
		this.unmappedTweenedKeys[ref.timeline].active = true
		this.unmapTimelineObject(ref.timeline, isObject, if (ref.parent != null)
			this.unmappedTweenedKeys[ref.parent.timeline].`object`() as Bone
		else
			root)
	}

	fun unmapTimelineObject(timeline: Int, isObject: Boolean, root: Bone) {
		val tweenTarget = this.tweenedKeys[timeline].`object`()
		val mapTarget = this.unmappedTweenedKeys[timeline].`object`()
		if (isObject)
			(mapTarget as Object).set(tweenTarget as Object)
		else
			(mapTarget as Bone).set(tweenTarget as Bone)
		mapTarget.unmap(root)
	}

	protected fun tweenBone(bone1: Bone, bone2: Bone, target: Bone, t: Float, curve: Curve, spin: Int) {
		target.angle = curve.tweenAngle(bone1.angle, bone2.angle, t, spin)
		curve.tweenPoint(bone1.position, bone2.position, t, target.position)
		curve.tweenPoint(bone1.scale, bone2.scale, t, target.scale)
		curve.tweenPoint(bone1.pivot, bone2.pivot, t, target.pivot)
	}

	protected fun tweenObject(object1: Object, object2: Object, target: Object, t: Float, curve: Curve, spin: Int) {
		this.tweenBone(object1, object2, target, t, curve, spin)
		target.alpha = curve.tweenAngle(object1.alpha, object2.alpha, t)
		target.ref.set(object1.ref)
	}

	fun getSimilarTimeline(t: Timeline): Timeline? {
		var found: Timeline? = getTimeline(t.name)
		if (found == null && t.id < this.timelines()) found = this.getTimeline(t.id)
		return found
	}

	/*Timeline getSimilarTimeline(BoneRef ref, Collection<Timeline> coveredTimelines){
		if(ref.parent == null) return null;
    	for(BoneRef boneRef: this.currentKey.objectRefs){
    		Timeline t = this.getTimeline(boneRef.timeline);
    		if(boneRef.parent != null && boneRef.parent.id == ref.parent.id && !coveredTimelines.contains(t))
    			return t;
    	}
    	return null;
	}

	Timeline getSimilarTimeline(ObjectRef ref, Collection<Timeline> coveredTimelines){
		if(ref.parent == null) return null;
    	for(ObjectRef objRef: this.currentKey.objectRefs){
    		Timeline t = this.getTimeline(objRef.timeline);
    		if(objRef.parent != null && objRef.parent.id == ref.parent.id && !coveredTimelines.contains(t))
    			return t;
    	}
    	return null;
	}*/

	/**
	 * Prepares this animation to set this animation in any time state.
	 * This method has to be called before [.update].
	 */
	fun prepare() {
		if (this.prepared) return
		this.tweenedKeys = Array<Timeline.Key>(timelines.size) { Timeline.Key.DUMMY }
		this.unmappedTweenedKeys = Array<Timeline.Key>(timelines.size) { Timeline.Key.DUMMY }

		for (i in this.tweenedKeys.indices) {
			this.tweenedKeys[i] = Timeline.Key(i)
			this.unmappedTweenedKeys[i] = Timeline.Key(i)
			this.tweenedKeys[i].setObject(Object(Point(0f, 0f)))
			this.unmappedTweenedKeys[i].setObject(Object(Point(0f, 0f)))
		}
		if (mainline.keys.isNotEmpty()) currentKey = mainline.getKey(0)
		this.prepared = true
	}

}
