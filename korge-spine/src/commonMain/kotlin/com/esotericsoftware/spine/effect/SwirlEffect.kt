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
import com.esotericsoftware.spine.utils.SpineUtils
import kotlin.math.*

class SwirlEffect(private var radius: Float) : VertexEffect {
    private var worldX: Float = 0.toFloat()
    private var worldY: Float = 0.toFloat()
    private var angle: Float = 0.toFloat()
    var interpolation: (Float) -> Float = { a ->
        val power = 2
        (a - 1.toDouble()).pow(power.toDouble()).toFloat() * (if (power % 2 == 0) -1 else 1) + 1
    }
    private var centerX: Float = 0.toFloat()
    private var centerY: Float = 0.toFloat()

    fun ((Float) -> Float).apply(start: Float, end: Float, a: Float): Float {
        return start + (end - start) * this(a)
    }

    override fun begin(skeleton: Skeleton) {
        worldX = skeleton.x + centerX
        worldY = skeleton.y + centerY
    }

    override fun transform(position: SpineVector2, uv: SpineVector2, light: RGBAf, dark: RGBAf) {
        val x = position.x - worldX
        val y = position.y - worldY
        val dist = sqrt((x * x + y * y).toDouble()).toFloat()
        if (dist < radius) {
            val theta = interpolation.apply(0f, angle, (radius - dist) / radius)
            val cos = SpineUtils.cos(theta)
            val sin = SpineUtils.sin(theta)
            position.x = cos * x - sin * y + worldX
            position.y = sin * x + cos * y + worldY
        }
    }

    override fun end() {}

    fun setRadius(radius: Float) {
        this.radius = radius
    }

    fun setCenter(centerX: Float, centerY: Float) {
        this.centerX = centerX
        this.centerY = centerY
    }

    fun setCenterX(centerX: Float) {
        this.centerX = centerX
    }

    fun setCenterY(centerY: Float) {
        this.centerY = centerY
    }

    fun setAngle(degrees: Float) {
        this.angle = degrees * SpineUtils.degRad
    }
}
