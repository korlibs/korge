package korlibs.image.bitmap

import korlibs.datastructure.*
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.color.RGBAPremultiplied
import korlibs.image.color.RgbaArray
import korlibs.image.color.asNonPremultiplied
import korlibs.image.color.asPremultiplied
import korlibs.image.format.*
import korlibs.image.vector.Context2d
import korlibs.io.lang.invalidOp
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import korlibs.memory.*
import kotlin.math.min

abstract class Bitmap(
    val width: Int,
    val height: Int,
    val bpp: Int,
    premultiplied: Boolean,
    val backingArray: Any?
) : SizeableInt, Extra by Extra.Mixin() {
    val rect: RectangleInt = RectangleInt(0, 0, width, height)
    override val size: SizeInt get() = SizeInt(width, height)
    var bitmapName: String? = null

    var premultiplied: Boolean = premultiplied
        set(value) {
            field = value
            //printStackTrace("Changed premultiplied! $value")
        }

    var asumePremultiplied: Boolean = false
    //override fun getOrNull() = this
    //override suspend fun get() = this

    protected val tempInts: IntArray by lazy { IntArray(width * 2) }
    protected val tempRgba: RgbaArray get() = RgbaArray(tempInts)

    /** Version of the content. lock+unlock mutates this version to allow for example to re-upload the bitmap to the GPU when synchronizing bitmaps into textures */
    var contentVersion: Int = 0

    var dirtyRegion: RectangleInt? = null
        private set

    /** Specifies whether mipmaps should be created for this [Bitmap] */
    var mipmaps: Boolean = false

    val stride: Int get() = (width * bpp) / 8
    val area: Int get() = width * height
    fun index(x: Int, y: Int) = y * width + x
    fun inside(x: Int, y: Int) = x in 0 until width && y in 0 until height

    fun clearDirtyRegion() {
        if (dirtyRegion != null) {
            dirtyRegion = null
        }
    }

    open fun lock() = Unit
    open fun unlock(rect: RectangleInt = this.rect): Int {
        when (dirtyRegion) {
            null -> dirtyRegion = rect
            else -> dirtyRegion = RectangleInt.union(dirtyRegion!!, rect)
        }
        return ++contentVersion
    }

    inline fun lock(rect: RectangleInt = this.rect, doLock: Boolean = true, block: () -> Unit): Int {
        if (doLock) lock()
        val result: Int
        try {
            block()
        } finally {
            result = if (doLock) unlock(rect) else 0
        }
        return result
    }

    open fun readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: IntArray, offset: Int = 0) {
        var n = offset
        for (y0 in 0 until height) for (x0 in 0 until width) out[n++] = getRgbaRaw(x0 + x, y0 + y).value
    }

    open fun writePixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: IntArray, offset: Int = 0) {
        var n = offset
        for (y0 in 0 until height) for (x0 in 0 until width) setRgbaRaw(x0 + x, y0 + y, RGBA(out[n++]))
    }

    //open fun readRgbaUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int = 0) {
    //    readPixelsUnsafe(x, y, width, height, out.ints, offset)
    //    if (premultiplied) RgbaPremultipliedArray(out.ints).depremultiplyInplace(offset, offset + width * height)
    //}
    //open fun writeRgbaUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int = 0) {
    //    TODO()
    //}

    /** UNSAFE: Sets the color [v] in the [x], [y] coordinates in the internal format of this Bitmap (either premultiplied or not) */
    open fun setRgbaRaw(x: Int, y: Int, v: RGBA): Unit {
        TODO()
    }

    /** UNSAFE: Gets the color [v] in the [x], [y] coordinates in the internal format of this Bitmap (either premultiplied or not) */
    open fun getRgbaRaw(x: Int, y: Int): RGBA = Colors.TRANSPARENT

    /** Sets the color [v] in the [x], [y] coordinates in [RGBA] non-premultiplied */
    open fun setRgba(x: Int, y: Int, v: RGBA): Unit {
        if (premultiplied) setRgbaRaw(x, y, v.premultiplied.asNonPremultiplied()) else setRgbaRaw(x, y, v)
    }

    /** Sets the color [v] in the [x], [y] coordinates in [RGBAPremultiplied] */
    open fun setRgba(x: Int, y: Int, v: RGBAPremultiplied): Unit {
        if (premultiplied) setRgbaRaw(x, y, v.asNonPremultiplied()) else setRgbaRaw(x, y, v.depremultiplied)
    }

    /** Gets the color [v] in the [x], [y] coordinates in [RGBA] non-premultiplied */
    open fun getRgba(x: Int, y: Int): RGBA =
        if (premultiplied) getRgbaRaw(x, y).asPremultiplied().depremultiplied else getRgbaRaw(x, y)

    /** Gets the color [v] in the [x], [y] coordinates in [RGBAPremultiplied] */
    open fun getRgbaPremultiplied(x: Int, y: Int): RGBAPremultiplied =
        if (premultiplied) getRgbaRaw(x, y).asPremultiplied() else getRgbaRaw(x, y).premultiplied

    /** UNSAFE: Sets the color [color] in the [x], [y] coordinates in the internal format of this Bitmap */
    open fun setInt(x: Int, y: Int, color: Int): Unit = Unit

    /** UNSAFE: Gets the color in the [x], [y] coordinates in the internal format of this Bitmap */
    open fun getInt(x: Int, y: Int): Int = 0

    fun getRgbaClamped(x: Int, y: Int): RGBA = if (inBounds(x, y)) getRgbaRaw(x, y) else Colors.TRANSPARENT

    fun getRgbaClampedBorder(x: Int, y: Int): RGBA = getRgbaRaw(x.clamp(0, width - 1), y.clamp(0, height - 1))

    fun getRgbaSampled(x: Float, y: Float): RGBA {
        val x0 = x.toIntFloor()
        val y0 = y.toIntFloor()
        if (x0 < 0 || y0 < 0 || x0 >= width || y0 >= height) return Colors.TRANSPARENT
        val x1 = x.toIntCeil()
        val y1 = y.toIntCeil()
        val x1Inside = x1 < width - 1
        val y1Inside = y1 < height - 1
        val xratio = fract(x)
        val yratio = fract(y)
        val c00 = getRgbaRaw(x0, y0)
        val c10 = if (x1Inside) getRgbaRaw(x1, y0) else c00
        val c01 = if (y1Inside) getRgbaRaw(x0, y1) else c00
        val c11 = if (x1Inside && y1Inside) getRgbaRaw(x1, y1) else c01
        return RGBA.mixRgba4(c00, c10, c01, c11, xratio.toRatio(), yratio.toRatio())
    }

    fun getRgbaSampled(x: Float, y: Float, count: Int, row: RgbaArray) {
        for (n in 0 until count) {
            row[n] = getRgbaSampled(x + n, y)
        }
    }

    fun copy(srcX: Int, srcY: Int, dst: Bitmap, dstX: Int, dstY: Int, width: Int, height: Int) {
        val src = this

        val srcX0 = src.clampWidth(srcX)
        val srcX1 = src.clampWidth(srcX + width)
        val srcY0 = src.clampHeight(srcY)
        val srcY1 = src.clampHeight(srcY + height)

        val dstX0 = dst.clampWidth(dstX)
        val dstX1 = dst.clampWidth(dstX + width)
        val dstY0 = dst.clampHeight(dstY)
        val dstY1 = dst.clampHeight(dstY + height)

        val srcX = srcX0
        val srcY = srcY0
        val dstX = dstX0
        val dstY = dstY0

        val width = min(srcX1 - srcX0, dstX1 - dstX0)
        val height = min(srcY1 - srcY0, dstY1 - dstY0)

        copyUnchecked(srcX, srcY, dst, dstX, dstY, width, height)
    }

    open fun copyUnchecked(srcX: Int, srcY: Int, dst: Bitmap, dstX: Int, dstY: Int, width: Int, height: Int) {
        for (y in 0 until height) {
            readPixelsUnsafe(srcX, srcY + y, width, 1, tempInts, 0)
            dst.writePixelsUnsafe(dstX, dstY + y, width, 1, tempInts, 0)
        }
    }

    fun inBoundsX(x: Int) = (x >= 0) && (x < width)
    fun inBoundsY(y: Int) = (y >= 0) && (y < height)

    fun inBounds(x: Int, y: Int) = inBoundsX(x) && inBoundsY(y)

    fun clampX(x: Int) = x.clamp(0, width - 1)
    fun clampY(y: Int) = y.clamp(0, height - 1)

    fun clampWidth(x: Int) = x.clamp(0, width)
    fun clampHeight(y: Int) = y.clamp(0, height)

    open fun flipY(): Bitmap {
        for (y in 0 until height / 2) swapRows(y, height - y - 1)
        return this
    }

    /** Inplace flips */
    open fun flipX(): Bitmap {
        for (x in 0 until width / 2) swapColumns(x, width - x - 1)
        return this
    }

    fun flippedY(): Bitmap = clone().flipY()
    fun flippedX(): Bitmap = clone().flipX()

    fun rotated(rotation: ImageRotation): Bitmap = when (rotation) {
        ImageRotation.R0 -> this.clone()
        ImageRotation.R90 -> this.transposed().flipX()
        //ImageRotation.R180 -> this.clone().flipX()//.flipY()
        ImageRotation.R180 -> this.transposed().flipX().transposed().flipX()
        ImageRotation.R270 -> this.transposed().flipY()
    }

    fun rotatedRight(): Bitmap = rotated(ImageRotation.R90)
    fun rotatedLeft(): Bitmap = rotated(ImageRotation.R270)

    fun oriented(orientation: ImageOrientation): Bitmap {
        val out = this.clone()
        if (orientation.flipX) out.flipX()
        return out.rotated(orientation.rotation)
    }

    /** Creates a new bitmap with the rows and columns transposed */
    open fun transposed(): Bitmap {
        val temp = IntArray2(width, height, readPixelsUnsafe(0, 0, width, height))
        val out = IntArray2(height, width, 0)
        for (y in 0 until height) for (x in 0 until width) out[y, x] = temp[x, y]
        return createWithThisFormat(height, width).also { it.writePixelsUnsafe(0, 0, height, width, out.data) }
    }

    open fun swapRows(y0: Int, y1: Int) {
        for (x in 0 until width) {
            val c0 = getInt(x, y0)
            val c1 = getInt(x, y1)
            setInt(x, y0, c1)
            setInt(x, y1, c0)
        }
    }

    open fun swapColumns(x0: Int, x1: Int) {
        for (y in 0 until height) {
            val c0 = getInt(x0, y)
            val c1 = getInt(x1, y)
            setInt(x0, y, c1)
            setInt(x1, y, c0)
        }
    }

    inline fun forEach(
        sx: Int = 0,
        sy: Int = 0,
        width: Int = this.width - sx,
        height: Int = this.height - sy,
        callback: (n: Int, x: Int, y: Int) -> Unit
    ) {
        for (y in sy until sy + height) {
            var n = index(sx, sy + y)
            for (x in sx until sx + width) {
                callback(n++, x, y)
            }
        }
    }

    open fun getContext2d(antialiasing: Boolean = true): Context2d =
        throw UnsupportedOperationException("Not implemented context2d on Bitmap, please use NativeImage or Bitmap32 instead")

    open fun createWithThisFormat(width: Int, height: Int): Bitmap =
        invalidOp("Unsupported createWithThisFormat ($this)")

    open fun toBMP32(): Bitmap32 = Bitmap32(width, height, premultiplied = premultiplied).also { out ->
        this.readPixelsUnsafe(0, 0, width, height, out.ints, 0)
    }

    fun toBMP32IfRequired(): Bitmap32 = if (this is Bitmap32) this else this.toBMP32()

    open fun contentEquals(other: Bitmap): Boolean {
        if (this.width != other.width) return false
        if (this.height != other.height) return false
        for (y in 0 until height) for (x in 0 until width) {
            if (this.getRgbaRaw(x, y) != other.getRgbaRaw(x, y)) return false
        }
        return true
    }

    open fun contentHashCode(): Int {
        var v = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                v += this.getRgbaRaw(x, y).value.hashCode() * (7 + x + y * 3)
            }
        }
        return (width * 31 + height) + v + premultiplied.toInt()
    }

    open fun clone(): Bitmap {
        val out = createWithThisFormat(width, height)
        copyUnchecked(0, 0, out, 0, 0, width, height)
        return out
    }
}

