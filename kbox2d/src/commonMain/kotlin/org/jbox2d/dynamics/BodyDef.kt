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
package org.jbox2d.dynamics

import com.soywiz.korma.geom.*
import org.jbox2d.common.*
import org.jbox2d.userdata.*

/**
 * A body definition holds all the data needed to construct a rigid body. You can safely re-use body
 * definitions. Shapes are added to a body after construction.
 *
 * @author daniel
 */
data class BodyDef(
    /**
     * The body type: static, kinematic, or dynamic. Note: if a dynamic body would have zero mass, the
     * mass is set to one.
     */
    var type: BodyType = BodyType.STATIC,

    /**
     * Use this to store application specific body data.
     */
    var userData: Any? = null,

    /**
     * The world position of the body. Avoid creating bodies at the origin since this can lead to many
     * overlapping shapes.
     */
    var position: Vec2 = Vec2(),

    /**
     * The world angle of the body in radians.
     */
    var angleRadians: Float = 0f,

    /**
     * The linear velocity of the body in world co-ordinates.
     */
    var linearVelocity: Vec2 = Vec2(),

    /**
     * The angular velocity of the body.
     */
    var angularVelocity: Float = 0f,

    /**
     * Linear damping is use to reduce the linear velocity. The damping parameter can be larger than
     * 1.0f but the damping effect becomes sensitive to the time step when the damping parameter is
     * large.
     */
    var linearDamping: Float = 0f,

    /**
     * Angular damping is use to reduce the angular velocity. The damping parameter can be larger than
     * 1.0f but the damping effect becomes sensitive to the time step when the damping parameter is
     * large.
     */
    var angularDamping: Float = 0f,

    /**
     * Set this flag to false if this body should never fall asleep. Note that this increases CPU
     * usage.
     */
    var allowSleep: Boolean = true,

    /**
     * Is this body initially sleeping?
     */
    var awake: Boolean = true,

    /**
     * Should this body be prevented from rotating? Useful for characters.
     */
    var fixedRotation: Boolean = false,

    /**
     * Is this a fast moving body that should be prevented from tunneling through other moving bodies?
     * Note that all bodies are prevented from tunneling through kinematic and static bodies. This
     * setting is only considered on dynamic bodies.
     *
     * @warning You should use this flag sparingly since it increases processing time.
     */
    var bullet: Boolean = false,

    /**
     * Does this body start out active?
     */
    var active: Boolean = true,

    /**
     * Experimental: scales the inertia tensor.
     */
    var gravityScale: Float = 1f
) : Box2dTypedUserData by Box2dTypedUserData.Mixin() {
    /**
     * The world angle of the body in degrees.
     */
    var angleDegrees: Float
        set(value) = run { angleRadians = value * MathUtils.DEG2RAD }
        get() = angleRadians * MathUtils.RAD2DEG

    /**
     * The world angle of the body.
     */
    var angle: Angle
        set(value) = run { angleRadians = value.radians.toFloat() }
        get() = angleRadians.radians
}
