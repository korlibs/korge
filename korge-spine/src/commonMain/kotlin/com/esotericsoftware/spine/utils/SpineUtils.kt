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

package com.esotericsoftware.spine.utils

object SpineUtils {
    val PI = 3.1415927f
    val PI2 = PI * 2
    val radiansToDegrees = 180f / PI
    val radDeg = radiansToDegrees
    val degreesToRadians = PI / 180
    val degRad = degreesToRadians

    fun cosDeg(angle: Float): Float {
        return kotlin.math.cos((angle * degRad).toDouble()).toFloat()
    }

    fun sinDeg(angle: Float): Float {
        return kotlin.math.sin((angle * degRad).toDouble()).toFloat()
    }

    fun cos(angle: Float): Float {
        return kotlin.math.cos(angle.toDouble()).toFloat()
    }

    fun sin(angle: Float): Float {
        return kotlin.math.sin(angle.toDouble()).toFloat()
    }

    fun atan2(y: Float, x: Float): Float {
        return kotlin.math.atan2(y.toDouble(), x.toDouble()).toFloat()
    }

    fun arraycopy(src: ByteArray, srcPos: Int, dest: ByteArray, destPos: Int, length: Int) {
        //src.copyInto(dest, destPos, srcPos, srcPos + length)
        com.soywiz.kmem.arraycopy(src, srcPos, dest, destPos, length)
    }

    fun arraycopy(src: ShortArray, srcPos: Int, dest: ShortArray, destPos: Int, length: Int) {
        com.soywiz.kmem.arraycopy(src, srcPos, dest, destPos, length)
    }

    fun arraycopy(src: IntArray, srcPos: Int, dest: IntArray, destPos: Int, length: Int) {
        com.soywiz.kmem.arraycopy(src, srcPos, dest, destPos, length)
    }

    fun arraycopy(src: FloatArray, srcPos: Int, dest: FloatArray, destPos: Int, length: Int) {
        com.soywiz.kmem.arraycopy(src, srcPos, dest, destPos, length)
    }

    fun <T> arraycopy(src: Array<T>, srcPos: Int, dest: Array<T>, destPos: Int, length: Int) {
        com.soywiz.kmem.arraycopy(src, srcPos, dest, destPos, length)
    }

    fun <T> arraycopy(src: JArray<T>, srcPos: Int, dest: JArray<T>, destPos: Int, length: Int) {
        JArray.arraycopy(src, srcPos, dest, destPos, length)
    }
}
