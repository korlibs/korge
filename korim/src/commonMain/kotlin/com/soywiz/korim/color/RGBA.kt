package com.soywiz.korim.color

import com.soywiz.kds.GenericListIterator
import com.soywiz.kds.GenericSubList
import com.soywiz.kmem.*
import com.soywiz.korim.internal.*
import com.soywiz.korim.internal.clamp0_255
import com.soywiz.korim.internal.d2i
import com.soywiz.korim.internal.f2i
import com.soywiz.korim.paint.*
import com.soywiz.korio.util.niceStr
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.Interpolable
import com.soywiz.korma.interpolation.interpolate
import com.soywiz.krypto.encoding.*

inline class RGBA(val value: Int) : Comparable<RGBA>, Interpolable<RGBA>, Paint {
    override fun transformed(m: Matrix): Paint = this
    val color: RGBA get() = this

    val r: Int get() = value.extract8(RED_OFFSET)
	val g: Int get() = value.extract8(GREEN_OFFSET)
	val b: Int get() = value.extract8(BLUE_OFFSET)
	val a: Int get() = value.extract8(ALPHA_OFFSET)

	val rf: Float get() = r.toFloat() / 255f
	val gf: Float get() = g.toFloat() / 255f
	val bf: Float get() = b.toFloat() / 255f
	val af: Float get() = a.toFloat() / 255f

	val rd: Double get() = r.toDouble() / 255.0
	val gd: Double get() = g.toDouble() / 255.0
	val bd: Double get() = b.toDouble() / 255.0
	val ad: Double get() = a.toDouble() / 255.0

	val rgb: Int get() = value and 0xFFFFFF

    fun readFloat(out: FloatArray, index: Int = 0) {
        out[index + 0] = rf
        out[index + 1] = gf
        out[index + 2] = bf
        out[index + 3] = af
    }

	fun withR(v: Int): RGBA = RGBA((value and (0xFF shl 0).inv()) or (v.clamp0_255() shl RED_OFFSET))
	fun withG(v: Int): RGBA = RGBA((value and (0xFF shl 8).inv()) or (v.clamp0_255() shl GREEN_OFFSET))
	fun withB(v: Int): RGBA = RGBA((value and (0xFF shl 16).inv()) or (v.clamp0_255() shl BLUE_OFFSET))
	fun withA(v: Int): RGBA = RGBA((value and (0xFF shl 24).inv()) or (v.clamp0_255() shl ALPHA_OFFSET))
    //fun withRGB(r: Int, g: Int, b: Int) = withR(r).withG(g).withB(b)
    fun withRGB(r: Int, g: Int, b: Int): RGBA =
        RGBA((value and 0x00FFFFFF.inv()) or (r.clamp0_255() shl RED_OFFSET) or (g.clamp0_255() shl GREEN_OFFSET) or (b.clamp0_255() shl BLUE_OFFSET))
	fun withRGB(rgb: Int): RGBA = RGBA(rgb, a)

    fun withRGBUnclamped(r: Int, g: Int, b: Int): RGBA =
        RGBA((value and 0x00FFFFFF.inv()) or ((r and 0xFF) shl RED_OFFSET) or ((g and 0xFF) shl GREEN_OFFSET) or ((b and 0xFF) shl BLUE_OFFSET))

    fun withRd(v: Double): RGBA = withR(d2i(v))
    fun withGd(v: Double): RGBA = withG(d2i(v))
    fun withBd(v: Double): RGBA = withB(d2i(v))
    fun withAd(v: Double): RGBA = withA(d2i(v))

    fun withRf(v: Float): RGBA = withR(f2i(v))
    fun withGf(v: Float): RGBA = withG(f2i(v))
    fun withBf(v: Float): RGBA = withB(f2i(v))
    fun withAf(v: Float): RGBA = withA(f2i(v))

    fun getComponent(c: Int): Int = when (c) {
        0 -> r
        1 -> g
        2 -> b
        3 -> a
        else -> r
    }

    fun getComponent(c: Char): Int = when (c.toLowerCase()) {
        'r' -> r
        'g' -> g
        'b' -> b
        'a' -> a
        else -> r
    }

    val hexString: String get() = buildString {
        // "#%02x%02x%02x%02x".format(r, g, b, a)
        append('#')
        appendHexByte(r)
        appendHexByte(g)
        appendHexByte(b)
        appendHexByte(a)
    }
    val hexStringNoAlpha: String get() = buildString {
        //"#%02x%02x%02x".format(r, g, b)
        append('#')
        appendHexByte(r)
        appendHexByte(g)
        appendHexByte(b)
    }
	val htmlColor: String get() = "rgba($r, $g, $b, ${af.niceStr})"
	val htmlStringSimple: String get() = hexStringNoAlpha

	override fun toString(): String = hexString

	operator fun plus(other: RGBA): RGBA = RGBA(this.r + other.r, this.g + other.g, this.b + other.b, this.a + other.a)
	operator fun minus(other: RGBA): RGBA = RGBA(this.r - other.r, this.g - other.g, this.b - other.b, this.a - other.a)

    override operator fun compareTo(other: RGBA): Int = this.value.compareTo(other.value)
    override fun interpolateWith(ratio: Double, other: RGBA): RGBA = RGBA.interpolate(this, other, ratio)

    val premultiplied: RGBAPremultiplied get() {
        val A = a + 1
        val RB = (((value and 0x00FF00FF) * A) ushr 8) and 0x00FF00FF
        val G = (((value and 0x0000FF00) * A) ushr 8) and 0x0000FF00
        return RGBAPremultiplied((value and 0x00FFFFFF.inv()) or RB or G)
    }

    infix fun mix(dst: RGBA): RGBA = RGBA.mix(this, dst)
    operator fun times(other: RGBA): RGBA = RGBA.multiply(this, other)

    companion object : ColorFormat32() {
        internal const val RED_OFFSET = 0
        internal const val GREEN_OFFSET = 8
        internal const val BLUE_OFFSET = 16
        internal const val ALPHA_OFFSET = 24

        fun float(array: FloatArray, index: Int = 0): RGBA = float(array[index + 0], array[index + 1], array[index + 2], array[index + 3])
        fun float(r: Float, g: Float, b: Float, a: Float): RGBA = unclamped(f2i(r), f2i(g), f2i(b), f2i(a))
        inline fun float(r: Number, g: Number, b: Number, a: Number = 1f): RGBA = float(r.toFloat(), g.toFloat(), b.toFloat(), a.toFloat())
        fun unclamped(r: Int, g: Int, b: Int, a: Int): RGBA = RGBA(packIntUnchecked(r, g, b, a))
		operator fun invoke(r: Int, g: Int, b: Int, a: Int): RGBA = RGBA(packIntClamped(r, g, b, a))
        operator fun invoke(r: Int, g: Int, b: Int): RGBA = RGBA(packIntClamped(r, g, b, 0xFF))
		operator fun invoke(rgb: Int, a: Int): RGBA = RGBA((rgb and 0xFFFFFF) or (a shl 24))
        operator fun invoke(rgba: RGBA): RGBA = rgba
		override fun getR(v: Int): Int = RGBA(v).r
		override fun getG(v: Int): Int = RGBA(v).g
		override fun getB(v: Int): Int = RGBA(v).b
		override fun getA(v: Int): Int = RGBA(v).a
		override fun pack(r: Int, g: Int, b: Int, a: Int): Int = RGBA(r, g, b, a).value
        fun packUnsafe(r: Int, g: Int, b: Int, a: Int): RGBA = RGBA(r or (g shl 8) or (b shl 16) or (a shl 24))

		//fun mutliplyByAlpha(v: Int, alpha: Double): Int = com.soywiz.korim.color.RGBA.pack(RGBA(v).r, RGBA(v).g, RGBA(v).b, (RGBA(v).a * alpha).toInt())
		//fun depremultiply(v: RGBA): RGBA = v.asPremultiplied().depremultiplied

        fun mixRgbFactor256(c1: RGBA, c2: RGBA, factor256: Int): RGBA =
            RGBA(mixRgbFactor256(c1.value, c2.value, factor256))

        fun mixRgbFactor256(c1: Int, c2: Int, factor256: Int): Int {
            val ifactor256 = (256 - factor256)
            return ((((((c1 and 0xFF00FF) * ifactor256) +
                    ((c2 and 0xFF00FF) * factor256)) and 0xFF00FF00.toInt()) or
                    ((((c1 and 0x00FF00) * ifactor256) + ((c2 and 0x00FF00) * factor256)) and 0x00FF0000))) ushr 8

        }
		fun mixRgb(c1: RGBA, c2: RGBA, factor: Double): RGBA = mixRgbFactor256(c1, c2, (factor * 256).toInt())

        fun mixRgba(c1: RGBA, c2: RGBA, factor: Double): RGBA =
            RGBA(mixRgb(c1, c2, factor).rgb, blendComponent(c1.a, c2.a, factor))

        private fun blendComponent(c1: Int, c2: Int, factor: Double): Int = (c1 * (1.0 - factor) + c2 * factor).toInt() and 0xFF

        fun mix(dst: RGBA, src: RGBA): RGBA {
            val srcA = src.a
            val iSrcA = 255 - srcA
            return when (srcA) {
                0x000 -> dst
                0xFF -> src
                else -> RGBA(mixRgbFactor256(dst, src, srcA + 1).rgb, (srcA + (dst.a * iSrcA) / 255).clamp0_255())
            }
        }

        // @TODO: Optimizable
        fun multiply(c1: RGBA, c2: RGBA): RGBA = RGBA(
            ((c1.r * c2.r) / 0xFF),
            ((c1.g * c2.g) / 0xFF),
            ((c1.b * c2.b) / 0xFF),
            ((c1.a * c2.a) / 0xFF)
        )

        fun interpolate(src: RGBA, dst: RGBA, ratio: Double): RGBA = RGBA(
            ratio.interpolate(src.r, dst.r),
            ratio.interpolate(src.g, dst.g),
            ratio.interpolate(src.b, dst.b),
            ratio.interpolate(src.a, dst.a)
        )
    }
}

