package com.soywiz.korim.bitmap

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korio.resources.*
import com.soywiz.korma.geom.*

abstract class BmpSlice(
    val bmpBase: Bitmap,
    val bounds: RectangleInt,
    val name: String? = null,
    val rotated: Boolean = false
) : Extra, Resourceable<BmpSlice> {
    override fun getOrNull() = this
    override suspend fun get() = this
    open val bmp: Bitmap = bmpBase
    val bmpWidth = bmpBase.width
    val bmpHeight = bmpBase.width

    private val tl = Point(left.toFloat() / bmpBase.width.toFloat(), top.toFloat() / bmpBase.height.toFloat())
    private val br = Point(right.toFloat() / bmpBase.width.toFloat(), bottom.toFloat() / bmpBase.height.toFloat())
    private val tr = Point(br.x, tl.y)
    private val bl = Point(tl.x, br.y)

    private val points = arrayOf(tl, tr, br, bl)
    private val offset = if (rotated) 1 else 0

    private val p0 = points.getCyclic(offset + 0)
    private val p1 = points.getCyclic(offset + 1)
    private val p2 = points.getCyclic(offset + 2)
    private val p3 = points.getCyclic(offset + 3)

    val left: Int get() = bounds.left
    val top: Int get() = bounds.top
    val width: Int get() = bounds.width
    val height: Int get() = bounds.height
    val right get() = bounds.right
    val bottom get() = bounds.bottom

	var parent: Any? = null

    val tl_x = p0.x.toFloat()
    val tl_y = p0.y.toFloat()

    val tr_x = p1.x.toFloat()
    val tr_y = p1.y.toFloat()

    val br_x = p2.x.toFloat()
    val br_y = p2.y.toFloat()

    val bl_x = p3.x.toFloat()
    val bl_y = p3.y.toFloat()

    val rotatedAngle: Int = 0
}

val BmpSlice.nameSure: String get() = name ?: "unknown"
fun <T : Bitmap> BmpSlice.asBitmapSlice(): BitmapSlice<T> = this as BitmapSlice<T>

fun BmpSlice.getIntBounds(out: RectangleInt = RectangleInt()) = out.setTo(left, top, width, height)

fun BmpSlice.extract(): Bitmap = bmpBase.extract(left, top, width, height)

class BitmapSlice<out T : Bitmap>(override val bmp: T, bounds: RectangleInt, name: String? = null, rotated: Boolean = false) : BmpSlice(bmp, bounds, name, rotated), Extra by Extra.Mixin() {
	val premultiplied get() = bmp.premultiplied

	fun extract(): T = bmp.extract(bounds.x, bounds.y, bounds.width, bounds.height)

	fun sliceWithBounds(left: Int, top: Int, right: Int, bottom: Int, name: String? = null): BitmapSlice<T> =
		BitmapSlice(bmp, createRectangleInt(bounds.left, bounds.top, bounds.right, bounds.bottom, left, top, right, bottom), name)

	fun sliceWithSize(x: Int, y: Int, width: Int, height: Int, name: String? = null): BitmapSlice<T> = sliceWithBounds(x, y, x + width, y + height, name)
	fun slice(rect: RectangleInt, name: String? = null): BitmapSlice<T> = sliceWithBounds(rect.left, rect.top, rect.right, rect.bottom, name)
	fun slice(rect: Rectangle, name: String? = null): BitmapSlice<T> = slice(rect.toInt(), name)

    fun split(width: Int, height: Int): List<BitmapSlice<T>> {
        val self = this
        val nheight = self.height / height
        val nwidth = self.width / width
        return arrayListOf<BitmapSlice<T>>().apply {
            for (y in 0 until nheight) {
                for (x in 0 until nwidth) {
                    add(self.sliceWithSize(x * width, y * height, width, height))
                }
            }
        }
    }

    fun withName(name: String? = null)  = BitmapSlice<T>(bmp, bounds, name, rotated)

	override fun toString(): String = "BitmapSlice($name:${SizeInt(bounds.width, bounds.height)})"
}

// http://pixijs.download/dev/docs/PIXI.Texture.html#Texture
fun BitmapSliceCompat(
	bmp: Bitmap,
	frame: Rectangle,
	orig: Rectangle,
	trim: Rectangle,
	rotated: Boolean,
	name: String = "unknown"
) = BitmapSlice(bmp, frame.toInt(), name = name, rotated = rotated)

fun <T : Bitmap> T.slice(bounds: RectangleInt = RectangleInt(0, 0, width, height), name: String? = null): BitmapSlice<T> = BitmapSlice<T>(this, bounds, name)
fun <T : Bitmap> T.sliceWithBounds(left: Int, top: Int, right: Int, bottom: Int, name: String? = null): BitmapSlice<T> = slice(createRectangleInt(0, 0, this.width, this.height, left, top, right, bottom), name)
fun <T : Bitmap> T.sliceWithSize(x: Int, y: Int, width: Int, height: Int, name: String? = null): BitmapSlice<T> = sliceWithBounds(x, y, x + width, y + height, name)

private fun createRectangleInt(
    bleft: Int, btop: Int, bright: Int, bbottom: Int,
    left: Int, top: Int, right: Int, bottom: Int,
    allowInvalidBounds: Boolean = false
): RectangleInt {
    return RectangleInt.fromBounds(
        (bleft + left).clamp(bleft, bright),
        (btop + top).clamp(btop, bbottom),
        (bleft + right).clamp(if (allowInvalidBounds) bleft else bleft + left, bright),
        (btop + bottom).clamp(if (allowInvalidBounds) btop else btop + top, bbottom)
    )
}
