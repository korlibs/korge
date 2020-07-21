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

/** Encapsulates a 3D vector. Allows chaining operations by returning a reference to itself in all modification methods.
 * @author badlogicgames@gmail.com
 */
data class Vector3(
    /** the x-component of this vector  */
    var x: Float = 0f,
    /** the y-component of this vector  */
    var y: Float = 0f,
    /** the z-component of this vector  */
    var z: Float = 0f
) {

    /** Sets the vector to the given components
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     * @return this vector for chaining
     */
    operator fun set(x: Float, y: Float, z: Float): Vector3 {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    fun set(vector: Vector3): Vector3 {
        return this.set(vector.x, vector.y, vector.z)
    }

    fun add(vector: Vector3): Vector3 {
        return this.add(vector.x, vector.y, vector.z)
    }

    /** Adds the given vector to this component
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @param z The z-component of the other vector
     * @return This vector for chaining.
     */
    fun add(x: Float, y: Float, z: Float): Vector3 {
        return this.set(this.x + x, this.y + y, this.z + z)
    }

    /** Converts this `Vector3` to a string in the format `(x,y,z)`.
     * @return a string representation of this object.
     */
    override fun toString(): String = "($x,$y,$z)"

    companion object {
        val X = Vector3(1f, 0f, 0f)
        val Y = Vector3(0f, 1f, 0f)

        /** @return The euclidean length
         */
        fun len(x: Float, y: Float, z: Float): Float = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
    }
}
/**
 * Compares this vector with the other vector using MathUtils.FLOAT_ROUNDING_ERROR for fuzzy equality testing
 *
 * @param x x component of the other vector to compare
 * @param y y component of the other vector to compare
 * @param z z component of the other vector to compare
 * @return true if vector are equal, otherwise false
 */