fun Double.interpolate(a: RGBA, b: RGBA): RGBA = RGBA.interpolate(a, b, this)

inline class RGBAPremultiplied(val value: Int) {
    constructor(rgb: Int, a: Int) : this((rgb and 0xFFFFFF) or (a shl 24))
    constructor(r: Int, g: Int, b: Int, a: Int) : this(packIntClamped(r, g, b, a))

    val rgb: Int get() = value and 0xFFFFFF
    val r: Int get() = (value ushr 0) and 0xFF
    val g: Int get() = (value ushr 8) and 0xFF
    val b: Int get() = (value ushr 16) and 0xFF
    val a: Int get() = (value ushr 24) and 0xFF

    val rf: Float get() = r.toFloat() / 255f
    val gf: Float get() = g.toFloat() / 255f
    val bf: Float get() = b.toFloat() / 255f
    val af: Float get() = a.toFloat() / 255f

    val rd: Double get() = r.toDouble() / 255.0
    val gd: Double get() = g.toDouble() / 255.0
    val bd: Double get() = b.toDouble() / 255.0
    val ad: Double get() = a.toDouble() / 255.0

    // @TODO: Use SIMD
    val depremultiplied: RGBA get() {
        //val A = (value ushr 24) + 1
        //val R = (((value and 0x0000FF) shl 8) / A) and 0x0000F0
        //val G = (((value and 0x00FF00) shl 8) / A) and 0x00FF00
        //val B = (((value and 0xFF0000) shl 8) / A) and 0xFF0000
        //return RGBA((value and 0x00FFFFFF.inv()) or B or G or R)

        //val A = a
        //val A1 = A + 1
        //val R = ((r shl 8) / A1) and 0xFF
        //val G = ((g shl 8) / A1) and 0xFF
        //val B = ((b shl 8) / A1) and 0xFF
        //return RGBA(R, G, B, A)

        //val A = a
        //val R = ((r * 255) / a) and 0xFF
        //val G = ((g * 255) / a) and 0xFF
        //val B = ((b * 255) / a) and 0xFF
        //return RGBA(R, G, B, A)

        val A = a
        val Af = (255f / A.toFloat())
        val R = (r * Af).toInt()
        val G = (g * Af).toInt()
        val B = (b * Af).toInt()
        return RGBA(R, G, B, A)
    }

    val depremultipliedFast: RGBA get() {
        val A = a
        val A1 = A + 1
        val R = ((r shl 8) / A1) and 0xFF
        val G = ((g shl 8) / A1) and 0xFF
        val B = ((b shl 8) / A1) and 0xFF
        return RGBA(R, G, B, A)
    }

    val depremultipliedAccurate: RGBA get() {
        val alpha = ad
        return when (alpha) {
            0.0 -> Colors.TRANSPARENT_BLACK
            else -> {
                val ialpha = 1.0 / alpha
                RGBA((r * ialpha).toInt(), (g * ialpha).toInt(), (b * ialpha).toInt(), a)
            }
        }
    }

    val hexString: String get() = this.asNonPremultiplied().hexString
    val htmlColor: String get() = this.asNonPremultiplied().htmlColor
    val htmlStringSimple: String get() = this.asNonPremultiplied().htmlStringSimple

    override fun toString(): String = hexString
    fun scaled256(s: Int): RGBAPremultiplied {
        val rb = (((value and 0x00FF00FF) * s) ushr 8) and 0x00FF00FF
        val g = (((value and 0x0000FF00) * s) ushr 8) and 0x0000FF00
        val a = (((value ushr 24) * s) ushr 8) shl 24
        return RGBAPremultiplied(rb or g or a)
    }

    fun scaled(alpha: Double): RGBAPremultiplied = scaled256((alpha.clamp01() * 256).toInt())
    fun scaled(alpha: Float): RGBAPremultiplied = scaled256((alpha.clamp01() * 256).toInt())

    companion object {
        private const val RB_MASK: Int = 0x00FF00FF
        private const val GA_MASK: Int = -16711936 // 0xFF00FF00

        operator fun invoke(rgba: RGBA): RGBAPremultiplied = rgba.premultiplied

        fun blendAlpha(dst: RGBAPremultiplied, src: RGBAPremultiplied): RGBAPremultiplied =
            RGBAPremultiplied(sumPacked4MulR(src.value, dst.value, 256 - src.a))

        //fun mix(c1: RGBAPremultiplied, c2: RGBAPremultiplied): RGBAPremultiplied =
        //    RGBAPremultiplied(c1.r + c2.r, c1.g + c2.g, c1.b + c2.b, c1.a + c2.a)

        fun blend(c1: RGBAPremultiplied, c2: RGBAPremultiplied): RGBAPremultiplied {
            val RB = (((c1.value and 0xFF00FF) + (c2.value and 0xFF00FF)) ushr 1) and 0xFF00FF
            val G = (((c1.value and 0x00FF00) + (c2.value and 0x00FF00)) ushr 1) and 0x00FF00
            val A = (((c1.value ushr 24) + (c2.value ushr 24)) ushr 1) and 0xFF
            return RGBAPremultiplied((A shl 24) or RB or G)
        }

        fun blend(c1: RGBAPremultiplied, c2: RGBAPremultiplied, c3: RGBAPremultiplied, c4: RGBAPremultiplied): RGBAPremultiplied {
            val RB = (((c1.value and 0xFF00FF) + (c2.value and 0xFF00FF) + (c3.value and 0xFF00FF) + (c4.value and 0xFF00FF)) ushr 2) and 0xFF00FF
            val G = (((c1.value and 0x00FF00) + (c2.value and 0x00FF00) + (c3.value and 0x00FF00) + (c4.value and 0x00FF00)) ushr 2) and 0x00FF00
            val A = (((c1.value ushr 24) + (c2.value ushr 24) + (c3.value ushr 24) + (c4.value ushr 24)) ushr 2) and 0xFF
            return RGBAPremultiplied((A shl 24) or RB or G)
        }
    }
}

