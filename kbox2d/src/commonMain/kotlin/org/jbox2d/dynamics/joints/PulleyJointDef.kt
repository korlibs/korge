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
/**
 * Created at 12:11:41 PM Jan 23, 2011
 */
package org.jbox2d.dynamics.joints

import org.jbox2d.common.Settings
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.internal.*

/**
 * Pulley joint definition. This requires two ground anchors, two dynamic body anchor points, and a
 * pulley ratio.
 *
 * @author Daniel Murphy
 */
class PulleyJointDef : JointDef(JointType.PULLEY) {

    /**
     * The first ground anchor in world coordinates. This point never moves.
     */

    var groundAnchorA: Vec2 = Vec2(-1.0f, 1.0f)

    /**
     * The second ground anchor in world coordinates. This point never moves.
     */

    var groundAnchorB: Vec2 = Vec2(1.0f, 1.0f)

    /**
     * The local anchor point relative to bodyA's origin.
     */

    var localAnchorA: Vec2 = Vec2(-1.0f, 0.0f)

    /**
     * The local anchor point relative to bodyB's origin.
     */

    var localAnchorB: Vec2 = Vec2(1.0f, 0.0f)

    /**
     * The a reference length for the segment attached to bodyA.
     */

    var lengthA: Float = 0f

    /**
     * The a reference length for the segment attached to bodyB.
     */

    var lengthB: Float = 0f

    /**
     * The pulley ratio, used to simulate a block-and-tackle.
     */

    var ratio: Float = 1f

    init {
        collideConnected = true
    }

    /**
     * Initialize the bodies, anchors, lengths, max lengths, and ratio using the world anchors.
     */
    fun initialize(b1: Body, b2: Body, ga1: Vec2, ga2: Vec2, anchor1: Vec2, anchor2: Vec2, r: Float) {
        bodyA = b1
        bodyB = b2
        groundAnchorA = ga1
        groundAnchorB = ga2
        localAnchorA = bodyA!!.getLocalPoint(anchor1)
        localAnchorB = bodyB!!.getLocalPoint(anchor2)
        val d1 = anchor1.sub(ga1)
        lengthA = d1.length()
        val d2 = anchor2.sub(ga2)
        lengthB = d2.length()
        ratio = r
        assert(ratio > Settings.EPSILON)
    }
}
