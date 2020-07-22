/******************************************************************************
 * Spine Runtimes License Agreement
 * Last updated January 1, 2020. Replaces all prior versions.
 *
 * Copyright (c) 2013-2020, Esoteric Software LLC
 *
 * Integration of the Spine Runtimes into software or otherwise creating
 * derivative works of the Spine Runtimes is permitted under the terms and
 * conditions of Section 2 of the Spine Editor License Agreement:
 * http://esotericsoftware.com/spine-editor-license
 *
 * Otherwise, it is permitted to integrate the Spine Runtimes into software
 * or otherwise create derivative works of the Spine Runtimes (collectively,
 * "Products"), provided that each user of the Products must obtain their own
 * Spine Editor license and redistribution of the Products in any form must
 * include this license and copyright notice.
 *
 * THE SPINE RUNTIMES ARE PROVIDED BY ESOTERIC SOFTWARE LLC "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL ESOTERIC SOFTWARE LLC BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES,
 * BUSINESS INTERRUPTION, OR LOSS OF USE, DATA, OR PROFITS) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THE SPINE RUNTIMES, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.esotericsoftware.spine.effect

import com.soywiz.korim.color.RGBAf
import com.esotericsoftware.spine.utils.SpineVector2
import com.esotericsoftware.spine.Skeleton
import kotlin.random.Random

class JitterEffect(private var x: Float, private var y: Float, val random: Random = Random) : VertexEffect {
    override fun begin(skeleton: Skeleton) {}

    private fun randomTriangular(min: Float, max: Float, mode: Float = (min + max) * 0.5f): Float {
        val u = random.nextFloat()
        val d = max - min
        return if (u <= (mode - min) / d) min + kotlin.math.sqrt(u * d * (mode - min).toDouble()).toFloat() else max - kotlin.math.sqrt((1 - u) * d * (max - mode).toDouble()).toFloat()
    }

    override fun transform(position: SpineVector2, uv: SpineVector2, light: RGBAf, dark: RGBAf) {
        position.x += randomTriangular(-x, y)
        position.y += randomTriangular(-x, y)
    }

    override fun end() {}

    fun setJitter(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    fun setJitterX(x: Float) {
        this.x = x
    }

    fun setJitterY(y: Float) {
        this.y = y
    }
}
