package korlibs.image.color

import korlibs.math.clampUByte
import korlibs.memory.extract8
import korlibs.image.internal.packIntClamped

// https://en.wikipedia.org/wiki/YUV
inline class YUVA(val value: Int) {
    constructor(y: Int, u: Int, v: Int, a: Int) : this(packIntClamped(y, u, v, a))

    val y get() = value.extract8(0)
    val u get() = value.extract8(8)
    val v get() = value.extract8(16)
    val a get() = value.extract8(24)

    companion object : ColorFormat32() {
        fun getY(v: Int): Int = v.extract8(0) // Luma
        fun getU(v: Int): Int = v.extract8(8) // Chrominance1
        fun getV(v: Int): Int = v.extract8(16) // Chrominance2

        override fun getA(v: Int): Int = v.extract8(24)
        override fun getR(v: Int): Int = getR(getY(v), getU(v), getV(v))
        override fun getG(v: Int): Int = getG(getY(v), getU(v), getV(v))
        override fun getB(v: Int): Int = getB(getY(v), getU(v), getV(v))

        override fun pack(r: Int, g: Int, b: Int, a: Int): Int = RGBA(r, g, b, a).toYUVA().value

        //val R = (1.164f * (Y - 16) + 1.596f * (V - 128)).toInt()
        //val G = (1.164f * (Y - 16) - 0.813f * (V - 128) - 0.391f * (U - 128)).toInt()
        //val B = (1.164f * (Y - 16) + 2.018f * (U - 128)).toInt()

        fun getY(r: Int, g: Int, b: Int): Int = (((0.299 * r) + (0.587 * g) + (0.114 * b)).toInt()).clampUByte()
        fun getU(r: Int, g: Int, b: Int): Int = ((0.492 * (b * getY(r, g, b))).toInt()).clampUByte()
        fun getV(r: Int, g: Int, b: Int): Int = ((0.877 * (r * getY(r, g, b))).toInt()).clampUByte()

        fun getR(y: Int, u: Int, v: Int): Int = ((y + 1.14 * v).toInt()).clampUByte()
        fun getG(y: Int, u: Int, v: Int): Int = ((y - 0.395 * u - 0.581 * v).toInt()).clampUByte()
        fun getB(y: Int, u: Int, v: Int): Int = ((y + 2.033 * u).toInt()).clampUByte()

        fun YUVtoRGB(out: RgbaArray, outPos: Int, inY: ByteArray, inU: ByteArray, inV: ByteArray, inPos: Int, count: Int) {
            for (n in 0 until count) {
                out[outPos + n] = YUVA(
                    (inY[inPos + n].toInt() and 255),
                    (inU[inPos + n].toInt() and 255) - 128,
                    (inV[inPos + n].toInt() and 255) - 128,
                    0xFF
                ).toRGBA()
            }
        }
    }
}

inline class YuvaArray(val ints: IntArray) {
    val size get() = ints.size
    operator fun get(index: Int): YUVA = YUVA(ints[index])
    operator fun set(index: Int, color: YUVA) { ints[index] = color.value }
}

fun RGBA.toYUVA(): YUVA = YUVA(YUVA.getY(r, g, b), YUVA.getU(r, g, b), YUVA.getV(r, g, b), a)
fun YUVA.toRGBA(): RGBA = RGBA(YUVA.getR(y, u, v), YUVA.getG(y, u, v), YUVA.getB(y, u, v), a)
