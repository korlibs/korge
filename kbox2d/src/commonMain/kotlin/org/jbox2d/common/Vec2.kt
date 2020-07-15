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

import org.jbox2d.internal.*
import kotlin.jvm.*
import kotlin.native.concurrent.ThreadLocal

/**
 * A 2D column vector
 */
data class Vec2(
     var x: Float = 0f,
     var y: Float = 0f
) {
    /** True if the vector represents a pair of valid, non-infinite floating point numbers.  */
    val isValid: Boolean get() = !x.isNaN() && !x.isInfinite() && !y.isNaN() && !y.isInfinite()

    constructor(toCopy: Vec2) : this(toCopy.x, toCopy.y) {}

    /** Zero out this vector.  */
    fun setZero() {
        x = 0.0f
        y = 0.0f
    }

    /** Set the vector component-wise.  */
    fun set(x: Float, y: Float): Vec2 = this.apply {
        this.x = x
        this.y = y
    }

    inline fun set(x: Number, y: Number): Vec2 = set(x.toFloat(), y.toFloat())

    /** Set this vector to another vector.  */
    fun set(v: Vec2): Vec2 = this.apply {
        this.x = v.x
        this.y = v.y
    }

    /** Return the sum of this vector and another; does not alter either one.  */
    fun add(v: Vec2): Vec2 = Vec2(x + v.x, y + v.y)


    /** Return the difference of this vector and another; does not alter either one.  */
    fun sub(v: Vec2): Vec2 = Vec2(x - v.x, y - v.y)

    /** Return this vector multiplied by a scalar; does not alter this vector.  */
    fun mul(a: Float): Vec2 = Vec2(x * a, y * a)

    /** Return the negation of this vector; does not alter this vector.  */
    fun negate(): Vec2 = Vec2(-x, -y)

    /** Flip the vector and return it - alters this vector.  */
    fun negateLocal(): Vec2 = this.apply {
        x = -x
        y = -y
    }

    /** Add another vector to this one and returns result - alters this vector.  */
    fun addLocal(v: Vec2): Vec2 = this.apply {
        x += v.x
        y += v.y
    }

    /** Adds values to this vector and returns result - alters this vector.  */
    fun addLocal(x: Float, y: Float): Vec2 = this.apply {
        this.x += x
        this.y += y
    }

    inline fun addLocal(x: Number, y: Number): Vec2 = addLocal(x.toFloat(), y.toFloat())

    /** Subtract another vector from this one and return result - alters this vector.  */
    fun subLocal(v: Vec2): Vec2 = this.apply {
        x -= v.x
        y -= v.y
    }

    /** Multiply this vector by a number and return result - alters this vector.  */
    fun mulLocal(a: Float): Vec2 = this.apply {
        x *= a
        y *= a
    }

    /** Get the skew vector such that dot(skew_vec, other) == cross(vec, other)  */
    fun skew(): Vec2 = Vec2(-y, x)

    /** Get the skew vector such that dot(skew_vec, other) == cross(vec, other)  */
    fun skew(out: Vec2) {
        out.x = -y
        out.y = x
    }

    /** Return the length of this vector.  */
    fun length(): Float = MathUtils.sqrt(x * x + y * y)

    /** Return the squared length of this vector.  */
    fun lengthSquared(): Float = x * x + y * y

    /** Normalize this vector and return the length before normalization. Alters this vector.  */
    fun normalize(): Float {
        val length = length()
        if (length < Settings.EPSILON) {
            return 0f
        }

        val invLength = 1.0f / length
        x *= invLength
        y *= invLength
        return length
    }

    /** Return a new vector that has positive components.  */
    fun abs(): Vec2 = Vec2(MathUtils.abs(x), MathUtils.abs(y))

    fun absLocal() {
        x = MathUtils.abs(x)
        y = MathUtils.abs(y)
    }

    // @Override // annotation omitted for GWT-compatibility
    /** Return a copy of this vector.  */
    fun clone(): Vec2 = Vec2(x, y)

    override fun toString(): String = "($x,$y)"

    companion object {
        @ThreadLocal
        internal val dummy = Vec2()

        fun abs(a: Vec2): Vec2 = Vec2(MathUtils.abs(a.x), MathUtils.abs(a.y))


        fun absToOut(a: Vec2, out: Vec2) {
            out.x = MathUtils.abs(a.x)
            out.y = MathUtils.abs(a.y)
        }


        fun dot(a: Vec2, b: Vec2): Float = a.x * b.x + a.y * b.y
        fun cross(a: Vec2, b: Vec2): Float = a.x * b.y - a.y * b.x
        fun cross(a: Vec2, s: Float): Vec2 = Vec2(s * a.y, -s * a.x)

        fun crossToOut(a: Vec2, s: Float, out: Vec2) {
            val tempy = -s * a.x
            out.x = s * a.y
            out.y = tempy
        }

        fun crossToOutUnsafe(a: Vec2, s: Float, out: Vec2) {
            assert(out !== a)
            out.x = s * a.y
            out.y = -s * a.x
        }

        fun cross(s: Float, a: Vec2): Vec2 = Vec2(-s * a.y, s * a.x)

        fun crossToOut(s: Float, a: Vec2, out: Vec2) {
            val tempY = s * a.x
            out.x = -s * a.y
            out.y = tempY
        }

        fun crossToOutUnsafe(s: Float, a: Vec2, out: Vec2) {
            assert(out !== a)
            out.x = -s * a.y
            out.y = s * a.x
        }

        fun negateToOut(a: Vec2, out: Vec2) {
            out.x = -a.x
            out.y = -a.y
        }

        fun min(a: Vec2, b: Vec2): Vec2 = Vec2(if (a.x < b.x) a.x else b.x, if (a.y < b.y) a.y else b.y)
        fun max(a: Vec2, b: Vec2): Vec2 = Vec2(if (a.x > b.x) a.x else b.x, if (a.y > b.y) a.y else b.y)
        fun minToOut(a: Vec2, b: Vec2, out: Vec2) {
            out.x = if (a.x < b.x) a.x else b.x
            out.y = if (a.y < b.y) a.y else b.y
        }
        fun maxToOut(a: Vec2, b: Vec2, out: Vec2) {
            out.x = if (a.x > b.x) a.x else b.x
            out.y = if (a.y > b.y) a.y else b.y
        }
    }
}
