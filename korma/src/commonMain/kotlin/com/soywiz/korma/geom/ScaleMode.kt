package com.soywiz.korma.geom

import com.soywiz.korma.internal.*
import kotlin.math.*
import kotlin.native.concurrent.ThreadLocal

class ScaleMode(
    val transform: (c: Int, iw: Double, ih: Double, cw: Double, ch: Double) -> Double
) {
    fun transformW(iw: Double, ih: Double, cw: Double, ch: Double) = transform(0, iw, ih, cw, ch)
    fun transformH(iw: Double, ih: Double, cw: Double, ch: Double) = transform(1, iw, ih, cw, ch)
    fun transform(iw: Double, ih: Double, cw: Double, ch: Double, target: Size = Size()) = target.setTo(
        transformW(iw, ih, cw, ch),
        transformH(iw, ih, cw, ch)
    )

    fun transformW(item: Size, container: Size) = transformW(item.width, item.height, container.width, container.height)
    fun transformH(item: Size, container: Size) = transformH(item.width, item.height, container.width, container.height)

    operator fun invoke(item: Size, container: Size, target: Size = Size()): Size =
        transform(item.width, item.height, container.width, container.height, target)

    operator fun invoke(item: SizeInt, container: SizeInt, target: SizeInt = SizeInt()): SizeInt = target.setTo(
        transformW(item.width.toDouble(), item.height.toDouble(), container.width.toDouble(), container.height.toDouble()).toInt(),
        transformH(item.width.toDouble(), item.height.toDouble(), container.width.toDouble(), container.height.toDouble()).toInt()
    )

    companion object {
        val COVER = ScaleMode { c, iw, ih, cw, ch ->
            val s0 = cw / iw
            val s1 = ch / ih
            val s = max(s0, s1)
            if (c == 0) iw * s else ih * s
        }

        val SHOW_ALL = ScaleMode { c, iw, ih, cw, ch ->
            val s0 = cw / iw
            val s1 = ch / ih
            val s = min(s0, s1)
            if (c == 0) iw * s else ih * s
        }

        val FIT get() = SHOW_ALL

        val EXACT = ScaleMode { c, iw, ih, cw, ch ->
            if (c == 0) cw else ch
        }

        val NO_SCALE = ScaleMode { c, iw, ih, cw, ch ->
            if (c == 0) iw else ih
        }
    }
}

fun Rectangle.applyScaleMode(container: Rectangle, mode: ScaleMode, anchor: Anchor, out: Rectangle = Rectangle()): Rectangle = this.size.applyScaleMode(container, mode, anchor, out)

fun Size.applyScaleMode(container: Rectangle, mode: ScaleMode, anchor: Anchor, out: Rectangle = Rectangle(), tempSize: Size = Size()): Rectangle {
    val outSize = this.applyScaleMode(container.size, mode, tempSize)
    out.setToAnchoredRectangle(Rectangle(0.0, 0.0, outSize.width, outSize.height), anchor, container)
    return out
}

fun SizeInt.applyScaleMode(container: RectangleInt, mode: ScaleMode, anchor: Anchor, out: RectangleInt = RectangleInt(), tempSize: SizeInt = SizeInt()): RectangleInt =
    this.asDouble().applyScaleMode(container.asDouble(), mode, anchor, out.asDouble(), tempSize.asDouble()).asInt()

fun SizeInt.applyScaleMode(container: SizeInt, mode: ScaleMode, out: SizeInt = SizeInt(0, 0)): SizeInt =
    mode(this, container, out)
fun Size.applyScaleMode(container: Size, mode: ScaleMode, out: Size = Size(0, 0)): Size =
    mode(this, container, out)

fun SizeInt.fitTo(container: SizeInt, out: SizeInt = SizeInt(0, 0)): SizeInt =
    applyScaleMode(container, ScaleMode.SHOW_ALL, out)
fun Size.fitTo(container: Size, out: Size = Size(0, 0)): Size =
    applyScaleMode(container, ScaleMode.SHOW_ALL, out)