fun RGBA.asPremultiplied() = RGBAPremultiplied(value)
fun RGBAPremultiplied.asNonPremultiplied() = RGBA(value)

fun RgbaArray.asPremultiplied() = RgbaPremultipliedArray(ints)
fun RgbaPremultipliedArray.asNonPremultiplied() = RgbaArray(ints)

inline class RgbaPremultipliedArray(val ints: IntArray) {
    companion object {
        operator fun invoke(colors: Array<RGBAPremultiplied>): RgbaPremultipliedArray = RgbaPremultipliedArray(colors.map { it.value }.toIntArray())
        operator fun invoke(size: Int): RgbaPremultipliedArray = RgbaPremultipliedArray(IntArray(size))
        operator fun invoke(size: Int, callback: (index: Int) -> RGBAPremultiplied): RgbaPremultipliedArray = RgbaPremultipliedArray(IntArray(size)).apply { for (n in 0 until size) this[n] = callback(n) }
    }

    val size: Int get() = ints.size
    operator fun get(index: Int): RGBAPremultiplied = RGBAPremultiplied(ints[index])
    operator fun set(index: Int, color: RGBAPremultiplied) { ints[index] = color.value }

    fun fill(value: RGBAPremultiplied, start: Int = 0, end: Int = this.size): Unit = ints.fill(value.value, start, end)

    fun premultiply(start: Int = 0, end: Int = size): RgbaArray {
        for (n in start until end) this[n] = this[n].asNonPremultiplied().premultiplied
        return this.asNonPremultiplied()
    }

    override fun toString(): String = "RgbaPremultipliedArray($size)"
}

