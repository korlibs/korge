package korlibs.image.bitmap.effect

import korlibs.image.bitmap.Bitmap32
import korlibs.image.bitmap.BitmapChannel
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import kotlin.math.absoluteValue
import kotlin.math.max

fun Bitmap32.dropShadow(x: Int, y: Int, r: Int, color: RGBA = Colors.BLACK): Bitmap32 {
    val pcolor = color.premultiplied
    val add = max(x.absoluteValue, y.absoluteValue) + r
    val out = Bitmap32(width + add * 2, height + add * 2, premultiplied = true)
    val shadow = this.extractChannel(BitmapChannel.ALPHA).blur(r).also { shadow ->
        shadow.premultiplied = true
        for (n in 0 until 0x100) {
            shadow.palette[n] = RGBA(
                (pcolor.r * n) / 255,
                (pcolor.g * n) / 255,
                (pcolor.b * n) / 255,
                (pcolor.a * n) / 255
            )
        }
    }.toBMP32()
    out.draw(shadow, add + x - r, add + y - r)
    out.draw(this, add, add)
    return out
}

fun Bitmap32.dropShadowInplace(x: Int, y: Int, r: Int, color: RGBA = Colors.BLACK) = this.apply {
    val copy = this.clone()
    val pcolor = color.premultiplied
    val shadow = this.extractChannel(BitmapChannel.ALPHA).blur(r).also { shadow ->
        shadow.premultiplied = true
        for (n in 0 until 0x100) {
            shadow.palette[n] = RGBA(
                (pcolor.r * n) / 255,
                (pcolor.g * n) / 255,
                (pcolor.b * n) / 255,
                (pcolor.a * n) / 255
            )
        }
    }.toBMP32()
    this.fill(Colors.TRANSPARENT)
    this.draw(shadow, x - r, y - r)
    this.draw(copy, 0, 0)
}
