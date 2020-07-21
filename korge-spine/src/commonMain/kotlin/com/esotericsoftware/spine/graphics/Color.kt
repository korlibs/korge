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
package com.esotericsoftware.spine.graphics

import com.esotericsoftware.spine.utils.*

/** A color class, holding the r, g, b and alpha component as floats in the range [0,1]. All methods perform clamping on the
 * internal values after execution.
 *
 * @author mzechner
 */
data class Color(
    var r: Float = 0f,
    var g: Float = 0f,
    var b: Float = 0f,
    var a: Float = 0f
) {
    init {
        clamp()
    }

    /** the red, green, blue and alpha components  */

    /** Constructs a new color using the given color
     *
     * @param color the color
     */
    constructor(color: Color) : this(color.r, color.g, color.b, color.a)

    /** Sets this color to the given color.
     *
     * @param color the Color
     */
    fun set(color: Color): Color {
        r = color.r
        g = color.g
        b = color.b
        a = color.a
        return this
    }

    /** Clamps this Color's components to a valid range [0 - 1]
     * @return this Color for chaining
     */
    fun clamp(): Color {
        if (r < 0) r = 0f else if (r > 1) r = 1f
        if (g < 0) g = 0f else if (g > 1) g = 1f
        if (b < 0) b = 0f else if (b > 1) b = 1f
        if (a < 0) a = 0f else if (a > 1) a = 1f
        return this
    }

    /** Sets this Color's component values.
     *
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @param a Alpha component
     *
     * @return this Color for chaining
     */
    operator fun set(r: Float, g: Float, b: Float, a: Float): Color {
        this.r = r
        this.g = g
        this.b = b
        this.a = a
        return clamp()
    }

    /** Sets this color's component values through an integer representation.
     *
     * @return this Color for chaining
     * @see .rgba8888ToColor
     */
    fun set(rgba: Int): Color {
        rgba8888ToColor(this, rgba)
        return this
    }

    /** Adds the given color component values to this Color's values.
     *
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @param a Alpha component
     *
     * @return this Color for chaining
     */
    fun add(r: Float, g: Float, b: Float, a: Float): Color {
        this.r += r
        this.g += g
        this.b += b
        this.a += a
        return clamp()
    }

    /** Packs the color components into a 32-bit integer with the format ABGR and then converts it to a float. Alpha is compressed
     * from 0-255 to 0-254 to avoid using float bits in the NaN range (see [NumberUtils.intToFloatColor]).
     * @return the packed color as a 32-bit float
     */
    fun toFloatBits(): Float {
        val color = (255 * a).toInt() shl 24 or ((255 * b).toInt() shl 16) or ((255 * g).toInt() shl 8) or (255 * r).toInt()
        return NumberUtils.intToFloatColor(color)
    }

    /** Returns the color encoded as hex string with the format RRGGBBAA.  */
    override fun toString(): String {
        var value = "0x" + ((255 * r).toInt() shl 24 or ((255 * g).toInt() shl 16) or ((255 * b).toInt() shl 8) or (255 * a).toInt()).toString(16)
        while (value.length < 8) value = "0$value"
        return value
    }

    companion object {
        val WHITE = Color(1f, 1f, 1f, 1f)
        val LIGHT_GRAY = Color(.75f, .75f, .75f, 1f)
        val BLACK = Color(0f, 0f, 0f, 1f)
        val GREEN = Color(0f, 1f, 0f, 1f)
        val RED = Color(1f, 0f, 0f, 1f)

        /** Sets the specified color from a hex string with the format RRGGBBAA.
         * @see .toString
         */
        /** Returns a new color from a hex string with the format RRGGBBAA.
         * @see .toString
         */
        fun valueOf(hex: String, color: Color = Color()): Color {
            var hex = hex
            hex = if (hex[0] == '#') hex.substring(1) else hex
            color.r = hex.substring(0, 2).toInt(16) / 255f
            color.g = hex.substring(2, 4).toInt(16) / 255f
            color.b = hex.substring(4, 6).toInt(16) / 255f
            color.a = if (hex.length != 8) 1f else hex.substring(6, 8).toInt(16) / 255f
            return color
        }

        /** Sets the Color components using the specified integer value in the format RGB888. This is inverse to the rgb888(r, g, b)
         * method.
         *
         * @param color The Color to be modified.
         * @param value An integer color value in RGB888 format.
         */

        fun rgb888ToColor(color: Color, value: Int) {
            color.r = (value and 0x00ff0000 ushr 16) / 255f
            color.g = (value and 0x0000ff00 ushr 8) / 255f
            color.b = (value and 0x000000ff) / 255f
        }

        /** Sets the Color components using the specified integer value in the format RGBA8888. This is inverse to the rgba8888(r, g,
         * b, a) method.
         *
         * @param color The Color to be modified.
         * @param value An integer color value in RGBA8888 format.
         */

        fun rgba8888ToColor(color: Color, value: Int) {
            color.r = (value and -0x1000000 ushr 24) / 255f
            color.g = (value and 0x00ff0000 ushr 16) / 255f
            color.b = (value and 0x0000ff00 ushr 8) / 255f
            color.a = (value and 0x000000ff) / 255f
        }
    }
}