// @TODO: Critical performance use SIMD if possible
fun scale(color: RgbaPremultipliedArray, colorOffset: Int, alpha: FloatArray, alphaOffset: Int, count: Int) {
    for (n in 0 until count) {
        val a = alpha[alphaOffset + n].clamp01()
        if (a == 1f) continue
        color[colorOffset + n] = color[colorOffset + n].scaled(a)
    }
}

// @TODO: Critical performance use SIMD if possible
fun mix(dst: RgbaArray, dstX: Int, src: RgbaPremultipliedArray, srcX: Int, count: Int) {
    //println("-----------------------")
    for (n in 0 until count) {
        dst[dstX + n] = RGBAPremultiplied.blendAlpha(dst[dstX + n].premultiplied, src[srcX + n]).depremultiplied
    }
}

// @TODO: Critical performance use SIMD if possible
fun mix(dst: RgbaPremultipliedArray, dstX: Int, src: RgbaPremultipliedArray, srcX: Int, count: Int) = mix(dst, dstX, dst, dstX, src, srcX, count)
fun mix(tgt: RgbaPremultipliedArray, tgtX: Int, dst: RgbaPremultipliedArray, dstX: Int, src: RgbaPremultipliedArray, srcX: Int, count: Int) {
    for (n in 0 until count) tgt[tgtX + n] = RGBAPremultiplied.blendAlpha(dst[dstX + n], src[srcX + n])
}

