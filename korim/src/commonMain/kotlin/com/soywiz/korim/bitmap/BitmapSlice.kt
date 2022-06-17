package com.soywiz.korim.bitmap

import com.soywiz.kds.Extra
import com.soywiz.kmem.clamp
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.RgbaArray
import com.soywiz.korim.format.ImageOrientation
import com.soywiz.korim.format.withImageOrientation
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.resources.Resourceable
import com.soywiz.korma.geom.ISizeInt
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Matrix3D
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.PointInt
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.RectangleInt
import com.soywiz.korma.geom.SizeInt
import com.soywiz.korma.geom.setBoundsTo
import com.soywiz.korma.geom.setTo
import kotlin.math.min
import kotlin.math.roundToInt

interface BmpCoords {
    val tl_x: Float
    val tl_y: Float

    val tr_x: Float
    val tr_y: Float

    val br_x: Float
    val br_y: Float

    val bl_x: Float
    val bl_y: Float
}

interface BmpCoordsWithT<T : ISizeInt> : BmpCoords, Closeable, Resourceable<BmpCoordsWithT<T>> {
    override fun getOrNull(): BmpCoordsWithT<T>? = this
    override suspend fun get(): BmpCoordsWithT<T> = this
    val baseWidth: Int get() = base.width
    val baseHeight: Int get() = base.height
    val name: String? get() = null
    val base: T
    val left: Int get() = (tl_x * baseWidth).toInt()
    val top: Int get() = (tl_y * baseHeight).toInt()
    val width: Int get() = (Point.distance(tl_x * baseWidth, tl_y * baseHeight, tr_x * baseWidth, tr_y * baseHeight)).toInt()
    val height: Int get() = (Point.distance(tl_x * baseWidth, tl_y * baseHeight, bl_x * baseWidth, bl_y * baseHeight)).toInt()
    val virtFrame: RectangleInt? get() = null
    val frameOffsetX: Int get() = virtFrame?.x ?: 0
    val frameOffsetY: Int get() = virtFrame?.y ?: 0
    val frameWidth: Int get() = virtFrame?.width ?: width
    val frameHeight: Int get() = virtFrame?.height ?: height
    val area: Int get() = width * height
    val isRotatedInBaseDeg90 get() = (width > 1 && tl_x == tr_x) || (height > 1 && tl_y == bl_y)
    override fun close() = Unit
    val sizeString: String get() = "${width}x${height}"

    fun subCoords(bounds: RectangleInt, imageOrientation: ImageOrientation): BmpCoordsWithT<T> {
        // Assure bounds are inside this slide
        if (bounds.x < 0) {
            bounds.width += bounds.x
            bounds.x = 0
        }
        if (bounds.y < 0) {
            bounds.height += bounds.y
            bounds.y = 0
        }
        val ioRotated = imageOrientation.isRotatedDeg90CwOrCcw
        bounds.width = if (ioRotated) bounds.width.clamp(0, height - bounds.y) else bounds.width.clamp(0, width - bounds.x)
        bounds.height = if (ioRotated) bounds.height.clamp(0, width - bounds.x) else bounds.height.clamp(0, height - bounds.y)

        // Calculate bmpCoords based on parentCoords
        val dx = br_x - tl_x
        val dy = br_y - tl_y
        val x = bounds.x.toFloat()
        val y = bounds.y.toFloat()
        val w = bounds.width.toFloat()
        val h = bounds.height.toFloat()
        val bw = width.toFloat()
        val bh = height.toFloat()

        val flipDim = isRotatedInBaseDeg90 xor ioRotated
        val xDim = if (flipDim) h else w
        val yDim = if (flipDim) w else h
        val _bw = if (isRotatedInBaseDeg90) bh else bw
        val _bh = if (isRotatedInBaseDeg90) bw else bh
        val _x = if (isRotatedInBaseDeg90) y else x
        val _y = if (isRotatedInBaseDeg90) x else y

        val tlX = tl_x + _x / _bw * dx
        val tlY = tl_y + _y / _bh * dy
        val brX = tlX + xDim / _bw * dx
        val brY = tlY + yDim / _bh * dy
        val trX = if (isRotatedInBaseDeg90) tlX else brX
        val trY = if (isRotatedInBaseDeg90) brY else tlY
        val blX = if (isRotatedInBaseDeg90) brX else tlX
        val blY = if (isRotatedInBaseDeg90) tlY else brY

        return copy(base, tlX, tlY, trX, trY, brX, brY, blX, blY)
            .withImageOrientation(imageOrientation)
    }
}

