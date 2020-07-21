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

import kotlin.math.*

/** A 3x3 [column major](http://en.wikipedia.org/wiki/Row-major_order#Column-major_order) matrix; useful for 2D
 * transforms.
 *
 * @author mzechner
 */
class Matrix3 {

    val `val` = FloatArray(9)

    /** Get the values in this matrix.
     * @return The float values that make up this matrix in column-major order.
     */
    val values = `val`

    override fun toString(): String {
        val `val` = `val`
        return """
            [${`val`[M00]}|${`val`[M01]}|${`val`[M02]}]
            [${`val`[M10]}|${`val`[M11]}|${`val`[M12]}]
            [${`val`[M20]}|${`val`[M21]}|${`val`[M22]}]
            """.trimIndent()
    }

    val rotation: Float
        get() = MathUtils.radiansToDegrees * atan2(`val`[M10].toDouble(), `val`[M00].toDouble()).toFloat()

    companion object {
        const val M00 = 0
        const val M01 = 3
        const val M02 = 6
        const val M10 = 1
        const val M11 = 4
        const val M12 = 7
        const val M20 = 2
        const val M21 = 5
        const val M22 = 8

    }
}
