package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter

import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Mainline.Key.BoneRef
import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Timeline.Key.Bone

/**
 * An inverse kinematics resolver implementation.
 * An instance of this class uses the CCD (Cyclic Coordinate Descent) algorithm to resolve the constraints.
 * @see [ccd-algorithm](https://sites.google.com/site/auraliusproject/ccd-algorithm)
 * and [cyclic-coordinate-descent-in-2d](http://www.ryanjuckett.com/programming/cyclic-coordinate-descent-in-2d/) .

 * @author Trixt0r
 */
class CCDResolver(player: Player) : IKResolver(player) {

	public override fun resolve(x: Float, y: Float, chainLength: Int, effectorRef: BoneRef) {
		//player.unmapObjects(null);
		val timeline = player._animation.getTimeline(effectorRef.timeline)
		val key = player.tweenedKeys[effectorRef.timeline]
		val unmappedKey = player.unmappedTweenedKeys[effectorRef.timeline]
		val effector = key.`object`()!!
		val unmappedffector = unmappedKey.`object`()!!
		var width = if (timeline.objectInfo != null) timeline.objectInfo.size.width else 200f
		width *= unmappedffector.scale.x
		var xx = unmappedffector.position.x + Math.cos(Math.toRadians(unmappedffector._angle.toDouble())).toFloat() * width
		var yy = unmappedffector.position.y + Math.sin(Math.toRadians(unmappedffector._angle.toDouble())).toFloat() * width
		if (Calculator.distanceBetween(xx, yy, x, y) <= this.tolerance)
			return

		effector._angle = Calculator.angleBetween(unmappedffector.position.x, unmappedffector.position.y, x, y)
		if (Math.signum(player.root.scale.x) == -1f) effector._angle += 180f
		var parentRef: BoneRef? = effectorRef.parent
		var parent: Bone? = null
		var unmappedParent: Bone? = null
		if (parentRef != null) {
			parent = player.tweenedKeys[parentRef.timeline].`object`()
			unmappedParent = player.unmappedTweenedKeys[parentRef.timeline].`object`()
			effector._angle -= unmappedParent!!._angle
		}
		player.unmapObjects(null)
		var i = 0
		while (i < chainLength && parentRef != null) {
			if (Calculator.distanceBetween(xx, yy, x, y) <= this.tolerance)
				return
			parent!!._angle += Calculator.angleDifference(Calculator.angleBetween(unmappedParent!!.position.x, unmappedParent.position.y, x, y),
				Calculator.angleBetween(unmappedParent.position.x, unmappedParent.position.y, xx, yy))
			parentRef = parentRef.parent
			if (parentRef != null && i < chainLength - 1) {
				parent = player.tweenedKeys[parentRef.timeline].`object`()
				unmappedParent = player.unmappedTweenedKeys[parentRef.timeline].`object`()
				parent!!._angle -= unmappedParent!!._angle
			} else
				parent = null
			player.unmapObjects(null)
			xx = unmappedffector.position.x + Math.cos(Math.toRadians(unmappedffector._angle.toDouble())).toFloat() * width
			yy = unmappedffector.position.y + Math.sin(Math.toRadians(unmappedffector._angle.toDouble())).toFloat() * width
			i++
		}
	}

}
