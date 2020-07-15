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

import com.soywiz.korma.geom.*
import org.jbox2d.common.*
import org.jbox2d.dynamics.Body

/**
 * Prismatic joint definition. This requires defining a line of motion using an axis and an anchor
 * point. The definition uses local anchor points and a local axis so that the initial configuration
 * can violate the constraint slightly. The joint translation is zero when the local anchor points
 * coincide in world space. Using local anchors and a local axis helps when saving and loading a
 * game.
 *
 * @warning at least one body should by dynamic with a non-fixed rotation.
 * @author Daniel
 */
class PrismaticJointDef : JointDef(JointType.PRISMATIC) {


    /**
     * The local anchor point relative to body1's origin.
     */

    val localAnchorA: Vec2 = Vec2()

    /**
     * The local anchor point relative to body2's origin.
     */

    val localAnchorB: Vec2 = Vec2()

    /**
     * The local translation axis in body1.
     */

    val localAxisA: Vec2 = Vec2(1.0f, 0.0f)

    /**
     * The constrained angle in radians between the bodies: body2_angle - body1_angle.
     */
    var referenceAngleRadians: Float = 0f

    /**
     * The constrained angle in degrees between the bodies: body2_angle - body1_angle.
     */
    var referenceAngleDegrees: Float
        set(value) = run { referenceAngleRadians = value * MathUtils.DEG2RAD }
        get() = referenceAngleRadians * MathUtils.RAD2DEG

    /**
     * The constrained angle between the bodies: body2_angle - body1_angle.
     */
    var referenceAngle: Angle
        set(value) = run { referenceAngleRadians = value.radians.toFloat() }
        get() = referenceAngleRadians.radians

    /**
     * Enable/disable the joint limit.
     */

    var enableLimit: Boolean = false

    /**
     * The lower translation limit, usually in meters.
     */

    var lowerTranslation: Float = 0f

    /**
     * The upper translation limit, usually in meters.
     */

    var upperTranslation: Float = 0f

    /**
     * Enable/disable the joint motor.
     */

    var enableMotor: Boolean = false

    /**
     * The maximum motor torque, usually in N-m.
     */

    var maxMotorForce: Float = 0f

    /**
     * The desired motor speed in radians per second.
     */

    var motorSpeed: Float = 0f

    /**
     * Initialize the bodies, anchors, axis, and reference angle using the world anchor and world
     * axis.
     */
    fun initialize(b1: Body, b2: Body, anchor: Vec2, axis: Vec2) {
        bodyA = b1
        bodyB = b2
        bodyA!!.getLocalPointToOut(anchor, localAnchorA)
        bodyB!!.getLocalPointToOut(anchor, localAnchorB)
        bodyA!!.getLocalVectorToOut(axis, localAxisA)
        referenceAngleRadians = bodyB!!.angleRadians - bodyA!!.angleRadians
    }
}
