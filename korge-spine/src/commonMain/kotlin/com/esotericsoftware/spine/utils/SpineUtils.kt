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

import com.soywiz.kds.*

object SpineUtils {
    const val PI = 3.1415927f
    const val PI2 = PI * 2
    const val radiansToDegrees = 180f / PI
    const val radDeg = radiansToDegrees
    const val degreesToRadians = PI / 180
    const val degRad = degreesToRadians

    inline fun cosDeg(angle: Float): Float = kotlin.math.cos(angle * degRad)
    inline fun sinDeg(angle: Float): Float = kotlin.math.sin(angle * degRad)
    inline fun cos(angle: Float): Float = kotlin.math.cos(angle)
    inline fun sin(angle: Float): Float = kotlin.math.sin(angle)
    inline fun atan2(y: Float, x: Float): Float = kotlin.math.atan2(y, x)

    /*
    private const val SIN_BITS = 14 // 16KB. Adjust for accuracy.
    private const val SIN_MASK = (-1 shl SIN_BITS).inv()
    private const val SIN_COUNT = SIN_MASK + 1
    private const val radFull = PI * 2
    private const val degFull = 360f
    private const val radToIndex = SIN_COUNT / radFull
    private const val degToIndex = SIN_COUNT / degFull

    /** Returns the sine in radians from a lookup table. For optimal precision, use radians between -PI2 and PI2 (both
     * inclusive).  */

    fun sin(radians: Float): Float = SIN_TABLE[(radians * radToIndex).toInt() and SIN_MASK]

    /** Returns the cosine in radians from a lookup table. For optimal precision, use radians between -PI2 and PI2 (both
     * inclusive).  */

    fun cos(radians: Float): Float = SIN_TABLE[((radians + PI / 2) * radToIndex).toInt() and SIN_MASK]

    /** Returns the sine in degrees from a lookup table. For optimal precision, use radians between -360 and 360 (both
     * inclusive).  */

    fun sinDeg(degrees: Float): Float = SIN_TABLE[(degrees * degToIndex).toInt() and SIN_MASK]

    /** Returns the cosine in degrees from a lookup table. For optimal precision, use radians between -360 and 360 (both
     * inclusive).  */

    fun cosDeg(degrees: Float): Float = SIN_TABLE[((degrees + 90) * degToIndex).toInt() and SIN_MASK]
    // ---
    /** Returns atan2 in radians, less accurate than atan2 but may be faster. Average error of 0.00231 radians (0.1323
     * degrees), largest error of 0.00488 radians (0.2796 degrees).  */

    fun atan2(y: Float, x: Float): Float {
        if (x == 0f) {
            if (y > 0f) return PI / 2
            return if (y == 0f) 0f else -PI / 2
        }
        val atan: Float
        val z = y / x
        if (abs(z) < 1f) {
            atan = z / (1f + 0.28f * z * z)
            return if (x < 0f) atan + (if (y < 0f) -PI else PI) else atan
        }
        atan = PI / 2 - z / (z * z + 0.28f)
        return if (y < 0f) atan - PI else atan
    }

    private val SIN_TABLE = FloatArray(SIN_COUNT).also { table ->
        for (i in 0 until SIN_COUNT) table[i] = sin((i + 0.5f) / SIN_COUNT * radFull.toDouble()).toFloat()
        var i = 0
        while (i < 360) {
            table[(i * degToIndex) as Int and SIN_MASK] = sin(i * degreesToRadians.toDouble()).toFloat()
            i += 90
        }
    }
     */

    inline fun arraycopy(src: ByteArray, srcPos: Int, dest: ByteArray, destPos: Int, length: Int) {
        com.soywiz.kmem.arraycopy(src, srcPos, dest, destPos, length)
    }

    inline fun arraycopy(src: ShortArray, srcPos: Int, dest: ShortArray, destPos: Int, length: Int) {
        com.soywiz.kmem.arraycopy(src, srcPos, dest, destPos, length)
    }

    inline fun arraycopy(src: IntArray, srcPos: Int, dest: IntArray, destPos: Int, length: Int) {
        com.soywiz.kmem.arraycopy(src, srcPos, dest, destPos, length)
    }

    inline fun arraycopy(src: FloatArray, srcPos: Int, dest: FloatArray, destPos: Int, length: Int) {
        com.soywiz.kmem.arraycopy(src, srcPos, dest, destPos, length)
    }

    inline fun <T> arraycopy(src: Array<T>, srcPos: Int, dest: Array<T>, destPos: Int, length: Int) {
        com.soywiz.kmem.arraycopy(src, srcPos, dest, destPos, length)
    }

    inline fun <T> arraycopy(src: FastArrayList<T>, srcPos: Int, dest: FastArrayList<T>, destPos: Int, length: Int) {
        com.soywiz.kmem.arraycopy(src, srcPos, dest, destPos, length)
    }
}
