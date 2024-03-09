package korlibs.image.vector

import korlibs.math.clampUByte
import korlibs.image.color.RgbaPremultipliedArray
import korlibs.image.color.mix

// https://drafts.fxtf.org/compositing-1/
interface CompositeOperation {
    companion object {
        val UNIMPLEMENTED = CompositeOperation { dst, dstN, src, srcN, count -> mix(dst, dstN, src, srcN, count) }
        operator fun invoke(func: (dst: RgbaPremultipliedArray, dstN: Int, src: RgbaPremultipliedArray, srcN: Int, count: Int) -> Unit): CompositeOperation =
            object : CompositeOperation {
                override fun blend(dst: RgbaPremultipliedArray, dstN: Int, src: RgbaPremultipliedArray, srcN: Int, count: Int) =
                    func(dst, dstN, src, srcN, count)
            }
    }

    fun blend(dst: RgbaPremultipliedArray, dstN: Int, src: RgbaPremultipliedArray, srcN: Int, count: Int)
}

val CompositeOperation.Companion.DEFAULT get() = CompositeMode.DEFAULT
val CompositeOperation.Companion.CLEAR get() = CompositeMode.CLEAR
val CompositeOperation.Companion.COPY get() = CompositeMode.COPY
val CompositeOperation.Companion.SOURCE_OVER get() = CompositeMode.SOURCE_OVER
val CompositeOperation.Companion.DESTINATION_OVER get() = CompositeMode.DESTINATION_OVER
val CompositeOperation.Companion.SOURCE_IN get() = CompositeMode.SOURCE_IN
val CompositeOperation.Companion.DESTINATION_IN get() = CompositeMode.DESTINATION_IN
val CompositeOperation.Companion.SOURCE_OUT get() = CompositeMode.SOURCE_OUT
val CompositeOperation.Companion.DESTINATION_OUT get() = CompositeMode.DESTINATION_OUT
val CompositeOperation.Companion.SOURCE_ATOP get() = CompositeMode.SOURCE_ATOP
val CompositeOperation.Companion.DESTINATION_ATOP get() = CompositeMode.DESTINATION_ATOP
val CompositeOperation.Companion.XOR get() = CompositeMode.XOR
val CompositeOperation.Companion.LIGHTER get() = CompositeMode.LIGHTER

val CompositeOperation.Companion.NORMAL get() = BlendMode.NORMAL
val CompositeOperation.Companion.MULTIPLY get() = BlendMode.MULTIPLY
val CompositeOperation.Companion.SCREEN get() = BlendMode.SCREEN
val CompositeOperation.Companion.OVERLAY get() = BlendMode.OVERLAY
val CompositeOperation.Companion.DARKEN get() = BlendMode.DARKEN
val CompositeOperation.Companion.LIGHTEN get() = BlendMode.LIGHTEN
val CompositeOperation.Companion.COLOR_DODGE get() = BlendMode.COLOR_DODGE
val CompositeOperation.Companion.COLOR_BURN get() = BlendMode.COLOR_BURN
val CompositeOperation.Companion.HARD_LIGHT get() = BlendMode.HARD_LIGHT
val CompositeOperation.Companion.SOFT_LIGHT get() = BlendMode.SOFT_LIGHT
val CompositeOperation.Companion.DIFFERENCE get() = BlendMode.DIFFERENCE
val CompositeOperation.Companion.EXCLUSION get() = BlendMode.EXCLUSION
val CompositeOperation.Companion.HUE get() = BlendMode.HUE
val CompositeOperation.Companion.SATURATION get() = BlendMode.SATURATION
val CompositeOperation.Companion.COLOR get() = BlendMode.COLOR
val CompositeOperation.Companion.LUMINOSITY get() = BlendMode.LUMINOSITY

// https://drafts.fxtf.org/compositing-1/
enum class CompositeMode(val op: CompositeOperation) : CompositeOperation by op {
    CLEAR(CompositeOperation { dst, dstN, src, srcN, count ->
        for (n in 0 until count) {
            val d = dst[dstN + n]
            val s = src[srcN + n]
            dst[dstN + n] = d.depremultiplied.withA((d.a - s.a).clampUByte()).premultiplied
        }
    }),
    COPY(CompositeOperation.UNIMPLEMENTED),
    SOURCE_OVER(CompositeOperation { dst, dstN, src, srcN, count ->
        mix(dst, dstN, dst, dstN, src, srcN, count)
    }),
    DESTINATION_OVER(CompositeOperation { dst, dstN, src, srcN, count ->
        mix(dst, dstN, src, srcN, dst, dstN, count)
    }),
    SOURCE_IN(CompositeOperation.UNIMPLEMENTED),
    DESTINATION_IN(CompositeOperation.UNIMPLEMENTED),
    SOURCE_OUT(CompositeOperation.UNIMPLEMENTED),
    DESTINATION_OUT(CompositeOperation.UNIMPLEMENTED),
    SOURCE_ATOP(CompositeOperation.UNIMPLEMENTED),
    DESTINATION_ATOP(CompositeOperation.UNIMPLEMENTED),
    XOR(CompositeOperation.UNIMPLEMENTED),
    LIGHTER(CompositeOperation.UNIMPLEMENTED);
    companion object {
        val DEFAULT get() = SOURCE_OVER
    }
}

// https://drafts.fxtf.org/compositing-1/
enum class BlendMode(val op: CompositeOperation) : CompositeOperation by op {
    NORMAL(CompositeOperation.UNIMPLEMENTED),
    MULTIPLY(CompositeOperation.UNIMPLEMENTED),
    SCREEN(CompositeOperation.UNIMPLEMENTED),
    OVERLAY(CompositeOperation.UNIMPLEMENTED),
    DARKEN(CompositeOperation.UNIMPLEMENTED),
    LIGHTEN(CompositeOperation.UNIMPLEMENTED),
    COLOR_DODGE(CompositeOperation.UNIMPLEMENTED),
    COLOR_BURN(CompositeOperation.UNIMPLEMENTED),
    HARD_LIGHT(CompositeOperation.UNIMPLEMENTED),
    SOFT_LIGHT(CompositeOperation.UNIMPLEMENTED),
    DIFFERENCE(CompositeOperation.UNIMPLEMENTED),
    EXCLUSION(CompositeOperation.UNIMPLEMENTED),
    HUE(CompositeOperation.UNIMPLEMENTED),
    SATURATION(CompositeOperation.UNIMPLEMENTED),
    COLOR(CompositeOperation.UNIMPLEMENTED),
    LUMINOSITY(CompositeOperation.UNIMPLEMENTED),
    ADDITION(CompositeOperation.UNIMPLEMENTED),
    SUBTRACT(CompositeOperation.UNIMPLEMENTED),
    DIVIDE(CompositeOperation.UNIMPLEMENTED),
}