// @TODO: Fix & enable to support slicing transformed textures

/*
private fun transformInRange(ratio: Float, a: Float, b: Float) = (b - a) * ratio + a

private fun <T : ISizeInt> BmpCoordsWithT<T>.sliceRatio(ratioLeft: Float, ratioRight: Float, ratioTop: Float, ratioBottom: Float, name: String? = null): BmpCoordsWithT<T> {
    //println("($ratioLeft, $ratioRight), ($ratioTop, $ratioBottom)")
    return BmpCoordsWithInstance(
        base,
        transformInRange(ratioLeft, tl_x, tr_x), transformInRange(ratioTop, tl_y, bl_y),
        transformInRange(ratioRight, tl_x, tr_x), transformInRange(ratioTop, tr_y, br_y),

        transformInRange(ratioRight, bl_x, br_x), transformInRange(ratioBottom, tr_y, br_y),
        transformInRange(ratioLeft, bl_x, br_x), transformInRange(ratioBottom, tl_y, bl_y),
    )
}

fun <T : ISizeInt> BmpCoordsWithT<T>.slice(bounds: RectangleInt = RectangleInt(0, 0, width, height), name: String? = null): BmpCoordsWithT<T> {
    return sliceRatio(
        bounds.left.toFloat() / base.width,
        bounds.right.toFloat() / base.width,
        bounds.top.toFloat() / base.height,
        bounds.bottom.toFloat() / base.height,
        name
    )
}
fun <T : ISizeInt> BmpCoordsWithT<T>.sliceWithBounds(left: Int, top: Int, right: Int, bottom: Int, name: String? = null): BmpCoordsWithT<T> = slice(createRectangleInt(0, 0, this.width, this.height, left, top, right, bottom), name)
fun <T : ISizeInt> BmpCoordsWithT<T>.sliceWithSize(x: Int, y: Int, width: Int, height: Int, name: String? = null): BmpCoordsWithT<T> = sliceWithBounds(x, y, x + width, y + height, name)
*/

typealias BitmapCoords = BmpCoordsWithT<Bitmap>
typealias BaseBmpSlice = BmpCoordsWithT<Bitmap>
val BaseBmpSlice.bmpBase get() = base

data class BmpCoordsWithInstance<T : ISizeInt>(
    override val base: T,
    override val tl_x: Float = 0f, override val tl_y: Float = 0f,
    override val tr_x: Float = 1f, override val tr_y: Float = 0f,
    override val br_x: Float = 1f, override val br_y: Float = 1f,
    override val bl_x: Float = 0f, override val bl_y: Float = 1f,
    override val name: String? = null,
    override val virtFrame: RectangleInt? = null
) : BmpCoordsWithInstanceBase<T>(base, tl_x, tl_y, tr_x, tr_y, br_x, br_y, bl_x, bl_y, name, virtFrame) {
    constructor(base: T, coords: BmpCoords, name: String? = null, virtFrame: RectangleInt? = null) : this(
        base,
        coords.tl_x, coords.tl_y,
        coords.tr_x, coords.tr_y,
        coords.br_x, coords.br_y,
        coords.bl_x, coords.bl_y,
        name,
        virtFrame
    )
}

