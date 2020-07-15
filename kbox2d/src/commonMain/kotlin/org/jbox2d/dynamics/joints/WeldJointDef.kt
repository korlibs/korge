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
 * Created at 3:38:52 AM Jan 15, 2011
 */

/**
 * @author Daniel Murphy
 */
class WeldJointDef : JointDef(JointType.WELD) {
    /**
     * The local anchor point relative to body1's origin.
     */

    val localAnchorA: Vec2 = Vec2()

    /**
     * The local anchor point relative to body2's origin.
     */

    val localAnchorB: Vec2 = Vec2()

    /**
     * The body2 angle minus body1 angle in the reference state (radians).
     */
    var referenceAngleRadians: Float = 0f

    /**
     * The body2 angle minus body1 angle in the reference state (degrees).
     */
    var referenceAngleDegrees: Float
        set(value) = run { referenceAngleRadians = value * MathUtils.DEG2RAD }
        get() = referenceAngleRadians * MathUtils.RAD2DEG

    /**
     * The body2 angle minus body1 angle in the reference state.
     */
    var referenceAngle: Angle
        set(value) = run { referenceAngleRadians = value.radians.toFloat() }
        get() = referenceAngleRadians.radians

    /**
     * The mass-spring-damper frequency in Hertz. Rotation only. Disable softness with a value of 0.
     */

    var frequencyHz: Float = 0f

    /**
     * The damping ratio. 0 = no damping, 1 = critical damping.
     */

    var dampingRatio: Float = 0f

    /**
     * Initialize the bodies, anchors, and reference angle using a world anchor point.
     *
     * @param bA
     * @param bB
     * @param anchor
     */
    fun initialize(bA: Body, bB: Body, anchor: Vec2) {
        bodyA = bA
        bodyB = bB
        bodyA!!.getLocalPointToOut(anchor, localAnchorA)
        bodyB!!.getLocalPointToOut(anchor, localAnchorB)
        referenceAngleRadians = bodyB!!.angleRadians - bodyA!!.angleRadians
    }
}
