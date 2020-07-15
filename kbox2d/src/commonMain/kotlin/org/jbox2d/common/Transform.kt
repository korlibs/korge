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
package org.jbox2d.common

import com.soywiz.korma.geom.*
import org.jbox2d.internal.*

// updated to rev 100

/**
 * A transform contains translation and rotation. It is used to represent the position and
 * orientation of rigid frames.
 */
class Transform {

    /** The translation caused by the transform  */

    val p: Vec2

    /** A matrix representing a rotation  */

    val q: Rot

    /** The default constructor.  */
    constructor() {
        p = Vec2()
        q = Rot()
    }

    /** Initialize as a copy of another transform.  */
    constructor(xf: Transform) {
        p = xf.p.clone()
        q = xf.q.clone()
    }

    /** Initialize using a position vector and a rotation matrix.  */
    constructor(_position: Vec2, _R: Rot) {
        p = _position.clone()
        q = _R.clone()
    }

    /** Set this to equal another transform.  */
    fun set(xf: Transform): Transform {
        p.set(xf.p)
        q.set(xf.q)
        return this
    }

    /**
     * Set this based on the position and angle.
     *
     * @param p
     * @param angle
     */
    fun set(p: Vec2, angle: Angle) = this.setRadians(p, angle.radians.toFloat())

    /**
     * Set this based on the position and angle in radians.
     *
     * @param p
     * @param angleRadians
     */
    fun setRadians(p: Vec2, angleRadians: Float) {
        this.p.set(p)
        q.setRadians(angleRadians)
    }

    /**
     * Set this based on the position and angle in degrees.
     *
     * @param p
     * @param angle
     */
    fun setDegrees(p: Vec2, angleDegrees: Float) = setRadians(p, angleDegrees * MathUtils.DEG2RAD)

    /** Set this to the identity transform.  */
    fun setIdentity() {
        p.setZero()
        q.setIdentity()
    }

    override fun toString(): String {
        var s = "XForm:\n"
        s += "Position: $p\n"
        s += "R: \n$q\n"
        return s
    }

    companion object {

        fun mul(T: Transform, v: Vec2): Vec2 {
            return Vec2(T.q.c * v.x - T.q.s * v.y + T.p.x, T.q.s * v.x + T.q.c * v.y + T.p.y)
        }


        fun mulToOut(T: Transform, v: Vec2, out: Vec2) {
            val tempy = T.q.s * v.x + T.q.c * v.y + T.p.y
            out.x = T.q.c * v.x - T.q.s * v.y + T.p.x
            out.y = tempy
        }


        fun mulToOutUnsafe(T: Transform, v: Vec2, out: Vec2) {
            assert(v !== out)
            out.x = T.q.c * v.x - T.q.s * v.y + T.p.x
            out.y = T.q.s * v.x + T.q.c * v.y + T.p.y
        }


        fun mulTrans(T: Transform, v: Vec2): Vec2 {
            val px = v.x - T.p.x
            val py = v.y - T.p.y
            return Vec2(T.q.c * px + T.q.s * py, -T.q.s * px + T.q.c * py)
        }


        fun mulTransToOut(T: Transform, v: Vec2, out: Vec2) {
            val px = v.x - T.p.x
            val py = v.y - T.p.y
            val tempy = -T.q.s * px + T.q.c * py
            out.x = T.q.c * px + T.q.s * py
            out.y = tempy
        }


        fun mulTransToOutUnsafe(T: Transform, v: Vec2, out: Vec2) {
            assert(v !== out)
            val px = v.x - T.p.x
            val py = v.y - T.p.y
            out.x = T.q.c * px + T.q.s * py
            out.y = -T.q.s * px + T.q.c * py
        }


        fun mul(A: Transform, B: Transform): Transform {
            val C = Transform()
            Rot.mulUnsafe(A.q, B.q, C.q)
            Rot.mulToOutUnsafe(A.q, B.p, C.p)
            C.p.addLocal(A.p)
            return C
        }


        fun mulToOut(A: Transform, B: Transform, out: Transform) {
            assert(out !== A)
            Rot.mul(A.q, B.q, out.q)
            Rot.mulToOut(A.q, B.p, out.p)
            out.p.addLocal(A.p)
        }


        fun mulToOutUnsafe(A: Transform, B: Transform, out: Transform) {
            assert(out !== B)
            assert(out !== A)
            Rot.mulUnsafe(A.q, B.q, out.q)
            Rot.mulToOutUnsafe(A.q, B.p, out.p)
            out.p.addLocal(A.p)
        }

        fun mulTrans(A: Transform, B: Transform, pool: Vec2 = Vec2()): Transform {
            val C = Transform()
            Rot.mulTransUnsafe(A.q, B.q, C.q)
            pool.set(B.p).subLocal(A.p)
            Rot.mulTransUnsafe(A.q, pool, C.p)
            return C
        }


        fun mulTransToOut(A: Transform, B: Transform, out: Transform, pool: Vec2 = Vec2()) {
            assert(out !== A)
            Rot.mulTrans(A.q, B.q, out.q)
            pool.set(B.p).subLocal(A.p)
            Rot.mulTrans(A.q, pool, out.p)
        }


        fun mulTransToOutUnsafe(A: Transform, B: Transform, out: Transform, pool: Vec2 = Vec2()) {
            assert(out !== A)
            assert(out !== B)
            Rot.mulTransUnsafe(A.q, B.q, out.q)
            pool.set(B.p).subLocal(A.p)
            Rot.mulTransUnsafe(A.q, pool, out.p)
        }
    }
}
