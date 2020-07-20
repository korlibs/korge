/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.badlogic.gdx.math

import com.badlogic.gdx.math.MathUtils.cos
import com.badlogic.gdx.math.MathUtils.sin
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Interpolation.Elastic
import com.badlogic.gdx.math.Interpolation.BounceOut
import com.badlogic.gdx.math.Interpolation.PowIn
import com.badlogic.gdx.math.Interpolation.PowOut
import com.badlogic.gdx.math.Interpolation.ExpIn
import com.badlogic.gdx.math.Interpolation.ExpOut
import com.badlogic.gdx.math.Interpolation.ElasticIn
import com.badlogic.gdx.math.Interpolation.ElasticOut
import com.badlogic.gdx.math.Interpolation.Swing
import com.badlogic.gdx.math.Interpolation.SwingIn
import com.badlogic.gdx.math.Interpolation.SwingOut
import com.badlogic.gdx.math.Interpolation.Bounce
import com.badlogic.gdx.math.Interpolation.BounceIn

/** Takes a linear value in the range of 0-1 and outputs a (usually) non-linear, interpolated value.
 * @author Nathan Sweet
 */
abstract class Interpolation {
    /** @param a Alpha value between 0 and 1.
     */
    abstract fun apply(a: Float): Float

    /** @param a Alpha value between 0 and 1.
     */
    fun apply(start: Float, end: Float, a: Float): Float {
        return start + (end - start) * apply(a)
    }

    //
    open class Pow(val power: Int) : Interpolation() {
        override fun apply(a: Float): Float {
            return if (a <= 0.5f) Math.pow(a * 2.toDouble(), power.toDouble()).toFloat() / 2 else Math.pow((a - 1) * 2.toDouble(), power.toDouble()).toFloat() / (if (power % 2 == 0) -2 else 2) + 1
        }
    }

    class PowIn(power: Int) : Pow(power) {
        override fun apply(a: Float): Float {
            return Math.pow(a.toDouble(), power.toDouble()).toFloat()
        }
    }

    class PowOut(power: Int) : Pow(power) {
        override fun apply(a: Float): Float {
            return Math.pow(a - 1.toDouble(), power.toDouble()).toFloat() * (if (power % 2 == 0) -1 else 1) + 1
        }
    }

    //
    open class Exp(val value: Float, val power: Float) : Interpolation() {
        val min: Float
        val scale: Float
        override fun apply(a: Float): Float {
            return if (a <= 0.5f) (Math.pow(value.toDouble(), power * (a * 2 - 1).toDouble()).toFloat() - min) * scale / 2 else (2 - (Math.pow(value.toDouble(), -power * (a * 2 - 1).toDouble()).toFloat() - min) * scale) / 2
        }

        init {
            min = Math.pow(value.toDouble(), -power.toDouble()).toFloat()
            scale = 1 / (1 - min)
        }
    }

    class ExpIn(value: Float, power: Float) : Exp(value, power) {
        override fun apply(a: Float): Float {
            return (Math.pow(value.toDouble(), power * (a - 1).toDouble()).toFloat() - min) * scale
        }
    }

    class ExpOut(value: Float, power: Float) : Exp(value, power) {
        override fun apply(a: Float): Float {
            return 1 - (Math.pow(value.toDouble(), -power * a.toDouble()).toFloat() - min) * scale
        }
    }

    //
    open class Elastic(val value: Float, val power: Float, bounces: Int, val scale: Float) : Interpolation() {
        val bounces: Float
        override fun apply(a: Float): Float {
            var a = a
            if (a <= 0.5f) {
                a *= 2f
                return Math.pow(value.toDouble(), power * (a - 1).toDouble()).toFloat() * sin(a * bounces) * scale / 2
            }
            a = 1 - a
            a *= 2f
            return 1 - Math.pow(value.toDouble(), power * (a - 1).toDouble()).toFloat() * sin(a * bounces) * scale / 2
        }

        init {
            this.bounces = bounces * MathUtils.PI * if (bounces % 2 == 0) 1 else -1
        }
    }

