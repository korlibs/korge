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

/**
 * Represents a rotation
 *
 * @author Daniel
 */
class Rot {


    var s: Float = 0.toFloat()

    var c: Float = 0.toFloat() // sin and cos

    val angleRadians: Float get() = MathUtils.atan2(s, c)

    val angleDegrees: Float get() = angleRadians * MathUtils.RAD2DEG

    val angle: Angle get() = angleRadians.radians

    constructor() {
        setIdentity()
    }

    @Deprecated("", replaceWith = ReplaceWith("Rot(angleRadians.radians)"))
    constructor(angleRadians: Float) {
        setRadians(angleRadians)
    }

    constructor(angle: Angle) {
        set(angle)
    }

    override fun toString(): String {
        return "Rot(s:$s, c:$c)"
    }

    fun setRadians(angleRadians: Float): Rot {
        s = MathUtils.sin(angleRadians)
        c = MathUtils.cos(angleRadians)
        return this
    }

    fun set(angle: Angle): Rot = setRadians(angle.radians.toFloat())

    fun setDegrees(angleDegrees: Float): Rot = setRadians(angleDegrees * MathUtils.DEG2RAD)

    fun set(other: Rot): Rot {
        s = other.s
        c = other.c
        return this
    }

    fun setIdentity(): Rot {
        s = 0f
        c = 1f
        return this
    }

    fun getXAxis(xAxis: Vec2) {
        xAxis.set(c, s)
    }

    fun getYAxis(yAxis: Vec2) {
        yAxis.set(-s, c)
    }

    // @Override // annotation omitted for GWT-compatibility
    fun clone(): Rot {
        val copy = Rot()
        copy.s = s
        copy.c = c
        return copy
    }

    companion object {

        fun mul(q: Rot, r: Rot, out: Rot) {
            val tempc = q.c * r.c - q.s * r.s
            out.s = q.s * r.c + q.c * r.s
            out.c = tempc
        }


        fun mulUnsafe(q: Rot, r: Rot, out: Rot) {
            assert(r !== out)
            assert(q !== out)
            // [qc -qs] * [rc -rs] = [qc*rc-qs*rs -qc*rs-qs*rc]
            // [qs qc] [rs rc] [qs*rc+qc*rs -qs*rs+qc*rc]
            // s = qs * rc + qc * rs
            // c = qc * rc - qs * rs
            out.s = q.s * r.c + q.c * r.s
            out.c = q.c * r.c - q.s * r.s
        }


        fun mulTrans(q: Rot, r: Rot, out: Rot) {
            val tempc = q.c * r.c + q.s * r.s
            out.s = q.c * r.s - q.s * r.c
            out.c = tempc
        }


        fun mulTransUnsafe(q: Rot, r: Rot, out: Rot) {
            // [ qc qs] * [rc -rs] = [qc*rc+qs*rs -qc*rs+qs*rc]
            // [-qs qc] [rs rc] [-qs*rc+qc*rs qs*rs+qc*rc]
            // s = qc * rs - qs * rc
            // c = qc * rc + qs * rs
            out.s = q.c * r.s - q.s * r.c
            out.c = q.c * r.c + q.s * r.s
        }


        fun mulToOut(q: Rot, v: Vec2, out: Vec2) {
            val tempy = q.s * v.x + q.c * v.y
            out.x = q.c * v.x - q.s * v.y
            out.y = tempy
        }


        fun mulToOutUnsafe(q: Rot, v: Vec2, out: Vec2) {
            out.x = q.c * v.x - q.s * v.y
            out.y = q.s * v.x + q.c * v.y
        }


        fun mulTrans(q: Rot, v: Vec2, out: Vec2) {
            val tempy = -q.s * v.x + q.c * v.y
            out.x = q.c * v.x + q.s * v.y
            out.y = tempy
        }


        fun mulTransUnsafe(q: Rot, v: Vec2, out: Vec2) {
            out.x = q.c * v.x + q.s * v.y
            out.y = -q.s * v.x + q.c * v.y
        }
    }
}