fun Bitmap.readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int): IntArray {
    val out = IntArray(width * height)
    readPixelsUnsafe(x, y, width, height, out, 0)
    return out
}

fun <T : Bitmap> T.createWithThisFormatTyped(width: Int, height: Int): T = this.createWithThisFormat(width, height).fastCastTo<T>()

fun <T : Bitmap> T.extract(x: Int, y: Int, width: Int, height: Int): T {
    val out = this.createWithThisFormatTyped(width, height)
    this.copy(x, y, out, 0, 0, width, height)
    return out
}

fun Bitmap32Context2d(width: Int, height: Int, antialiased: Boolean = true, block: Context2d.() -> Unit): Bitmap32 =
    Bitmap32(width, height, premultiplied = true).context2d(antialiased = antialiased, doLock = false) { block() }
fun NativeImageContext2d(width: Int, height: Int, antialiased: Boolean = true, block: Context2d.() -> Unit): NativeImage =
    NativeImage(width, height, premultiplied = true).context2d(antialiased = antialiased, doLock = false) { block() }
fun NativeImageOrBitmap32Context2d(width: Int, height: Int, antialiased: Boolean = true, native: Boolean = true, block: Context2d.() -> Unit): Bitmap =
    NativeImageOrBitmap32(width, height, premultiplied = true, native = native).context2d(antialiased = antialiased, doLock = false) { block() }