open class BmpCoordsWithInstanceBase<T : ISizeInt>(
    override val base: T,
    override val tl_x: Float = 0f, override val tl_y: Float = 0f,
    override val tr_x: Float = 1f, override val tr_y: Float = 0f,
    override val br_x: Float = 1f, override val br_y: Float = 1f,
    override val bl_x: Float = 0f, override val bl_y: Float = 1f,
    override val name: String? = null,
    override val virtFrame: RectangleInt? = null
) : BmpCoordsWithT<T> {
    constructor(base: T, coords: BmpCoords, name: String? = null, virtFrame: RectangleInt? = null) : this(
        base,
        coords.tl_x, coords.tl_y,
        coords.tr_x, coords.tr_y,
        coords.br_x, coords.br_y,
        coords.bl_x, coords.bl_y,
        name,
        virtFrame
    )
    constructor(base: BmpCoordsWithT<T>, name: String? = null, virtFrame: RectangleInt? = null) : this(base.base, base, name ?: base.name, virtFrame)

    override fun close() {
        (base as? Closeable)?.close()
    }
}

open class MutableBmpCoordsWithInstanceBase<T : ISizeInt>(
    override var base: T,
    override var tl_x: Float, override var tl_y: Float,
    override var tr_x: Float, override var tr_y: Float,
    override var br_x: Float, override var br_y: Float,
    override var bl_x: Float, override var bl_y: Float,
    override var name: String? = null,
    override var virtFrame: RectangleInt? = null
) : BmpCoordsWithT<T> {
    constructor(base: T, coords: BmpCoords, name: String? = null, virtFrame: RectangleInt? = null) : this(
        base,
        coords.tl_x, coords.tl_y,
        coords.tr_x, coords.tr_y,
        coords.br_x, coords.br_y,
        coords.bl_x, coords.bl_y,
        name,
        virtFrame
    )
    constructor(base: BmpCoordsWithT<T>, name: String? = null, virtFrame: RectangleInt? = null) : this(base.base, base, name ?: base.name, virtFrame)

    fun setTo(
        tl_x: Float, tl_y: Float,
        tr_x: Float, tr_y: Float,
        br_x: Float, br_y: Float,
        bl_x: Float, bl_y: Float,
        virtFrame: RectangleInt? = null,
    ) {
        this.tl_x = tl_x
        this.tl_y = tl_y
        this.tr_x = tr_x
        this.tr_y = tr_y
        this.br_x = br_x
        this.br_y = br_y
        this.bl_x = bl_x
        this.bl_y = bl_y
        this.virtFrame = virtFrame
    }

    fun setTo(coords: BmpCoords, virtFrame: RectangleInt? = null) {
        setTo(
            coords.tl_x, coords.tl_y, coords.tr_x, coords.tr_y,
            coords.br_x, coords.br_y, coords.bl_x, coords.bl_y,
            virtFrame
        )
    }

    fun setTo(base: T, coords: BmpCoords, name: String? = null, virtFrame: RectangleInt? = null) {
        this.base = base
        setTo(coords, virtFrame)
        this.name = name
    }

    override fun close() {
        (base as? Closeable)?.close()
    }

    fun setBasicCoords(x0: Float, y0: Float, x1: Float, y1: Float) {
        setTo(x0, y0, x1, y0, x1, y1, x0, y1)
    }
}

open class UntransformedSizeBmpCoordsWithInstance<T : ISizeInt>(
    val baseCoords: BmpCoordsWithT<T>
) : BmpCoordsWithInstanceBase<T>(baseCoords) {
    override val width: Int get() = baseCoords.baseWidth
    override val height: Int get() = baseCoords.baseHeight

    override fun toString(): String =
        "UntransformedSizeBmpCoordsWithInstance(width=$width, height=$height, baseCoords=$baseCoords)"
}

// @TODO: This was failing because frameWidth, and frameHeight was being delegated to the original instance

