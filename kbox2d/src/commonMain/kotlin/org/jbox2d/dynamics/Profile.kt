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

import org.jbox2d.common.MathUtils

class Profile {


    val step = ProfileEntry()

    val stepInit = ProfileEntry()

    val collide = ProfileEntry()

    val solveParticleSystem = ProfileEntry()

    val solve = ProfileEntry()

    val solveInit = ProfileEntry()

    val solveVelocity = ProfileEntry()

    val solvePosition = ProfileEntry()

    val broadphase = ProfileEntry()

    val solveTOI = ProfileEntry()

    class ProfileEntry {
        internal var longAvg: Float = 0.toFloat()
        internal var shortAvg: Float = 0.toFloat()
        internal var min: Float = 0.toFloat()
        internal var max: Float = 0.toFloat()
        internal var accum: Float = 0.toFloat()

        init {
            min = Float.MAX_VALUE
            max = -Float.MAX_VALUE
        }

        fun record(value: Float) {
            longAvg = longAvg * (1 - LONG_FRACTION) + value * LONG_FRACTION
            shortAvg = shortAvg * (1 - SHORT_FRACTION) + value * SHORT_FRACTION
            min = MathUtils.min(value, min)
            max = MathUtils.max(value, max)
        }

        fun startAccum() {
            accum = 0f
        }

        fun accum(value: Float) {
            accum += value
        }

        fun endAccum() {
            record(accum)
        }

        override fun toString(): String {
            return "$shortAvg ($longAvg) [$min,$max]"
        }
    }

    fun toDebugStrings(strings: MutableList<String>) {
        strings.add("Profile:")
        strings.add(" step: $step")
        strings.add("  init: $stepInit")
        strings.add("  collide: $collide")
        strings.add("  particles: $solveParticleSystem")
        strings.add("  solve: $solve")
        strings.add("   solveInit: $solveInit")
        strings.add("   solveVelocity: $solveVelocity")
        strings.add("   solvePosition: $solvePosition")
        strings.add("   broadphase: $broadphase")
        strings.add("  solveTOI: $solveTOI")
    }

    companion object {
        private val LONG_AVG_NUMS = 20
        private val LONG_FRACTION = 1f / LONG_AVG_NUMS
        private val SHORT_AVG_NUMS = 5
        private val SHORT_FRACTION = 1f / SHORT_AVG_NUMS
    }
}
