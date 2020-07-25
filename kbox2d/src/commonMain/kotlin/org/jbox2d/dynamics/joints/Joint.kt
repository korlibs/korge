/*******************************************************************************
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.jbox2d.dynamics.joints

import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.SolverData
import org.jbox2d.dynamics.World
import org.jbox2d.internal.*
import org.jbox2d.pooling.*
import org.jbox2d.userdata.*

/**
 * The base joint class. Joints are used to constrain two bodies together in various fashions. Some
 * joints also feature limits and motors.
 *
 * @author Daniel Murphy
 */
abstract class Joint protected constructor(
    // Cache here per time step to reduce cache misses.
    protected var pool: IWorldPool,
    def: JointDef
) : Box2dTypedUserData by Box2dTypedUserData.Mixin() {

    /**
     * get the type of the concrete joint.
     *
     * @return
     */
    val type: JointType

    var prev: Joint? = null

    /**
     * get the next joint the world joint list.
     */
    var next: Joint? = null

    var edgeA: JointEdge
    var edgeB: JointEdge

    /**
     * get the first body attached to this joint.
     */
    var bodyA: Body? = null

    /**
     * get the second body attached to this joint.
     */
    var bodyB: Body? = null

    var islandFlag: Boolean = false

    /**
     * Get collide connected. Note: modifying the collide connect flag won't work correctly because
     * the flag is only checked when fixture AABBs begin to overlap.
     */

    val _collideConnected: Boolean

    fun getCollideConnected() = _collideConnected

    var userData: Any? = null

    /**
     * Short-cut function to determine if either body is inactive.
     */
    val isActive: Boolean
        get() = bodyA!!.isActive && bodyB!!.isActive

    init {
        assert(def.bodyA !== def.bodyB)
        this.type = def.type
        prev = null
        next = null
        bodyA = def.bodyA
        bodyB = def.bodyB
        _collideConnected = def.collideConnected
        islandFlag = false
        userData = def.userData

        edgeA = JointEdge()
        edgeA.joint = null
        edgeA.other = null
        edgeA.prev = null
        edgeA.next = null

        edgeB = JointEdge()
        edgeB.joint = null
        edgeB.other = null
        edgeB.prev = null
        edgeB.next = null

        // m_localCenterA = new Vec2();
        // m_localCenterB = new Vec2();
    }

    /**
     * get the anchor point on bodyA in world coordinates.
     */
    abstract fun getAnchorA(out: Vec2)

    /**
     * get the anchor point on bodyB in world coordinates.
     */
    abstract fun getAnchorB(out: Vec2)

    /**
     * get the reaction force on body2 at the joint anchor in Newtons.
     */
    abstract fun getReactionForce(inv_dt: Float, out: Vec2)

    /**
     * get the reaction torque on body2 in N*m.
     */
    abstract fun getReactionTorque(inv_dt: Float): Float

    /** Internal  */
    abstract fun initVelocityConstraints(data: SolverData)

    /** Internal  */
    abstract fun solveVelocityConstraints(data: SolverData)

    /**
     * This returns true if the position errors are within tolerance. Internal.
     */
    abstract fun solvePositionConstraints(data: SolverData): Boolean

    /**
     * Override to handle destruction of joint
     */
    open fun destructor() {}

    companion object {

        fun create(world: World, def: JointDef): Joint? {
            // Joint joint = null;
            when (def.type) {
                JointType.MOUSE -> return MouseJoint(world.pool, def as MouseJointDef)
                JointType.DISTANCE -> return DistanceJoint(world.pool, def as DistanceJointDef)
                JointType.PRISMATIC -> return PrismaticJoint(world.pool, def as PrismaticJointDef)
                JointType.REVOLUTE -> return RevoluteJoint(world.pool, def as RevoluteJointDef)
                JointType.WELD -> return WeldJoint(world.pool, def as WeldJointDef)
                JointType.FRICTION -> return FrictionJoint(world.pool, def as FrictionJointDef)
                JointType.WHEEL -> return WheelJoint(world.pool, def as WheelJointDef)
                JointType.GEAR -> return GearJoint(world.pool, def as GearJointDef)
                JointType.PULLEY -> return PulleyJoint(world.pool, def as PulleyJointDef)
                JointType.CONSTANT_VOLUME -> return ConstantVolumeJoint(world, def as ConstantVolumeJointDef)
                JointType.ROPE -> return RopeJoint(world.pool, def as RopeJointDef)
                JointType.MOTOR -> return MotorJoint(world.pool, def as MotorJointDef)
                JointType.UNKNOWN -> return null
                else -> return null
            }
        }


        fun destroy(joint: Joint) {
            joint.destructor()
        }
    }
}
