package com.soywiz.korim.color

import com.soywiz.kmem.extract8
import com.soywiz.korim.internal.clamp0_255
import com.soywiz.korim.internal.packIntClamped

// https://en.wikipedia.org/wiki/YCbCr
inline class YCbCr(val value: Int) {
    constructor(y: Int, cb: Int, cr: Int, a: Int = 0xFF) : this(packIntClamped(y, cb, cr, a))

    val y: Int get() = value.extract8(0) // Luma
    val cb: Int get() = value.extract8(8) // Chrominance1
    val cr: Int get() = value.extract8(16) // Chrominance2
    val a: Int get() = value.extract8(24) // Alpha

    companion object : ColorFormat32() {
        fun getY(v: Int): Int = YCbCr(v).y
        fun getCb(v: Int): Int = YCbCr(v).cb
        fun getCr(v: Int): Int = YCbCr(v).cr

        override fun getR(v: Int): Int = getY(v)
        override fun getG(v: Int): Int = getCb(v)
        override fun getB(v: Int): Int = getCr(v)
        override fun getA(v: Int): Int = v.extract8(24)

        override fun pack(r: Int, g: Int, b: Int, a: Int): Int = RGBA.pack(r, g, b, a)

        fun getY(r: Int, g: Int, b: Int): Int = ((0 + (0.299 * r) + (0.587 * g) + (0.114 * b)).toInt()).clamp0_255()
        fun getCb(r: Int, g: Int, b: Int): Int = ((128 - (0.168736 * r) - (0.331264 * g) + (0.5 * b)).toInt()).clamp0_255()
        fun getCr(r: Int, g: Int, b: Int): Int = ((128 + (0.5 * r) - (0.418688 * g) - (0.081312 * b)).toInt()).clamp0_255()
        fun getR(y: Int, cb: Int, cr: Int): Int = ((y + 1.402 * (cr - 128)).toInt()).clamp0_255()
        fun getG(y: Int, cb: Int, cr: Int): Int = ((y - 0.34414 * (cb - 128) - 0.71414 * (cr - 128)).toInt()).clamp0_255()
        fun getB(y: Int, cb: Int, cr: Int): Int = ((y + 1.772 * (cb - 128)).toInt()).clamp0_255()
    }
}

inline class YCbCrArray(val ints: IntArray) {
    val size get() = ints.size
    operator fun get(index: Int): YCbCr = YCbCr(ints[index])
    operator fun set(index: Int, color: YCbCr) { ints[index] = color.value }
}

fun RGBA.toYCbCr(): YCbCr = YCbCr(YCbCr.getY(r, g, b), YCbCr.getCb(r, g, b), YCbCr.getCr(r, g, b), a)
fun YCbCr.toRGBA(): RGBA = RGBA(YCbCr.getR(y, cb, cr), YCbCr.getG(y, cb, cr), YCbCr.getB(y, cb, cr), a)