    class ElasticIn(value: Float, power: Float, bounces: Int, scale: Float) : Elastic(value, power, bounces, scale) {
        override fun apply(a: Float): Float {
            return if (a >= 0.99) 1f else Math.pow(value.toDouble(), power * (a - 1).toDouble()).toFloat() * sin(a * bounces) * scale
        }
    }

    class ElasticOut(value: Float, power: Float, bounces: Int, scale: Float) : Elastic(value, power, bounces, scale) {
        override fun apply(a: Float): Float {
            var a = a
            if (a == 0f) return 0f
            a = 1 - a
            return 1 - Math.pow(value.toDouble(), power * (a - 1).toDouble()).toFloat() * sin(a * bounces) * scale
        }
    }

    //
    class Bounce : BounceOut {
        constructor(widths: FloatArray, heights: FloatArray) : super(widths, heights) {}
        constructor(bounces: Int) : super(bounces) {}

        private fun out(a: Float): Float {
            val test = a + widths[0] / 2
            return if (test < widths[0]) test / (widths[0] / 2) - 1 else super.apply(a)
        }

        override fun apply(a: Float): Float {
            return if (a <= 0.5f) (1 - out(1 - a * 2)) / 2 else out(a * 2 - 1) / 2 + 0.5f
        }
    }

    open class BounceOut : Interpolation {
        val widths: FloatArray
        val heights: FloatArray

        constructor(widths: FloatArray, heights: FloatArray) {
            require(widths.size == heights.size) { "Must be the same number of widths and heights." }
            this.widths = widths
            this.heights = heights
        }

        constructor(bounces: Int) {
            require(!(bounces < 2 || bounces > 5)) { "bounces cannot be < 2 or > 5: $bounces" }
            widths = FloatArray(bounces)
            heights = FloatArray(bounces)
            heights[0] = 1f
            when (bounces) {
                2 -> {
                    widths[0] = 0.6f
                    widths[1] = 0.4f
                    heights[1] = 0.33f
                }
                3 -> {
                    widths[0] = 0.4f
                    widths[1] = 0.4f
                    widths[2] = 0.2f
                    heights[1] = 0.33f
                    heights[2] = 0.1f
                }
                4 -> {
                    widths[0] = 0.34f
                    widths[1] = 0.34f
                    widths[2] = 0.2f
                    widths[3] = 0.15f
                    heights[1] = 0.26f
                    heights[2] = 0.11f
                    heights[3] = 0.03f
                }
                5 -> {
                    widths[0] = 0.3f
                    widths[1] = 0.3f
                    widths[2] = 0.2f
                    widths[3] = 0.1f
                    widths[4] = 0.1f
                    heights[1] = 0.45f
                    heights[2] = 0.3f
                    heights[3] = 0.15f
                    heights[4] = 0.06f
                }
            }
            widths[0] *= 2f
        }

        override fun apply(a: Float): Float {
            var a = a
            if (a == 1f) return 1f
            a += widths[0] / 2
            var width = 0f
            var height = 0f
            var i = 0
            val n = widths.size
            while (i < n) {
                width = widths[i]
                if (a <= width) {
                    height = heights[i]
                    break
                }
                a -= width
                i++
            }
            a /= width
            val z = 4 / width * height * a
            return 1 - (z - z * a) * width
        }
    }

    class BounceIn : BounceOut {
        constructor(widths: FloatArray, heights: FloatArray) : super(widths, heights) {}
        constructor(bounces: Int) : super(bounces) {}

        override fun apply(a: Float): Float {
            return 1 - super.apply(1 - a)
        }
    }

    //
    class Swing(scale: Float) : Interpolation() {
        private val scale: Float
        override fun apply(a: Float): Float {
            var a = a
            if (a <= 0.5f) {
                a *= 2f
                return a * a * ((scale + 1) * a - scale) / 2
            }
            a--
            a *= 2f
            return a * a * ((scale + 1) * a + scale) / 2 + 1
        }

        init {
            this.scale = scale * 2
        }
    }

    class SwingOut(private val scale: Float) : Interpolation() {
        override fun apply(a: Float): Float {
            var a = a
            a--
            return a * a * ((scale + 1) * a + scale) + 1
        }
    }