//open class UntransformedSizeBmpCoordsWithInstance<T : ISizeInt>(
//    val baseCoords: BmpCoordsWithT<T>
//) : BmpCoordsWithT<T> by baseCoords {
//    override val width: Int get() = baseCoords.baseWidth
//    override val height: Int get() = baseCoords.baseHeight
//
//    override fun toString(): String =
//        "UntransformedSizeBmpCoordsWithInstance(width=$width, height=$height, baseCoords=$baseCoords)"
//}

fun <T : ISizeInt> BmpCoordsWithT<T>.copy(
    base: T = this.base,
    tl_x: Float = this.tl_x, tl_y: Float = this.tl_y,
    tr_x: Float = this.tr_x, tr_y: Float = this.tr_y,
    br_x: Float = this.br_x, br_y: Float = this.br_y,
    bl_x: Float = this.bl_x, bl_y: Float = this.bl_y,
    name: String? = this.name,
    virtFrame: RectangleInt? = null
): BmpCoordsWithInstance<T> = BmpCoordsWithInstance(base, tl_x, tl_y, tr_x, tr_y, br_x, br_y, bl_x, bl_y, name, virtFrame)

fun <T : ISizeInt> BmpCoordsWithT<T>.rotatedLeft(): BmpCoordsWithInstance<T> =
    copy(base, tr_x, tr_y, br_x, br_y, bl_x, bl_y, tl_x, tl_y,
        virtFrame = if (virtFrame != null) {
            RectangleInt(frameOffsetY, frameWidth - width - frameOffsetX, frameHeight, frameWidth)
        } else {
            null
        }
    )

fun <T : ISizeInt> BmpCoordsWithT<T>.rotatedRight(): BmpCoordsWithInstance<T> =
    copy(base, bl_x, bl_y, tl_x, tl_y, tr_x, tr_y, br_x, br_y,
        virtFrame = if (virtFrame != null) {
            RectangleInt(frameHeight - height - frameOffsetY, frameOffsetX, frameHeight, frameWidth)
        } else {
            null
        }
    )

fun <T : ISizeInt> BmpCoordsWithT<T>.flippedX(): BmpCoordsWithInstance<T> =
    copy(base, tr_x, tr_y, tl_x, tl_y, bl_x, bl_y, br_x, br_y,
        virtFrame = if (virtFrame != null) {
            RectangleInt(frameWidth - width - frameOffsetX, frameOffsetY, frameWidth, frameHeight)
        } else {
            null
        }
    )

fun <T : ISizeInt> BmpCoordsWithT<T>.flippedY(): BmpCoordsWithInstance<T> =
    copy(base, bl_x, bl_y, br_x, br_y, tr_x, tr_y, tl_x, tl_y,
        virtFrame = if (virtFrame != null) {
            RectangleInt(frameOffsetX, frameHeight - height - frameOffsetY, frameWidth, frameHeight)
        } else {
            null
        }
    )

fun <T : ISizeInt> BmpCoordsWithT<T>.transformed(m: Matrix): BmpCoordsWithInstance<T> = copy(
    base,
    m.transformXf(tl_x, tl_y), m.transformYf(tl_x, tl_y),
    m.transformXf(tr_x, tr_y), m.transformYf(tr_x, tr_y),
    m.transformXf(br_x, br_y), m.transformYf(br_x, br_y),
    m.transformXf(bl_x, bl_y), m.transformYf(bl_x, bl_y),
)

fun <T : ISizeInt> BmpCoordsWithT<T>.transformed(m: Matrix3D): BmpCoordsWithInstance<T> {
    // @TODO: This allocates
    val v1 = m.transform(tl_x, tl_y, 0f, 1f)
    val v2 = m.transform(tr_x, tr_y, 0f, 1f)
    val v3 = m.transform(br_x, br_y, 0f, 1f)
    val v4 = m.transform(bl_x, bl_y, 0f, 1f)
    return copy(base, v1.x, v1.y, v2.x, v2.y, v3.x, v3.y, v4.x, v4.y)
}

