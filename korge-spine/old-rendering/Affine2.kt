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
package com.esotericsoftware.spine.utils

/** A specialized 3x3 matrix that can represent sequences of 2D translations, scales, flips, rotations, and shears. [Affine transformations](http://en.wikipedia.org/wiki/Affine_transformation) preserve straight lines, and
 * parallel lines remain parallel after the transformation. Operations on affine matrices are faster because the last row can
 * always be assumed (0, 0, 1).
 *
 * @author vmilea
 */
// constant: m21 = 0, m21 = 1, m22 = 1
data class Affine2(
    var m00: Float = 1f,
    var m01: Float = 0f,
    var m02: Float = 0f,
    var m10: Float = 0f,
    var m11: Float = 1f,
    var m12: Float = 0f
) {
    override fun toString(): String = "[$m00|$m01|$m02]\n[$m10|$m11|$m12]\n[0.0|0.0|0.1]"
}
