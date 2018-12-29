package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter

import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Mainline.Key.*
import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Timeline.Key.*
import com.soywiz.korma.geom.*
import kotlin.math.*

/**
 * An inverse kinematics resolver implementation.
 * An instance of this class uses the CCD (Cyclic Coordinate Descent) algorithm to resolve the constraints.
 * @see [ccd-algorithm](https://sites.google.com/site/auraliusproject/ccd-algorithm)
 * and [cyclic-coordinate-descent-in-2d](http://www.ryanjuckett.com/programming/cyclic-coordinate-descent-in-2d/) .

 * @author Trixt0r
 */
@Suppress("unused")
class CCDResolver(player: Player) : IKResolver(player) {

	public override fun resolve(x: Float, y: Float, chainLength: Int, effector: BoneRef) {
		//player.unmapObjects(null);
		val timeline = player._animation.getTimeline(effector.timeline)
		val key = player.tweenedKeys[effector.timeline]
		val unmappedKey = player.unmappedTweenedKeys[effector.timeline]
		val teffector = key.`object`()
		val unmappedffector = unmappedKey.`object`()
		var width = timeline.objectInfo.size.width
		width *= unmappedffector.scale.x
		var xx = unmappedffector.position.x + cos(Angle.degreesToRadians(unmappedffector._angle.toDouble())).toFloat() * width
		var yy = unmappedffector.position.y + sin(Angle.degreesToRadians(unmappedffector._angle.toDouble())).toFloat() * width
		if (Calculator.distanceBetween(xx, yy, x, y) <= this.tolerance)
			return

		teffector._angle = Calculator.angleBetween(unmappedffector.position.x, unmappedffector.position.y, x, y)
		if (sign(player.root.scale.x) == -1f) teffector._angle += 180f
		var parentRef: BoneRef? = effector.parent
		var parent: Bone? = null
		var unmappedParent: Bone? = null
		if (parentRef != null) {
			parent = player.tweenedKeys[parentRef.timeline].`object`()
			unmappedParent = player.unmappedTweenedKeys[parentRef.timeline].`object`()
			teffector._angle -= unmappedParent._angle
		}
		player.unmapObjects(null)
		var i = 0
		while (i < chainLength && parentRef != null) {
			if (Calculator.distanceBetween(xx, yy, x, y) <= this.tolerance)
				return
			parent!!._angle += Calculator.angleDifference(
				Calculator.angleBetween(unmappedParent!!.position.x, unmappedParent.position.y, x, y),
				Calculator.angleBetween(unmappedParent.position.x, unmappedParent.position.y, xx, yy)
			)
			parentRef = parentRef.parent
			if (parentRef != null && i < chainLength - 1) {
				parent = player.tweenedKeys[parentRef.timeline].`object`()
				unmappedParent = player.unmappedTweenedKeys[parentRef.timeline].`object`()
				parent._angle -= unmappedParent._angle
			} else
				parent = null
			player.unmapObjects(null)
			xx = unmappedffector.position.x + cos(Angle.degreesToRadians(unmappedffector._angle.toDouble())).toFloat() * width
			yy = unmappedffector.position.y + sin(Angle.degreesToRadians(unmappedffector._angle.toDouble())).toFloat() * width
			i++
		}
	}

}