fun <T : ISizeInt> BmpCoordsWithT<T>.named(name: String?): BmpCoordsWithInstance<T> = copy(name = name)

/**
 * @property virtFrame This defines a virtual frame [RectangleInt] which surrounds the bounds [RectangleInt] of the [Bitmap].
 *                     It is used in a trimmed texture atlas to specify the original size of a single texture.
 *                     X and y of virtFrame is the offset of the virtual frame to the top left edge of
 *                     the bounds rectangle. Width and height defines the size of the virtual frame.
 */
abstract class BmpSlice(
    bmpBase: Bitmap,
    val bounds: RectangleInt,
    override val name: String? = null,
    final override val virtFrame: RectangleInt? = null,
    bmpCoords: BmpCoordsWithT<*> = BmpCoordsWithInstance(bmpBase),
) : Extra, BitmapCoords {

    @Deprecated("Use bmpCoords")
    constructor(
        bmpBase: Bitmap,
        bounds: RectangleInt,
        name: String? = null,
        rotated: Boolean = false,
        virtFrame: RectangleInt? = null,
        bmpCoords: BmpCoordsWithT<*> = BmpCoordsWithInstance(bmpBase).subCoords(bounds, if (rotated) ImageOrientation.ROTATE_90 else ImageOrientation.ORIGINAL),
    ): this(bmpBase, bounds, name, virtFrame, bmpCoords) {
        this.rotated = rotated
    }

    var bmpBase: Bitmap = bmpBase
        private set

    override val base get() = bmpBase
    open val bmp: Bitmap = bmpBase
    val bmpWidth: Int get() = bmpBase.width
    val bmpHeight: Int get() = bmpBase.height

    override val left: Int get() = bounds.left
    override val top: Int get() = bounds.top
    override val width: Int get() = bounds.width
    override val height: Int get() = bounds.height
    val right get() = bounds.right
    val bottom get() = bounds.bottom

    var bmpCoords: BmpCoordsWithT<*> = bmpCoords
        private set

    init {
        if (bounds.x < 0) {
            bounds.width += bounds.x
            bounds.x = 0
        }
        if (bounds.y < 0) {
            bounds.height += bounds.y
            bounds.y = 0
        }
        if (bounds.width < 0) bounds.width = 0
        if (bounds.height < 0) bounds.height = 0
    }

    val trimmed: Boolean = virtFrame != null
    override val frameWidth: Int = virtFrame?.width ?: bounds.width
    override val frameHeight: Int = virtFrame?.height ?: bounds.height

    var parent: Any? = null

    override val tl_x: Float get() = this.bmpCoords.tl_x
    override val tl_y: Float get() = this.bmpCoords.tl_y
    override val tr_x: Float get() = this.bmpCoords.tr_x
    override val tr_y: Float get() = this.bmpCoords.tr_y
    override val br_x: Float get() = this.bmpCoords.br_x
    override val br_y: Float get() = this.bmpCoords.br_y
    override val bl_x: Float get() = this.bmpCoords.bl_x
    override val bl_y: Float get() = this.bmpCoords.bl_y

    @Deprecated("This value has no effect")
    var rotated: Boolean = false
        private set
    @Deprecated("This value has no effect")
    val rotatedAngle: Int = 0

    private val pixelOffsets: IntArray by lazy {
        val x = (tl_x * baseWidth).roundToInt()
        val y = (tl_y * baseHeight).roundToInt()
        val xOff = if (tl_x > br_x) x - 1 else x
        val yOff = if (tl_y > br_y) y - 1 else y
        val xDir = if (tl_x < br_x) 1 else -1
        val yDir = if (tl_y < br_y) 1 else -1

        if (isRotatedInBaseDeg90) {
            intArrayOf(xOff, yOff, 0, xDir, 0, yDir)
        } else {
            intArrayOf(xOff, yOff, xDir, 0, yDir, 0)
        }
    }

    private val tmpPoint: PointInt by lazy { PointInt() }

    fun isValidBasePixelPos(x: Int, y: Int): Boolean = x in 0 until frameWidth && y in 0 until frameHeight

    fun basePixelPos(x: Int, y: Int, out: PointInt = tmpPoint): PointInt? = if (isValidBasePixelPos(x, y)) basePixelPosUnsafe(x, y, out) else throw IllegalArgumentException("Point $x,$y is not in bounds of slice")

    fun basePixelPosUnsafe(x: Int, y: Int, out: PointInt = tmpPoint): PointInt? = out.also {
        val offsetX = x - frameOffsetX
        val offsetY = y - frameOffsetY
        if (offsetX < 0 || offsetY < 0 || offsetX >= width || offsetY >= height)
            return null

        it.x = pixelOffsets[0] + pixelOffsets[2] * offsetX + pixelOffsets[3] * offsetY
        it.y = pixelOffsets[1] + pixelOffsets[4] * offsetY + pixelOffsets[5] * offsetX
    }

    fun readPixels(x: Int, y: Int, width: Int, height: Int, out: RgbaArray = RgbaArray(width * height), offset: Int = 0): RgbaArray {
        check(isValidBasePixelPos(x, y))
        check(isValidBasePixelPos(x + width - 1, y + height - 1))
        check(out.size >= offset + width * height)
        readPixelsUnsafe(x, y, width, height, out, offset)
        return out
    }

    fun readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int = 0) {
        var n = offset
        val p = PointInt()
        for (y0 in 0 until height) for (x0 in 0 until width) {
            if (basePixelPosUnsafe(x0 + x, y0 + y, p) != null) {
                out[n++] = bmpBase.getRgba(p.x, p.y)
            } else {
                out[n++] = Colors.TRANSPARENT_BLACK
            }
        }
    }

    fun getRgba(x: Int, y: Int): RGBA {
        basePixelPos(x, y)?.let {
            return bmpBase.getRgba(it.x, it.y)
        }
        return Colors.TRANSPARENT_BLACK
    }

    fun setRgba(x: Int, y: Int, value: RGBA) {
        basePixelPos(x, y).also {
            if (it != null) {
                bmpBase.setRgba(it.x, it.y, value)
            } else {
                if (x < 0 || y < 0 || x >= frameWidth || y >= frameHeight) {
                    throw IllegalArgumentException("Point $x,$y is not in bounds of slice")
                }
                bmpBase = extract()
                bounds.setBoundsTo(0, 0, bmpBase.width, bmpBase.height)
                virtFrame?.setBoundsTo(0, 0, bmpBase.width, bmpBase.height)
                bmpCoords = BmpCoordsWithInstanceBase(
                    SizeInt(bmpBase.width, bmpBase.height)
                )
                intArrayOf(0, 0, 1, 0, 1, 0).copyInto(pixelOffsets)
                bmpBase.setRgba(x, y, value)
            }
        }
    }

    open fun sliceWithBounds(left: Int, top: Int, right: Int, bottom: Int, name: String? = null, imageOrientation: ImageOrientation = ImageOrientation.ORIGINAL): BmpSlice = slice(RectangleInt(left, top, right - left, bottom - top), name, imageOrientation)
    open fun sliceWithSize(x: Int, y: Int, width: Int, height: Int, name: String? = null, imageOrientation: ImageOrientation = ImageOrientation.ORIGINAL): BmpSlice = slice(RectangleInt(x, y, width, height), name, imageOrientation)
    open fun slice(rect: RectangleInt, name: String? = null, imageOrientation: ImageOrientation = ImageOrientation.ORIGINAL): BmpSlice =
        BitmapSlice(bmp, rect, name, bmpCoords = subCoords(rect, imageOrientation))
    open fun slice(rect: Rectangle, name: String? = null, imageOrientation: ImageOrientation = ImageOrientation.ORIGINAL): BmpSlice = slice(rect.toInt(), name, imageOrientation)

    internal fun <T: Bitmap> extractWithBase(base: T): T
    {
        val out: T
        val x = (min(min(tl_x, tr_x), min(bl_x, br_x)) * baseWidth).roundToInt()
        val y = (min(min(tl_y, tr_y), min(bl_y, br_y)) * baseHeight).roundToInt()
        val rotated = isRotatedInBaseDeg90
        val reverseX = width > 1 && if (rotated) tl_y > br_y else tl_x > br_x
        val reverseY = height > 1 && if (rotated) tl_x > br_x else tl_y > br_y

        if (frameOffsetX == 0 && frameOffsetY == 0 && frameWidth == width && frameHeight == height) {
            out = base.extract(x, y, width, height)
        } else {
            out = base.createWithThisFormatTyped(frameWidth, frameHeight)
            if (!rotated) {
                bmp.copyUnchecked(x, y, out, frameOffsetX, frameOffsetY, width, height)
            } else {
                val rgbaArray = RgbaArray(width)
                for (x0 in 0 until height) {
                    bmp.readPixelsUnsafe(x + x0, y, 1, width, rgbaArray)
                    out.writePixelsUnsafe(frameOffsetX, frameOffsetY + x0, width, 1, rgbaArray)
                }
            }
        }
        if (reverseX) {
            out.flipX()
        }
        if (reverseY) {
            out.flipY()
        }
        return out
    }
}