inline fun <T : Bitmap> T.context2d(antialiased: Boolean = true, doLock: Boolean = true, callback: Context2d.() -> Unit): T {
    lock(doLock = doLock) {
        val ctx = getContext2d(antialiased)
        try {
            callback(ctx)
        } finally {
            ctx.dispose()
        }
    }
    return this
}

fun <T : Bitmap> T.checkMatchDimensions(other: T): T {
    check((this.width == other.width) && (this.height == other.height)) { "Bitmap doesn't have the same dimensions (${width}x${height}) != (${other.width}x${other.height})" }
    return other
}

/**
 * Enable or disable mipmap generation for this [Bitmap]
 *
 * Import: For this to work, both [Bitmap.width] and [Bitmap.height] must be power of two: 2, 4, 8, ..., 512, 1024, ..., 4096
 *
 * (Not used directly by KorIM, but KorGE)
 * */
fun <T : Bitmap> T.mipmaps(enable: Boolean = true): T = this.apply { this.mipmaps = enable }

var Bitmap.baseMipmapLevel: Int? by Extra.Property { null }
var Bitmap.maxMipmapLevel: Int? by Extra.Property { null }

fun <T : Bitmap> T.asumePremultiplied(): T {
    this.premultiplied = true
    this.asumePremultiplied = true
    return this
}
