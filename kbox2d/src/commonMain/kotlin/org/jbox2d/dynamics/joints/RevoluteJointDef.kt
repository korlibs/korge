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
/*
 * JBox2D - A Java Port of Erin Catto's Box2D
 *
 * JBox2D homepage: http://jbox2d.sourceforge.net/
 * Box2D homepage: http://www.box2d.org
 *
 * This software is provided 'as-is', without any express or implied
 * warranty.  In no event will the authors be held liable for any damages
 * arising from the use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 * 1. The origin of this software must not be misrepresented; you must not
 * claim that you wrote the original software. If you use this software
 * in a product, an acknowledgment in the product documentation would be
 * appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 * misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package org.jbox2d.dynamics.joints

import com.soywiz.korma.geom.*
import org.jbox2d.common.*
import org.jbox2d.dynamics.Body

/**
 * Revolute joint definition. This requires defining an anchor point where the bodies are joined.
 * The definition uses local anchor points so that the initial configuration can violate the
 * constraint slightly. You also need to specify the initial relative angle for joint limits. This
 * helps when saving and loading a game. The local anchor points are measured from the body's origin
 * rather than the center of mass because:<br></br>
 *
 *  * you might not know where the center of mass will be.
 *  * if you add/remove shapes from a body and recompute the mass, the joints will be broken.
 *
 */
class RevoluteJointDef : JointDef(JointType.REVOLUTE) {

    /**
     * The local anchor point relative to body1's origin.
     */

    var localAnchorA: Vec2 = Vec2(0.0f, 0.0f)

    /**
     * The local anchor point relative to body2's origin.
     */

    var localAnchorB: Vec2 = Vec2(0.0f, 0.0f)

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
     * A flag to enable joint limits.
     */

    var enableLimit: Boolean = false

    /** The lower angle for the joint limit (radians). */
    var lowerAngleRadians: Float = 0f

    /** The lower angle for the joint limit (degrees). */
    var lowerAngleDegrees: Float
        set(value) = run { lowerAngleRadians = value * MathUtils.DEG2RAD }
        get() = lowerAngleRadians * MathUtils.RAD2DEG

    /** The lower angle for the joint limit. */
    var lowerAngle: Angle
        set(value) = run { lowerAngleRadians = value.radians.toFloat() }
        get() = lowerAngleRadians.radians

    /** The upper angle for the joint limit (radians). */
    var upperAngleRadians: Float = 0f

    /** The upper angle for the joint limit (degrees). */
    var upperAngleDegrees: Float
        set(value) = run { upperAngleRadians = value * MathUtils.DEG2RAD }
        get() = upperAngleRadians * MathUtils.RAD2DEG

    /** The lower angle for the joint limit. */
    var upperAngle: Angle
        set(value) = run { upperAngleRadians = value.radians.toFloat() }
        get() = upperAngleRadians.radians

    /**
     * A flag to enable the joint motor.
     */

    var enableMotor: Boolean = false

    /**
     * The desired motor speed. Usually in radians per second.
     */

    var motorSpeed: Float = 0f

    /**
     * The maximum motor torque used to achieve the desired motor speed. Usually in N-m.
     */

    var maxMotorTorque: Float = 0f

    /**
     * Initialize the bodies, anchors, and reference angle using the world anchor.
     *
     * @param b1
     * @param b2
     * @param anchor
     */
    fun initialize(b1: Body, b2: Body, anchor: Vec2) {
        bodyA = b1
        bodyB = b2
        bodyA!!.getLocalPointToOut(anchor, localAnchorA)
        bodyB!!.getLocalPointToOut(anchor, localAnchorB)
        referenceAngleRadians = bodyB!!.angleRadians - bodyA!!.angleRadians
    }
}
