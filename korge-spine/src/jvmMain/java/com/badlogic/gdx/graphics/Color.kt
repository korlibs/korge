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
package com.badlogic.gdx.graphics

import com.badlogic.gdx.utils.*

/** A color class, holding the r, g, b and alpha component as floats in the range [0,1]. All methods perform clamping on the
 * internal values after execution.
 *
 * @author mzechner
 */
class Color {
    /** the red, green, blue and alpha components  */
    @JvmField
    var r = 0f
    @JvmField
    var g = 0f
    @JvmField
    var b = 0f
    @JvmField
    var a = 0f

    /** Constructs a new Color with all components set to 0.  */
    constructor() {}

    /** @see .rgba8888ToColor
     */
    constructor(rgba8888: Int) {
        rgba8888ToColor(this, rgba8888)
    }

    /** Constructor, sets the components of the color
     *
     * @param r the red component
     * @param g the green component
     * @param b the blue component
     * @param a the alpha component
     */
    constructor(r: Float, g: Float, b: Float, a: Float) {
        this.r = r
        this.g = g
        this.b = b
        this.a = a
        clamp()
    }

    /** Constructs a new color using the given color
     *
     * @param color the color
     */
    constructor(color: Color) {
        set(color)
    }

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

    /** Multiplies the this color and the given color
     *
     * @param color the color
     * @return this color.
     */
    fun mul(color: Color): Color {
        r *= color.r
        g *= color.g
        b *= color.b
        a *= color.a
        return clamp()
    }

    /** Multiplies all components of this Color with the given value.
     *
     * @param value the value
     * @return this color
     */
    fun mul(value: Float): Color {
        r *= value
        g *= value
        b *= value
        a *= value
        return clamp()
    }

    /** Adds the given color to this color.
     *
     * @param color the color
     * @return this color
     */
    fun add(color: Color): Color {
        r += color.r
        g += color.g
        b += color.b
        a += color.a
        return clamp()
    }