fun premultiply(src: RgbaArray, srcN: Int, dst: RgbaPremultipliedArray, dstN: Int, count: Int) {
    for (n in 0 until count) dst[dstN + n] = src[srcN + n].premultiplied
}

fun depremultiply(src: RgbaPremultipliedArray, srcN: Int, dst: RgbaArray, dstN: Int, count: Int) {
    for (n in 0 until count) dst[dstN + n] = src[srcN + n].depremultiplied
}

//infix fun RGBA.mix(dst: RGBA): RGBA = RGBA.mix(this, dst)
infix fun RGBAPremultiplied.mix(src: RGBAPremultiplied): RGBAPremultiplied {
    val dst = this
    val srcAf = src.af
    val oneMSrcAf = (1f - srcAf)
    val outA = (src.a + (dst.a * oneMSrcAf)).toInt()
    val outR = (src.r + (dst.r * oneMSrcAf)).toInt()
    val outG = (src.g + (dst.g * oneMSrcAf)).toInt()
    val outB = (src.b + (dst.b * oneMSrcAf)).toInt()
    return RGBAPremultiplied(outR, outG, outB, outA)
    //val A = (src.a + (dst.a * oneMSrcAf).toInt()).clamp0_255()
    //val RB = ((src.value and 0xFF00FF) + ((dst.value and 0xFF00FF) * oneMSrcAf).toInt()) and 0xFF00FF
    //val G = ((src.value and 0x00FF00) + ((dst.value and 0x00FF00) * oneMSrcAf).toInt()) and 0x00FF00
    //return RGBAPremultiplied(RB or G, A)
}

