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
package org.jbox2d.pooling

import org.jbox2d.collision.AABB
import org.jbox2d.collision.Collision
import org.jbox2d.collision.Distance
import org.jbox2d.collision.TimeOfImpact
import org.jbox2d.common.Mat22
import org.jbox2d.common.Mat33
import org.jbox2d.common.Rot
import org.jbox2d.common.Vec2
import org.jbox2d.common.Vec3
import org.jbox2d.dynamics.contacts.Contact

/**
 * World pool interface
 * @author Daniel
 */
interface IWorldPool {

    val polyContactStack: IDynamicStack<Contact>

    val circleContactStack: IDynamicStack<Contact>

    val polyCircleContactStack: IDynamicStack<Contact>

    val edgeCircleContactStack: IDynamicStack<Contact>

    val edgePolyContactStack: IDynamicStack<Contact>

    val chainCircleContactStack: IDynamicStack<Contact>

    val chainPolyContactStack: IDynamicStack<Contact>

    val collision: Collision

    val timeOfImpact: TimeOfImpact

    val distance: Distance

    fun popVec2(): Vec2

    fun popVec2(num: Int): Array<Vec2>

    fun pushVec2(num: Int)

    fun popVec3(): Vec3

    fun popVec3(num: Int): Array<Vec3>

    fun pushVec3(num: Int)

    fun popMat22(): Mat22

    fun popMat22(num: Int): Array<Mat22>

    fun pushMat22(num: Int)

    fun popMat33(): Mat33

    fun pushMat33(num: Int)

    fun popAABB(): AABB

    fun popAABB(num: Int): Array<AABB>

    fun pushAABB(num: Int)

    fun popRot(): Rot

    fun pushRot(num: Int)

    fun getFloatArray(argLength: Int): FloatArray

    fun getIntArray(argLength: Int): IntArray

    fun getVec2Array(argLength: Int): Array<Vec2>
}