val BmpSlice.nameSure: String get() = name ?: "unknown"
fun <T : Bitmap> BmpSlice.asBitmapSlice(): BitmapSlice<T> = this as BitmapSlice<T>

fun BmpSlice.getIntBounds(out: RectangleInt = RectangleInt()) = out.setTo(left, top, width, height)

fun BmpSlice.extract(): Bitmap = this.extractWithBase(bmpBase)

class BitmapSlice<out T : Bitmap>(
    override val bmp: T,
    bounds: RectangleInt,
    name: String? = null,
    virtFrame: RectangleInt? = null,
    bmpCoords: BmpCoordsWithT<*> = BmpCoordsWithInstance(bmp),
) : BmpSlice(bmp, bounds, name, virtFrame, bmpCoords), Extra by Extra.Mixin() {

    @Deprecated("Use bmpCoords")
    constructor(
        bmp: T,
        bounds: RectangleInt,
        name: String? = null,
        rotated: Boolean = false,
        virtFrame: RectangleInt? = null,
    ): this(bmp, bounds, name, virtFrame, BmpCoordsWithInstance(bmp).subCoords(bounds, if (rotated) ImageOrientation.ROTATE_90 else ImageOrientation.ORIGINAL))

    val premultiplied get() = bmp.premultiplied

    fun extract(): T = extractWithBase(bmp)

    override fun sliceWithBounds(left: Int, top: Int, right: Int, bottom: Int, name: String?, imageOrientation: ImageOrientation): BitmapSlice<T> = slice(RectangleInt(left, top, right - left, bottom - top), name, imageOrientation)
    override fun sliceWithSize(x: Int, y: Int, width: Int, height: Int, name: String?, imageOrientation: ImageOrientation): BitmapSlice<T> = slice(RectangleInt(x, y, width, height), name, imageOrientation)
    override fun slice(rect: RectangleInt, name: String?, imageOrientation: ImageOrientation): BitmapSlice<T> =
        BitmapSlice(bmp, rect, name, bmpCoords = this.subCoords(rect, imageOrientation))
    override fun slice(rect: Rectangle, name: String?, imageOrientation: ImageOrientation): BitmapSlice<T> = slice(rect.toInt(), name, imageOrientation)

    fun split(width: Int, height: Int): List<BitmapSlice<T>> = splitInRows(width, height)

    fun splitInRows(width: Int, height: Int): List<BitmapSlice<T>> {
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

    fun withName(name: String? = null) = copy(name = name)

    override fun toString(): String = "BitmapSlice($name:${SizeInt(bounds.width, bounds.height)})"
}

@Deprecated("Use copy with ImageOrientation")
inline fun <T : Bitmap> BitmapSlice<T>.copy(
    bmp: T = this.bmp,
    bounds: RectangleInt = this.bounds,
    name: String? = this.name,
    rotated: Boolean = this.rotated,
    virtFrame: RectangleInt? = this.virtFrame
) = BitmapSlice(bmp, bounds, name, rotated, virtFrame)

inline fun <T : Bitmap> BitmapSlice<T>.copy(
    bmp: T = this.bmp,
    bounds: RectangleInt = this.bounds,
    name: String? = this.name,
    virtFrame: RectangleInt? = this.virtFrame,
    bmpCoords: BmpCoordsWithT<*>
) = BitmapSlice(bmp, bounds, name, virtFrame, bmpCoords)

fun BitmapSlice<Bitmap>.virtFrame(rect: RectangleInt?) =
    if (rect != null)
        copy(virtFrame = rect, bmpCoords = this.bmpCoords)
    else
        this
fun BitmapSlice<Bitmap>.virtFrame(x: Int, y: Int, w: Int, h: Int) = virtFrame(RectangleInt(x, y, w, h))

// http://pixijs.download/dev/docs/PIXI.Texture.html#Texture
fun BitmapSliceCompat(
	bmp: Bitmap,
	frame: Rectangle,
	orig: Rectangle,
	trim: Rectangle,
	rotated: Boolean,
	name: String = "unknown"
) = bmp.sliceWithSize(frame.toInt(), name, if (rotated) ImageOrientation.ROTATE_90 else ImageOrientation.ORIGINAL)

fun <T : Bitmap> T.slice(bounds: RectangleInt = RectangleInt(0, 0, width, height), name: String? = null, imageOrientation: ImageOrientation = ImageOrientation.ORIGINAL): BitmapSlice<T> = BitmapSlice(this, RectangleInt(0, 0, width, height), name, bmpCoords = BmpCoordsWithInstance(this))
    .let {
        if (bounds != it.bounds || imageOrientation != ImageOrientation.ORIGINAL) {
            it.slice(bounds, name, imageOrientation)
        } else {
            it
        }
    }
fun <T : Bitmap> T.sliceWithBounds(left: Int, top: Int, right: Int, bottom: Int, name: String? = null, imageOrientation: ImageOrientation = ImageOrientation.ORIGINAL): BitmapSlice<T> = slice(RectangleInt(left, top, right - left, bottom - top), name, imageOrientation)
fun <T : Bitmap> T.sliceWithSize(x: Int, y: Int, width: Int, height: Int, name: String? = null, imageOrientation: ImageOrientation = ImageOrientation.ORIGINAL): BitmapSlice<T> = slice(RectangleInt(x, y, width, height), name, imageOrientation)
fun <T : Bitmap> T.sliceWithSize(rect: RectangleInt, name: String? = null, imageOrientation: ImageOrientation = ImageOrientation.ORIGINAL): BitmapSlice<T> = slice(RectangleInt(rect.x, rect.y, rect.width, rect.height), name, imageOrientation)

private fun createRectangleInt(
    bleft: Int, btop: Int, bright: Int, bbottom: Int,
    left: Int, top: Int, right: Int, bottom: Int,
    allowInvalidBounds: Boolean = false
): RectangleInt = RectangleInt.fromBounds(
    (bleft + left).clamp(bleft, bright),
    (btop + top).clamp(btop, bbottom),
    (bleft + right).clamp(if (allowInvalidBounds) bleft else bleft + left, bright),
    (btop + bottom).clamp(if (allowInvalidBounds) btop else btop + top, bbottom)
)
