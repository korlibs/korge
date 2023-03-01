package com.soywiz.korma.geom

import kotlin.math.*

class ScaleMode(
    val name: String? = null,
    val transform: (c: Int, iw: Double, ih: Double, cw: Double, ch: Double) -> Double
) {
    override fun toString(): String = "ScaleMode($name)"

    fun transformW(iw: Double, ih: Double, cw: Double, ch: Double) = transform(0, iw, ih, cw, ch)
    fun transformH(iw: Double, ih: Double, cw: Double, ch: Double) = transform(1, iw, ih, cw, ch)
    fun transform(iw: Double, ih: Double, cw: Double, ch: Double, target: MSize = MSize()) = target.setTo(
        transformW(iw, ih, cw, ch),
        transformH(iw, ih, cw, ch)
    )

    fun transformW(item: MSize, container: MSize) = transformW(item.width, item.height, container.width, container.height)
    fun transformH(item: MSize, container: MSize) = transformH(item.width, item.height, container.width, container.height)

    operator fun invoke(item: ISize, container: ISize, target: MSize = MSize()): MSize =
        transform(item.width, item.height, container.width, container.height, target)

    operator fun invoke(item: ISizeInt, container: ISizeInt, target: MSizeInt = MSizeInt()): MSizeInt = target.setTo(
        transformW(item.width.toDouble(), item.height.toDouble(), container.width.toDouble(), container.height.toDouble()).toInt(),
        transformH(item.width.toDouble(), item.height.toDouble(), container.width.toDouble(), container.height.toDouble()).toInt()
    )
    operator fun invoke(item: Size, container: Size): Size = Size(
        transformW(item.width.toDouble(), item.height.toDouble(), container.width.toDouble(), container.height.toDouble()),
        transformH(item.width.toDouble(), item.height.toDouble(), container.width.toDouble(), container.height.toDouble())
    )
    operator fun invoke(item: SizeInt, container: SizeInt): SizeInt = SizeInt(
        transformW(item.width.toDouble(), item.height.toDouble(), container.width.toDouble(), container.height.toDouble()).toInt(),
        transformH(item.width.toDouble(), item.height.toDouble(), container.width.toDouble(), container.height.toDouble()).toInt()
    )

    object Provider {
        @Suppress("unused") val LIST = listOf(COVER, SHOW_ALL, EXACT, NO_SCALE)
    }

    companion object {
        val COVER: ScaleMode = ScaleMode("COVER") { c, iw, ih, cw, ch ->
            val s0 = cw / iw
            val s1 = ch / ih
            val s = max(s0, s1)
            if (c == 0) iw * s else ih * s
        }

        val SHOW_ALL: ScaleMode = ScaleMode("SHOW_ALL") { c, iw, ih, cw, ch ->
            val s0 = cw / iw
            val s1 = ch / ih
            val s = min(s0, s1)
            if (c == 0) iw * s else ih * s
        }

        val FIT: ScaleMode get() = SHOW_ALL

        val FILL: ScaleMode get() = EXACT

        val EXACT: ScaleMode = ScaleMode("EXACT") { c, iw, ih, cw, ch ->
            if (c == 0) cw else ch
        }

        val NO_SCALE: ScaleMode = ScaleMode("NO_SCALE") { c, iw, ih, cw, ch ->
            if (c == 0) iw else ih
        }
    }
}

fun MRectangle.applyScaleMode(
    container: MRectangle, mode: ScaleMode, anchor: Anchor, out: MRectangle = MRectangle()
): MRectangle = this.size.applyScaleMode(container, mode, anchor, out)
fun MSize.applyScaleMode(container: MRectangle, mode: ScaleMode, anchor: Anchor, out: MRectangle = MRectangle(), tempSize: MSize = MSize()): MRectangle {
    val outSize = this.applyScaleMode(container.size, mode, tempSize)
    out.setToAnchoredRectangle(MRectangle(0.0, 0.0, outSize.width, outSize.height), anchor, container)
    return out
}

fun MSize.applyScaleMode(container: MSize, mode: ScaleMode, out: MSize = MSize(0, 0)): MSize =
    mode(this, container, out)
fun MSize.fitTo(container: MSize, out: MSize = MSize(0, 0)): MSize =
    applyScaleMode(container, ScaleMode.SHOW_ALL, out)


fun MSizeInt.applyScaleMode(container: MRectangleInt, mode: ScaleMode, anchor: Anchor, out: MRectangleInt = MRectangleInt(), tempSize: MSizeInt = MSizeInt()): MRectangleInt =
    this.asDouble().applyScaleMode(container.float, mode, anchor, out.float, tempSize.asDouble()).int
fun MSizeInt.applyScaleMode(container: MSizeInt, mode: ScaleMode, out: MSizeInt = MSizeInt(0, 0)): MSizeInt =
    mode(this, container, out)
fun MSizeInt.fitTo(container: MSizeInt, out: MSizeInt = MSizeInt(0, 0)): MSizeInt =
    applyScaleMode(container, ScaleMode.SHOW_ALL, out)


fun SizeInt.applyScaleMode(container: RectangleInt, mode: ScaleMode, anchor: Anchor): RectangleInt =
    this.toFloat().applyScaleMode(container.toFloat(), mode, anchor).toInt()
fun SizeInt.applyScaleMode(container: SizeInt, mode: ScaleMode): SizeInt = mode(this, container)
fun SizeInt.fitTo(container: SizeInt): SizeInt = applyScaleMode(container, ScaleMode.SHOW_ALL)

fun Size.applyScaleMode(container: Rectangle, mode: ScaleMode, anchor: Anchor): Rectangle {
    val outSize = this.applyScaleMode(container.size, mode)
    return Rectangle(
        (container.x + anchor.doubleX * (container.width - outSize.width)).toFloat(),
        (container.y + anchor.doubleY * (container.height - outSize.height)).toFloat(),
        outSize.width,
        outSize.height
    )
}
fun Size.applyScaleMode(container: Size, mode: ScaleMode): Size = mode(this, container)
fun Size.fitTo(container: Size): Size = applyScaleMode(container, ScaleMode.SHOW_ALL)