inline class RgbaArray(val ints: IntArray) : List<RGBA> {
    companion object {
        operator fun invoke(colors: Array<RGBA>): RgbaArray = RgbaArray(colors.map { it.value }.toIntArray())
        operator fun invoke(size: Int): RgbaArray = RgbaArray(IntArray(size))
        inline operator fun invoke(size: Int, callback: (index: Int) -> RGBA): RgbaArray = RgbaArray(IntArray(size)).apply { for (n in 0 until size) this[n] = callback(n) }
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<RGBA> = GenericSubList(this, fromIndex, toIndex)
	override fun contains(element: RGBA): Boolean = ints.contains(element.value)
	override fun containsAll(elements: Collection<RGBA>): Boolean = elements.all { contains(it) }
	override fun indexOf(element: RGBA): Int = ints.indexOf(element.value)
	override fun lastIndexOf(element: RGBA): Int = ints.lastIndexOf(element.value)
	override fun isEmpty(): Boolean = ints.isEmpty()
	override fun iterator(): Iterator<RGBA> = listIterator(0)
	override fun listIterator(): ListIterator<RGBA> = listIterator(0)
	override fun listIterator(index: Int): ListIterator<RGBA> = GenericListIterator(this, index)

	override val size get() = ints.size
	override operator fun get(index: Int): RGBA = RGBA(ints[index])
	operator fun set(index: Int, color: RGBA) { ints[index] = color.value }
	fun fill(value: RGBA, start: Int = 0, end: Int = this.size): Unit = ints.fill(value.value, start, end)

    fun depremultiply(start: Int = 0, end: Int = size): RgbaPremultipliedArray {
        for (n in start until end) this[n] = this[n].asPremultiplied().depremultiplied
        return this.asPremultiplied()
    }

    override fun toString(): String = "RgbaArray($size)"
}

fun RGBA.mix(other: RGBA, ratio: Double) = RGBA.mixRgba(this, other, ratio)

fun List<RGBA>.toRgbaArray(): RgbaArray = RgbaArray(IntArray(this.size) { this@toRgbaArray[it].value })

fun arraycopy(src: RgbaArray, srcPos: Int, dst: RgbaArray, dstPos: Int, size: Int): Unit = arraycopy(src.ints, srcPos, dst.ints, dstPos, size)

fun Array<RGBA>.toRgbaArray() = RgbaArray(this.size) { this@toRgbaArray[it] }
