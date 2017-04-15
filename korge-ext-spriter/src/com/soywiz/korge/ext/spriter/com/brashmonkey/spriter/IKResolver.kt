package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter

import java.util.HashMap
import kotlin.collections.Map.Entry

import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Mainline.Key.BoneRef
import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Timeline.Key.Bone

/**
 * A IKResolver is responsible for resolving previously set constraints.
 * @see [ Inverse kinematics](http://en.wikipedia.org/wiki/Inverse_kinematics)

 * @author Trixt0r
 */
abstract class IKResolver
/**
 * Creates a resolver with a default tolerance of 5f.
 */
(var player: Player) {

	/**
	 * Resolves the inverse kinematics constraint with a specific algtorithm
	 * @param x the target x value
	 * *
	 * @param y the target y value
	 * *
	 * @param chainLength number of parents which are affected
	 * *
	 * @param effector the actual effector where the resolved information has to be stored in.
	 */
	protected abstract fun resolve(x: Float, y: Float, chainLength: Int, effector: BoneRef)

	protected var ikMap: HashMap<IKObject, BoneRef> = HashMap<IKObject, BoneRef>()
	/**
	 * Returns the tolerance of this resolver.
	 * @return the tolerance
	 */
	/**
	 * Sets the tolerance distance of this resolver.
	 * The resolver should stop the algorithm if the distance to the set ik object is less than the tolerance.
	 * @param tolerance the tolerance
	 */
	var tolerance: Float = 5f

	/**
	 * Resolves the inverse kinematics constraints with the implemented algorithm in [.resolve].
	 * @param player player to apply the resolving.
	 */
	fun resolve() {
		for ((key, value) in this.ikMap) {
			for (j in 0..key.getIterations() - 1)
				this.resolve(key.x, key.y, key.chainLength, value)
		}
	}

	/**
	 * Adds the given object to the internal IKObject - Bone map.
	 * This means, the values of the given ik object affect the mapped bone.
	 * @param ikObject the ik object
	 * *
	 * @param boneRef the bone reference which gets affected
	 */
	fun mapIKObject(ikObject: IKObject, boneRef: BoneRef) {
		this.ikMap.put(ikObject, boneRef)
	}

	/**
	 * Adds the given object to the internal IKObject - Bone map.
	 * This means, the values of the given ik object affect the mapped bone.
	 * @param ikObject the ik object
	 * *
	 * @param bone the bone which gets affected
	 */
	fun mapIKObject(ikObject: IKObject, bone: Bone) {
		this.ikMap.put(ikObject, player.getBoneRef(bone)!!)
	}

	/**
	 * Removes the given object from the internal map.
	 * @param ikObject the ik object to remove
	 */
	fun unmapIKObject(ikObject: IKObject) {
		this.ikMap.remove(ikObject)
	}

}
