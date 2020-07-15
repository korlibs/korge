package org.jbox2d.particle

import com.soywiz.korma.geom.*
import org.jbox2d.collision.shapes.*
import org.jbox2d.common.*
import org.jbox2d.userdata.*

/**
 * A particle group definition holds all the data needed to construct a particle group. You can
 * safely re-use these definitions.
 */
class ParticleGroupDef : Box2dTypedUserData by Box2dTypedUserData.Mixin() {

    /** The particle-behavior flags.  */

    var flags: Int = 0

    /** The group-construction flags.  */

    var groupFlags: Int = 0

    /**
     * The world position of the group. Moves the group's shape a distance equal to the value of
     * position.
     */

    val position = Vec2()

    /**
     * The world angle of the group in radians. Rotates the shape by an angle equal to the value of
     * angle.
     */
    var angleRadians: Float = 0f

    /**
     * The world angle of the group in degrees. Rotates the shape by an angle equal to the value of
     * angle.
     */
    var angleDegrees: Float
        set(value) = run { angleRadians = value * MathUtils.DEG2RAD }
        get() = angleRadians * MathUtils.RAD2DEG

    /**
     * The world angle of the group. Rotates the shape by an angle equal to the value of
     * angle.
     */
    var angle: Angle
        set(value) = run { angleRadians = value.radians.toFloat() }
        get() = angleRadians.radians

    /** The linear velocity of the group's origin in world co-ordinates.  */

    val linearVelocity = Vec2()

    /** The angular velocity of the group.  */

    var angularVelocity: Float = 0f

    /** The color of all particles in the group.  */

    var color: ParticleColor? = null

    /**
     * The strength of cohesion among the particles in a group with flag b2_elasticParticle or
     * b2_springParticle.
     */

    var strength: Float = 1f

    /** Shape containing the particle group.  */

    var shape: Shape? = null

    /** If true, destroy the group automatically after its last particle has been destroyed.  */

    var destroyAutomatically: Boolean = true

    /** Use this to store application-specific group data.  */

    var userData: Any? = null
}
