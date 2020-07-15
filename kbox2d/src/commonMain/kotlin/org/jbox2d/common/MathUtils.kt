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

package org.jbox2d.common

import kotlin.math.*
import kotlin.random.*

/**
 * A few math methods that don't fit very well anywhere else.
 */
class MathUtils : PlatformMathUtils() {
    companion object {

        val PI = kotlin.math.PI.toFloat()

        val TWOPI = (kotlin.math.PI * 2).toFloat()

        val INV_PI = 1f / PI

        val HALF_PI = PI / 2

        val QUARTER_PI = PI / 4

        val THREE_HALVES_PI = TWOPI - HALF_PI

        /**
         * Degrees to radians conversion factor
         */

        val DEG2RAD = PI / 180

        /**
         * Radians to degrees conversion factor
         */

        val RAD2DEG = 180 / PI


        val sinLUT = FloatArray(Settings.SINCOS_LUT_LENGTH) {
            kotlin.math.sin((it * Settings.SINCOS_LUT_PRECISION).toDouble()).toFloat()
        }

        fun sin(x: Float): Float {
            return if (Settings.SINCOS_LUT_ENABLED) {
                sinLUT(x)
            } else {
                kotlin.math.sin(x.toDouble()).toFloat()
            }
        }


        fun sinLUT(x: Float): Float {
            var x = x
            x %= TWOPI

            if (x < 0) {
                x += TWOPI
            }

            if (Settings.SINCOS_LUT_LERP) {

                x /= Settings.SINCOS_LUT_PRECISION

                val index = x.toInt()

                if (index != 0) {
                    x %= index.toFloat()
                }

                // the next index is 0
                return if (index == Settings.SINCOS_LUT_LENGTH - 1) {
                    (1 - x) * sinLUT[index] + x * sinLUT[0]
                } else {
                    (1 - x) * sinLUT[index] + x * sinLUT[index + 1]
                }

            } else {
                return sinLUT[MathUtils.round(x / Settings.SINCOS_LUT_PRECISION) % Settings.SINCOS_LUT_LENGTH]
            }
        }


        fun cos(x: Float): Float {
            return if (Settings.SINCOS_LUT_ENABLED) {
                sinLUT(HALF_PI - x)
            } else {
                kotlin.math.cos(x.toDouble()).toFloat()
            }
        }


        fun abs(x: Float): Float {
            return if (Settings.FAST_ABS) {
                if (x > 0) x else -x
            } else {
                kotlin.math.abs(x)
            }
        }


        fun fastAbs(x: Float): Float {
            return if (x > 0) x else -x
        }


        fun abs(x: Int): Int {
            val y = x shr 31
            return (x xor y) - y
        }


        fun floor(x: Float): Int = if (Settings.FAST_FLOOR) fastFloor(x) else floor(x.toDouble()).toInt()


        fun fastFloor(x: Float): Int {
            val y = x.toInt()
            return if (x < y) y - 1 else y
        }


        fun ceil(x: Float): Int = if (Settings.FAST_CEIL) fastCeil(x) else ceil(x.toDouble()).toInt()


        fun fastCeil(x: Float): Int {
            val y = x.toInt()
            return if (x > y) y + 1 else y
        }


        fun round(x: Float): Int = if (Settings.FAST_ROUND) floor(x + .5f) else round(x.toDouble()).toInt()

        /**
         * Rounds up the value to the nearest higher power^2 value.
         *
         * @param x
         * @return power^2 value
         */

        fun ceilPowerOf2(x: Int): Int {
            var pow2 = 1
            while (pow2 < x) pow2 = pow2 shl 1
            return pow2
        }


        fun max(a: Float, b: Float): Float = if (a > b) a else b
        fun max(a: Int, b: Int): Int = if (a > b) a else b
        fun min(a: Float, b: Float): Float = if (a < b) a else b
        fun min(a: Int, b: Int): Int = if (a < b) a else b

        fun map(`val`: Float, fromMin: Float, fromMax: Float,
                toMin: Float, toMax: Float): Float {
            val mult = (`val` - fromMin) / (fromMax - fromMin)
            val res = toMin + mult * (toMax - toMin)
            return res
        }

        /** Returns the closest value to 'a' that is in between 'low' and 'high'  */

        fun clamp(a: Float, low: Float, high: Float): Float = max(low, min(a, high))


        fun clamp(a: Vec2, low: Vec2, high: Vec2): Vec2 {
            val min = Vec2()
            min.x = if (a.x < high.x) a.x else high.x
            min.y = if (a.y < high.y) a.y else high.y
            min.x = if (low.x > min.x) low.x else min.x
            min.y = if (low.y > min.y) low.y else min.y
            return min
        }


        fun clampToOut(a: Vec2, low: Vec2, high: Vec2, dest: Vec2) {
            dest.x = if (a.x < high.x) a.x else high.x
            dest.y = if (a.y < high.y) a.y else high.y
            dest.x = if (low.x > dest.x) low.x else dest.x
            dest.y = if (low.y > dest.y) low.y else dest.y
        }

        /**
         * Next Largest Power of 2: Given a binary integer value x, the next largest power of 2 can be
         * computed by a SWAR algorithm that recursively "folds" the upper bits into the lower bits. This
         * process yields a bit vector with the same most significant 1 as x, but all 1's below it. Adding
         * 1 to that value yields the next largest power of 2.
         */

        fun nextPowerOfTwo(x: Int): Int {
            var x = x
            x = x or (x shr 1)
            x = x or (x shr 2)
            x = x or (x shr 4)
            x = x or (x shr 8)
            x = x or (x shr 16)
            return x + 1
        }


        fun isPowerOfTwo(x: Int): Boolean {
            return x > 0 && x and x - 1 == 0
        }


        fun pow(a: Float, b: Float): Float {
            return if (Settings.FAST_POW) {
                PlatformMathUtils.fastPow(a, b)
            } else {
                a.toDouble().pow(b.toDouble()).toFloat()
            }
        }


        fun atan2(y: Float, x: Float): Float {
            return if (Settings.FAST_ATAN2) {
                fastAtan2(y, x)
            } else {
                kotlin.math.atan2(y.toDouble(), x.toDouble()).toFloat()
            }
        }


        fun fastAtan2(y: Float, x: Float): Float {
            if (x == 0.0f) {
                if (y > 0.0f) return HALF_PI
                return if (y == 0.0f) 0.0f else -HALF_PI
            }
            val atan: Float
            val z = y / x
            if (abs(z) < 1.0f) {
                atan = z / (1.0f + 0.28f * z * z)
                if (x < 0.0f) {
                    return if (y < 0.0f) atan - PI else atan + PI
                }
            } else {
                atan = HALF_PI - z / (z * z + 0.28f)
                if (y < 0.0f) return atan - PI
            }
            return atan
        }


        fun reduceAngle(theta: Float): Float {
            var theta = theta
            theta %= TWOPI
            if (abs(theta) > PI) {
                theta = theta - TWOPI
            }
            if (abs(theta) > HALF_PI) {
                theta = PI - theta
            }
            return theta
        }


        fun randomFloat(argLow: Float, argHigh: Float): Float {

            return kotlin.random.Random.nextFloat() * (argHigh - argLow) + argLow
        }


        fun randomFloat(r: Random, argLow: Float, argHigh: Float): Float {
            return r.nextFloat() * (argHigh - argLow) + argLow
        }


        fun sqrt(x: Float): Float {
            return kotlin.math.sqrt(x.toDouble()).toFloat()
        }


        fun distanceSquared(v1: Vec2, v2: Vec2): Float {
            val dx = v1.x - v2.x
            val dy = v1.y - v2.y
            return dx * dx + dy * dy
        }


        fun distance(v1: Vec2, v2: Vec2): Float {
            return sqrt(distanceSquared(v1, v2))
        }
    }
}
