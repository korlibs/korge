package org.jbox2d.dynamics.joints

import org.jbox2d.common.Vec2

/**
 * Rope joint definition. This requires two body anchor points and a maximum lengths. Note: by
 * default the connected objects will not collide. see collideConnected in b2JointDef.
 *
 * @author Daniel Murphy
 */
class RopeJointDef : JointDef(JointType.ROPE) {

    /**
     * The local anchor point relative to bodyA's origin.
     */

    val localAnchorA = Vec2(-1f, 0f)

    /**
     * The local anchor point relative to bodyB's origin.
     */

    val localAnchorB = Vec2(1f, 0f)

    /**
     * The maximum length of the rope. Warning: this must be larger than b2_linearSlop or the joint
     * will have no effect.
     */

    var maxLength: Float = 0f
}