    /** Subtracts the given color from this color
     *
     * @param color the color
     * @return this color
     */
    fun sub(color: Color): Color {
        r -= color.r
        g -= color.g
        b -= color.b
        a -= color.a
        return clamp()
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

    /** Subtracts the given values from this Color's component values.
     *
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @param a Alpha component
     *
     * @return this Color for chaining
     */
    fun sub(r: Float, g: Float, b: Float, a: Float): Color {
        this.r -= r
        this.g -= g
        this.b -= b
        this.a -= a
        return clamp()
    }

    /** Multiplies this Color's color components by the given ones.
     *
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @param a Alpha component
     *
     * @return this Color for chaining
     */
    fun mul(r: Float, g: Float, b: Float, a: Float): Color {
        this.r *= r
        this.g *= g
        this.b *= b
        this.a *= a
        return clamp()
    }

    /** Linearly interpolates between this color and the target color by t which is in the range [0,1]. The result is stored in
     * this color.
     * @param target The target color
     * @param t The interpolation coefficient
     * @return This color for chaining.
     */
    fun lerp(target: Color, t: Float): Color {
        r += t * (target.r - r)
        g += t * (target.g - g)
        b += t * (target.b - b)
        a += t * (target.a - a)
        return clamp()
    }

    /** Linearly interpolates between this color and the target color by t which is in the range [0,1]. The result is stored in
     * this color.
     * @param r The red component of the target color
     * @param g The green component of the target color
     * @param b The blue component of the target color
     * @param a The alpha component of the target color
     * @param t The interpolation coefficient
     * @return This color for chaining.
     */
    fun lerp(r: Float, g: Float, b: Float, a: Float, t: Float): Color {
        this.r += t * (r - this.r)
        this.g += t * (g - this.g)
        this.b += t * (b - this.b)
        this.a += t * (a - this.a)
        return clamp()
    }

    /** Multiplies the RGB values by the alpha.  */
    fun premultiplyAlpha(): Color {
        r *= a
        g *= a
        b *= a
        return this
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val color = o as Color
        return toIntBits() == color.toIntBits()
    }

    override fun hashCode(): Int {
        var result = if (r != +0.0f) NumberUtils.floatToIntBits(r) else 0
        result = 31 * result + if (g != +0.0f) NumberUtils.floatToIntBits(g) else 0
        result = 31 * result + if (b != +0.0f) NumberUtils.floatToIntBits(b) else 0
        result = 31 * result + if (a != +0.0f) NumberUtils.floatToIntBits(a) else 0
        return result
    }

    /** Packs the color components into a 32-bit integer with the format ABGR and then converts it to a float. Alpha is compressed
     * from 0-255 to 0-254 to avoid using float bits in the NaN range (see [NumberUtils.intToFloatColor]).
     * @return the packed color as a 32-bit float
     */
    fun toFloatBits(): Float {
        val color = (255 * a).toInt() shl 24 or ((255 * b).toInt() shl 16) or ((255 * g).toInt() shl 8) or (255 * r).toInt()
        return NumberUtils.intToFloatColor(color)
    }

    /** Packs the color components into a 32-bit integer with the format ABGR.
     * @return the packed color as a 32-bit int.
     */
    fun toIntBits(): Int {
        return (255 * a).toInt() shl 24 or ((255 * b).toInt() shl 16) or ((255 * g).toInt() shl 8) or (255 * r).toInt()
    }

    /** Returns the color encoded as hex string with the format RRGGBBAA.  */
    override fun toString(): String {
        var value = Integer
            .toHexString((255 * r).toInt() shl 24 or ((255 * g).toInt() shl 16) or ((255 * b).toInt() shl 8) or (255 * a).toInt())
        while (value.length < 8) value = "0$value"
        return value
    }

    /** Sets the RGB Color components using the specified Hue-Saturation-Value. Note that HSV components are voluntary not clamped
     * to preserve high range color and can range beyond typical values.
     * @param h The Hue in degree from 0 to 360
     * @param s The Saturation from 0 to 1
     * @param v The Value (brightness) from 0 to 1
     * @return The modified Color for chaining.
     */
    fun fromHsv(h: Float, s: Float, v: Float): Color {
        val x = (h / 60f + 6) % 6
        val i = x.toInt()
        val f = x - i
        val p = v * (1 - s)
        val q = v * (1 - s * f)
        val t = v * (1 - s * (1 - f))
        when (i) {
            0 -> {
                r = v
                g = t
                b = p
            }
            1 -> {
                r = q
                g = v
                b = p
            }
            2 -> {
                r = p
                g = v
                b = t
            }
            3 -> {
                r = p
                g = q
                b = v
            }
            4 -> {
                r = t
                g = p
                b = v
            }
            else -> {
                r = v
                g = p
                b = q
            }
        }
        return clamp()
    }

    /** Sets RGB components using the specified Hue-Saturation-Value. This is a convenient method for
     * [.fromHsv]. This is the inverse of [.toHsv].
     * @param hsv The Hue, Saturation and Value components in that order.
     * @return The modified Color for chaining.
     */
    fun fromHsv(hsv: FloatArray): Color {
        return fromHsv(hsv[0], hsv[1], hsv[2])
    }

    /** Extract Hue-Saturation-Value. This is the inverse of [.fromHsv].
     * @param hsv The HSV array to be modified.
     * @return HSV components for chaining.
     */
    fun toHsv(hsv: FloatArray): FloatArray {
        val max = Math.max(Math.max(r, g), b)
        val min = Math.min(Math.min(r, g), b)
        val range = max - min
        if (range == 0f) {
            hsv[0] = 0f
        } else if (max == r) {
            hsv[0] = (60 * (g - b) / range + 360) % 360
        } else if (max == g) {
            hsv[0] = 60 * (b - r) / range + 120
        } else {
            hsv[0] = 60 * (r - g) / range + 240
        }
        if (max > 0) {
            hsv[1] = 1 - min / max
        } else {
            hsv[1] = 0f
        }
        hsv[2] = max
        return hsv
    }

    /** @return a copy of this color
     */
    fun cpy(): Color {
        return Color(this)
    }

    companion object {
        @JvmField
        val WHITE = Color(1f, 1f, 1f, 1f)
        @JvmField
        val LIGHT_GRAY = Color(-0x40404001)
        @JvmField
        val GRAY = Color(0x7f7f7fff)
        @JvmField
        val DARK_GRAY = Color(0x3f3f3fff)
        @JvmField
        val BLACK = Color(0f, 0f, 0f, 1f)

        /** Convenience for frequently used `WHITE.toFloatBits()`  */
        @JvmField
        val WHITE_FLOAT_BITS = WHITE.toFloatBits()
        @JvmField
        val CLEAR = Color(0f, 0f, 0f, 0f)
        @JvmField
        val BLUE = Color(0f, 0f, 1f, 1f)
        @JvmField
        val NAVY = Color(0f, 0f, 0.5f, 1f)
        @JvmField
        val ROYAL = Color(0x4169e1ff)
        @JvmField
        val SLATE = Color(0x708090ff)
        @JvmField
        val SKY = Color(-0x78311401)
        @JvmField
        val CYAN = Color(0f, 1f, 1f, 1f)
        @JvmField
        val TEAL = Color(0f, 0.5f, 0.5f, 1f)
        @JvmField
        val GREEN = Color(0x00ff00ff)
        @JvmField
        val CHARTREUSE = Color(0x7fff00ff)
        @JvmField
        val LIME = Color(0x32cd32ff)
        @JvmField
        val FOREST = Color(0x228b22ff)
        @JvmField
        val OLIVE = Color(0x6b8e23ff)
        @JvmField
        val YELLOW = Color(-0xff01)
        @JvmField
        val GOLD = Color(-0x28ff01)
        @JvmField
        val GOLDENROD = Color(-0x255adf01)
        @JvmField
        val ORANGE = Color(-0x5aff01)
        @JvmField
        val BROWN = Color(-0x74baec01)
        @JvmField
        val TAN = Color(-0x2d4b7301)
        @JvmField
        val FIREBRICK = Color(-0x4ddddd01)
        @JvmField
        val RED = Color(-0xffff01)
        @JvmField
        val SCARLET = Color(-0xcbe301)
        @JvmField
        val CORAL = Color(-0x80af01)
        @JvmField
        val SALMON = Color(-0x57f8d01)
        @JvmField
        val PINK = Color(-0x964b01)
        @JvmField
        val MAGENTA = Color(1f, 0f, 1f, 1f)
        @JvmField
        val PURPLE = Color(-0x5fdf0f01)
        @JvmField
        val VIOLET = Color(-0x117d1101)
        @JvmField
        val MAROON = Color(-0x4fcf9f01)
        /** Sets the specified color from a hex string with the format RRGGBBAA.
         * @see .toString
         */
        /** Returns a new color from a hex string with the format RRGGBBAA.
         * @see .toString
         */
        @JvmOverloads
        @JvmStatic
        fun valueOf(hex: String, color: Color = Color()): Color {
            var hex = hex
            hex = if (hex[0] == '#') hex.substring(1) else hex
            color.r = hex.substring(0, 2).toInt(16) / 255f
            color.g = hex.substring(2, 4).toInt(16) / 255f
            color.b = hex.substring(4, 6).toInt(16) / 255f
            color.a = if (hex.length != 8) 1f else hex.substring(6, 8).toInt(16) / 255f
            return color
        }

        /** Packs the color components into a 32-bit integer with the format ABGR and then converts it to a float. Note that no range
         * checking is performed for higher performance.
         * @param r the red component, 0 - 255
         * @param g the green component, 0 - 255
         * @param b the blue component, 0 - 255
         * @param a the alpha component, 0 - 255
         * @return the packed color as a float
         * @see NumberUtils.intToFloatColor
         */
        @JvmStatic
        fun toFloatBits(r: Int, g: Int, b: Int, a: Int): Float {
            val color = a shl 24 or (b shl 16) or (g shl 8) or r
            return NumberUtils.intToFloatColor(color)
        }

        /** Packs the color components into a 32-bit integer with the format ABGR and then converts it to a float.
         * @return the packed color as a 32-bit float
         * @see NumberUtils.intToFloatColor
         */
        @JvmStatic
        fun toFloatBits(r: Float, g: Float, b: Float, a: Float): Float {
            val color = (255 * a).toInt() shl 24 or ((255 * b).toInt() shl 16) or ((255 * g).toInt() shl 8) or (255 * r).toInt()
            return NumberUtils.intToFloatColor(color)
        }

        /** Packs the color components into a 32-bit integer with the format ABGR. Note that no range checking is performed for higher
         * performance.
         * @param r the red component, 0 - 255
         * @param g the green component, 0 - 255
         * @param b the blue component, 0 - 255
         * @param a the alpha component, 0 - 255
         * @return the packed color as a 32-bit int
         */
        @JvmStatic
        fun toIntBits(r: Int, g: Int, b: Int, a: Int): Int {
            return a shl 24 or (b shl 16) or (g shl 8) or r
        }

        @JvmStatic
        fun alpha(alpha: Float): Int {
            return (alpha * 255.0f).toInt()
        }

        @JvmStatic
        fun luminanceAlpha(luminance: Float, alpha: Float): Int {
            return (luminance * 255.0f).toInt() shl 8 or (alpha * 255).toInt()
        }

        @JvmStatic
        fun rgb565(r: Float, g: Float, b: Float): Int {
            return (r * 31).toInt() shl 11 or ((g * 63).toInt() shl 5) or (b * 31).toInt()
        }

        @JvmStatic
        fun rgba4444(r: Float, g: Float, b: Float, a: Float): Int {
            return (r * 15).toInt() shl 12 or ((g * 15).toInt() shl 8) or ((b * 15).toInt() shl 4) or (a * 15).toInt()
        }

        @JvmStatic
        fun rgb888(r: Float, g: Float, b: Float): Int {
            return (r * 255).toInt() shl 16 or ((g * 255).toInt() shl 8) or (b * 255).toInt()
        }

        @JvmStatic
        fun rgba8888(r: Float, g: Float, b: Float, a: Float): Int {
            return (r * 255).toInt() shl 24 or ((g * 255).toInt() shl 16) or ((b * 255).toInt() shl 8) or (a * 255).toInt()
        }

        @JvmStatic
        fun argb8888(a: Float, r: Float, g: Float, b: Float): Int {
            return (a * 255).toInt() shl 24 or ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt()
        }

        @JvmStatic
        fun rgb565(color: Color): Int {
            return (color.r * 31).toInt() shl 11 or ((color.g * 63).toInt() shl 5) or (color.b * 31).toInt()
        }

        @JvmStatic
        fun rgba4444(color: Color): Int {
            return (color.r * 15).toInt() shl 12 or ((color.g * 15).toInt() shl 8) or ((color.b * 15).toInt() shl 4) or (color.a * 15).toInt()
        }

        @JvmStatic
        fun rgb888(color: Color): Int {
            return (color.r * 255).toInt() shl 16 or ((color.g * 255).toInt() shl 8) or (color.b * 255).toInt()
        }

        @JvmStatic
        fun rgba8888(color: Color): Int {
            return (color.r * 255).toInt() shl 24 or ((color.g * 255).toInt() shl 16) or ((color.b * 255).toInt() shl 8) or (color.a * 255).toInt()
        }

        @JvmStatic
        fun argb8888(color: Color): Int {
            return (color.a * 255).toInt() shl 24 or ((color.r * 255).toInt() shl 16) or ((color.g * 255).toInt() shl 8) or (color.b * 255).toInt()
        }

        /** Sets the Color components using the specified integer value in the format RGB565. This is inverse to the rgb565(r, g, b)
         * method.
         *
         * @param color The Color to be modified.
         * @param value An integer color value in RGB565 format.
         */
        @JvmStatic
        fun rgb565ToColor(color: Color, value: Int) {
            color.r = (value and 0x0000F800 ushr 11) / 31f
            color.g = (value and 0x000007E0 ushr 5) / 63f
            color.b = (value and 0x0000001F ushr 0) / 31f
        }

        /** Sets the Color components using the specified integer value in the format RGBA4444. This is inverse to the rgba4444(r, g,
         * b, a) method.
         *
         * @param color The Color to be modified.
         * @param value An integer color value in RGBA4444 format.
         */
        @JvmStatic
        fun rgba4444ToColor(color: Color, value: Int) {
            color.r = (value and 0x0000f000 ushr 12) / 15f
            color.g = (value and 0x00000f00 ushr 8) / 15f
            color.b = (value and 0x000000f0 ushr 4) / 15f
            color.a = (value and 0x0000000f) / 15f
        }

        /** Sets the Color components using the specified integer value in the format RGB888. This is inverse to the rgb888(r, g, b)
         * method.
         *
         * @param color The Color to be modified.
         * @param value An integer color value in RGB888 format.
         */
        @JvmStatic
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
        @JvmStatic
        fun rgba8888ToColor(color: Color, value: Int) {
            color.r = (value and -0x1000000 ushr 24) / 255f
            color.g = (value and 0x00ff0000 ushr 16) / 255f
            color.b = (value and 0x0000ff00 ushr 8) / 255f
            color.a = (value and 0x000000ff) / 255f
        }

        /** Sets the Color components using the specified integer value in the format ARGB8888. This is the inverse to the argb8888(a,
         * r, g, b) method
         *
         * @param color The Color to be modified.
         * @param value An integer color value in ARGB8888 format.
         */
        @JvmStatic
        fun argb8888ToColor(color: Color, value: Int) {
            color.a = (value and -0x1000000 ushr 24) / 255f
            color.r = (value and 0x00ff0000 ushr 16) / 255f
            color.g = (value and 0x0000ff00 ushr 8) / 255f
            color.b = (value and 0x000000ff) / 255f
        }

        /** Sets the Color components using the specified float value in the format ABGR8888.
         * @param color The Color to be modified.
         */
        @JvmStatic
        fun abgr8888ToColor(color: Color, value: Float) {
            val c = NumberUtils.floatToIntColor(value)
            color.a = (c and -0x1000000 ushr 24) / 255f
            color.b = (c and 0x00ff0000 ushr 16) / 255f
            color.g = (c and 0x0000ff00 ushr 8) / 255f
            color.r = (c and 0x000000ff) / 255f
        }
    }
}
