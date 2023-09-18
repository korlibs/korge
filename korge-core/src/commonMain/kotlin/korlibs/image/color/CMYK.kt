package korlibs.image.color

import korlibs.math.clampUByte
import korlibs.memory.extract8
import kotlin.math.max

// https://en.wikipedia.org/wiki/CMYK_color_model
inline class CMYK(val value: Int) {
    val c: Int get() = value.extract8(0)
    val m: Int get() = value.extract8(8)
    val y: Int get() = value.extract8(16)
    val k: Int get() = value.extract8(24)

    val cf: Float get() = c.toFloat() / 255f
    val mf: Float get() = m.toFloat() / 255f
    val yf: Float get() = y.toFloat() / 255f
    val kf: Float get() = k.toFloat() / 255f

    val r: Int get() = 255 - (c * (1 - k / 255) + k).clampUByte()
    val g: Int get() = 255 - (m * (1 - k / 255) + k).clampUByte()
    val b: Int get() = 255 - (y * (1 - k / 255) + k).clampUByte()
    val a: Int get() = 255

    fun toRGBA() = RGBA(r, g, b, a)

    companion object : ColorFormat32() {
        operator fun invoke(c: Int, m: Int, y: Int, k: Int): CMYK = CMYK(RGBA.pack(c, m, y, k))
        fun float(c: Float, m: Float, y: Float, k: Float): CMYK = CMYK(RGBA.float(c, m, y, k).value)

        operator fun invoke(rgba: RGBA): CMYK {
            val r0 = rgba.rf
            val g0 = rgba.gf
            val b0 = rgba.bf
            val k = 1f - max(max(r0, g0), b0)
            val ik = 1f / (1 - k)
            val c = (1f - r0 - k) * ik
            val m = (1f - g0 - k) * ik
            val y = (1f - b0 - k) * ik
            return float(c, m, y, k)
        }

        override fun getR(v: Int): Int = CMYK(v).c
        override fun getG(v: Int): Int = CMYK(v).m
        override fun getB(v: Int): Int = CMYK(v).y
        override fun getA(v: Int): Int = CMYK(v).k
        override fun pack(r: Int, g: Int, b: Int, a: Int): Int = RGBA.pack(r, g, b, a)
    }
}

fun RGBA.toCMYK() = CMYK(this)
