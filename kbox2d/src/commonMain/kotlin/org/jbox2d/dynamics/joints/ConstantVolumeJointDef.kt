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

import org.jbox2d.dynamics.*
import kotlin.jvm.*

/**
 * Definition for a [ConstantVolumeJoint], which connects a group a bodies together so they
 * maintain a constant volume within them.
 */
class ConstantVolumeJointDef : JointDef(JointType.CONSTANT_VOLUME) {

    var frequencyHz: Float = 0f

    var dampingRatio: Float = 0f

    internal var bodies: ArrayList<Body> = ArrayList()
    internal var joints: ArrayList<DistanceJoint>? = null

    init {
        collideConnected = false
        frequencyHz = 0.0f
        dampingRatio = 0.0f
    }

    /**
     * Adds a body to the group
     *
     * @param argBody
     */
    fun addBody(argBody: Body) {
        bodies.add(argBody)
        if (bodies.size == 1) {
            bodyA = argBody
        }
        if (bodies.size == 2) {
            bodyB = argBody
        }
    }

    /**
     * Adds a body and the pre-made distance joint. Should only be used for deserialization.
     */
    fun addBodyAndJoint(argBody: Body, argJoint: DistanceJoint) {
        addBody(argBody)
        if (joints == null) {
            joints = ArrayList()
        }
        joints!!.add(argJoint)
    }
}
