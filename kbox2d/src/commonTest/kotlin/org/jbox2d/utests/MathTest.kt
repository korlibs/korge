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
 * Created at 4:32:38 AM Jan 14, 2011
 */
package org.jbox2d.utests

import org.jbox2d.common.*
import kotlin.math.*
import kotlin.random.*
import kotlin.test.*

/**
 * @author Daniel Murphy
 */
class MathTest {
    @Test
    fun testFastMath() {
        val r = Random(0)
        for (i in 0 until RAND_ITERS) {
            val a = r.nextFloat() * MAX - MAX / 2
            assertEquals(floor(a.toDouble()).toInt(), MathUtils.floor(a))
        }

        for (i in 0 until RAND_ITERS) {
            val a = r.nextFloat() * MAX - MAX / 2
            assertEquals(ceil(a.toDouble()).toInt(), MathUtils.ceil(a))
        }

        for (i in 0 until RAND_ITERS) {
            val a = r.nextFloat() * MAX - MAX / 2
            val b = r.nextFloat() * MAX - MAX / 2
            assertEquals(max(a, b), MathUtils.max(a, b))
        }

        for (i in 0 until RAND_ITERS) {
            val a = r.nextFloat() * MAX - MAX / 2
            val b = r.nextFloat() * MAX - MAX / 2
            assertEquals(min(a, b), MathUtils.min(a, b))
        }

        for (i in 0 until RAND_ITERS) {
            val a = r.nextFloat() * MAX - MAX / 2
            assertEquals(round(a).toInt(), MathUtils.round(a)) // Failed on watchOS X86
        }

        for (i in 0 until RAND_ITERS) {
            val a = r.nextFloat() * MAX - MAX / 2
            assertEquals(abs(a), MathUtils.abs(a))
        }
    }

    fun testVec2() {
        val v = Vec2()
        v.x = 0f
        v.y = 1f
        v.subLocal(Vec2(10f, 10f))
        assertEquals(-10f, v.x)
        assertEquals(-9f, v.y)

        val v2 = v.add(Vec2(1f, 1f))
        assertEquals(-9f, v2.x)
        assertEquals(-8f, v2.y)
        assertFalse(v == v2)
    }

    fun testMat22Unsafes() {
        val v1 = Vec2(10f, -1.3f)
        val m1 = Mat22(1f, 34f, -3f, 3f)
        val m2 = Mat22(2f, -1f, 4.1f, -4f)
        val vo = Vec2()
        val mo = Mat22()

        Mat22.mulToOutUnsafe(m1, m2, mo)
        assertEquals(Mat22.mul(m1, m2), mo)

        Mat22.mulToOutUnsafe(m1, v1, vo)
        assertEquals(Mat22.mul(m1, v1), vo)

        Mat22.mulTransToOutUnsafe(m1, m2, mo)
        assertEquals(Mat22.mulTrans(m1, m2), mo)

        Mat22.mulTransToOutUnsafe(m1, v1, vo)
        assertEquals(Mat22.mulTrans(m1, v1), vo)
    }

    fun testMat33() {
        val mat = Mat33()

        mat.ex.set(3f, 19f, -5f)
        mat.ey.set(-1f, 1f, 4f)
        mat.ez.set(-10f, 4f, 4f)

        val b = Vec3(4f, 1f, 2f)
        assertEquals(Vec3(0.096f, 1.1013334f, -.48133332f), mat.solve33(b))

        val b2 = Vec2(4f, 1f)
        assertEquals(Vec2(0.22727273f, -3.318182f), mat.solve22(b2))
    }

    fun testVec3() {
        val v1 = Vec3()
        val v2 = Vec3()

        assertEquals(Vec3(1f, -15f, 36f), Vec3.cross(v1.set(9f, 3f, 1f), v2.set(3f, 5f, 2f)))
    }

    companion object {

        private val MAX = (Float.MAX_VALUE / 1000).toInt()
        private val RAND_ITERS = 100
    }
}