    class SwingIn(private val scale: Float) : Interpolation() {
        override fun apply(a: Float): Float {
            return a * a * ((scale + 1) * a - scale)
        }
    }

    companion object {
        //
        @JvmField
        val linear: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                return a
            }
        }
        //
        /** Aka "smoothstep".  */
        @JvmField
        val smooth: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                return a * a * (3 - 2 * a)
            }
        }
        @JvmField
        val smooth2: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                var a = a
                a = a * a * (3 - 2 * a)
                return a * a * (3 - 2 * a)
            }
        }

        /** By Ken Perlin.  */
        @JvmField
        val smoother: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                return a * a * a * (a * (a * 6 - 15) + 10)
            }
        }
        @JvmField
        val fade = smoother

        //
        @JvmField
        val pow2 = Pow(2)

        /** Slow, then fast.  */
        @JvmField
        val pow2In = PowIn(2)
        @JvmField
        val slowFast = pow2In

        /** Fast, then slow.  */
        @JvmField
        val pow2Out = PowOut(2)
        @JvmField
        val fastSlow = pow2Out
        @JvmField
        val pow2InInverse: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                return Math.sqrt(a.toDouble()).toFloat()
            }
        }
        @JvmField
        val pow2OutInverse: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                return 1 - Math.sqrt(-(a - 1).toDouble()).toFloat()
            }
        }
        @JvmField
        val pow3 = Pow(3)
        @JvmField
        val pow3In = PowIn(3)
        @JvmField
        val pow3Out = PowOut(3)
        @JvmField
        val pow3InInverse: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                return Math.cbrt(a.toDouble()).toFloat()
            }
        }
        @JvmField
        val pow3OutInverse: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                return 1 - Math.cbrt(-(a - 1).toDouble()).toFloat()
            }
        }
        @JvmField
        val pow4 = Pow(4)
        @JvmField
        val pow4In = PowIn(4)
        @JvmField
        val pow4Out = PowOut(4)
        @JvmField
        val pow5 = Pow(5)
        @JvmField
        val pow5In = PowIn(5)
        @JvmField
        val pow5Out = PowOut(5)
        @JvmField
        val sine: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                return (1 - cos(a * MathUtils.PI)) / 2
            }
        }
        @JvmField
        val sineIn: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                return 1 - cos(a * MathUtils.PI / 2)
            }
        }
        @JvmField
        val sineOut: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                return sin(a * MathUtils.PI / 2)
            }
        }
        @JvmField
        val exp10 = Exp(2f, 10f)
        @JvmField
        val exp10In = ExpIn(2f, 10f)
        @JvmField
        val exp10Out = ExpOut(2f, 10f)
        @JvmField
        val exp5 = Exp(2f, 5f)
        @JvmField
        val exp5In = ExpIn(2f, 5f)
        @JvmField
        val exp5Out = ExpOut(2f, 5f)
        @JvmField
        val circle: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                var a = a
                if (a <= 0.5f) {
                    a *= 2f
                    return (1 - Math.sqrt(1 - a * a.toDouble()).toFloat()) / 2
                }
                a--
                a *= 2f
                return (Math.sqrt(1 - a * a.toDouble()).toFloat() + 1) / 2
            }
        }
        @JvmField
        val circleIn: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                return 1 - Math.sqrt(1 - a * a.toDouble()).toFloat()
            }
        }
        @JvmField
        val circleOut: Interpolation = object : Interpolation() {
            override fun apply(a: Float): Float {
                var a = a
                a--
                return Math.sqrt(1 - a * a.toDouble()).toFloat()
            }
        }
        @JvmField
        val elastic = Elastic(2f, 10f, 7, 1f)
        @JvmField
        val elasticIn = ElasticIn(2f, 10f, 6, 1f)
        @JvmField
        val elasticOut = ElasticOut(2f, 10f, 7, 1f)
        @JvmField
        val swing = Swing(1.5f)
        @JvmField
        val swingIn = SwingIn(2f)
        @JvmField
        val swingOut = SwingOut(2f)
        @JvmField
        val bounce = Bounce(4)
        @JvmField
        val bounceIn = BounceIn(4)
        @JvmField
        val bounceOut = BounceOut(4)
    }
}
