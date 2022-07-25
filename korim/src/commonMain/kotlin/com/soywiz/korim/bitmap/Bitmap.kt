package com.soywiz.korim.bitmap

import com.soywiz.kds.Extra
import com.soywiz.kds.atomic.kdsIsFrozen
import com.soywiz.kds.fastCastTo
import com.soywiz.kmem.clamp
import com.soywiz.kmem.fract
import com.soywiz.kmem.toIntCeil
import com.soywiz.kmem.toIntFloor
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.RGBAPremultiplied
import com.soywiz.korim.color.RgbaArray
import com.soywiz.korim.color.asNonPremultiplied
import com.soywiz.korim.color.asPremultiplied
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.lang.invalidOp
import com.soywiz.korma.geom.ISizeInt
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.Size
import com.soywiz.korma.geom.Sizeable
import kotlin.math.min

abstract class Bitmap(
    override val width: Int,
    override val height: Int,
    val bpp: Int,
    premultiplied: Boolean,
    val backingArray: Any?
) : Sizeable, ISizeInt, Extra by Extra.Mixin() {

    var premultiplied: Boolean = premultiplied
        set(value) {
            field = value
            //printStackTrace("Changed premultiplied! $value")
        }

    var asumePremultiplied: Boolean = false
    //override fun getOrNull() = this
    //override suspend fun get() = this

    //@ThreadLocal
    protected val tempInts: IntArray by lazy { IntArray(width * 2) }
    protected val tempRgba: RgbaArray get() = RgbaArray(tempInts)

    /** Version of the content. lock+unlock mutates this version to allow for example to re-upload the bitmap to the GPU when synchronizing bitmaps into textures */
    var contentVersion: Int = 0

    /** Associated texture object to this Bitmap that could be used by other engines */
	var texture: Any? = null

    var dirtyRegion: Rectangle? = null
        private set

    private val dirtyRegionObj: Rectangle = Rectangle()

    /** Specifies whether mipmaps should be created for this [Bitmap] */
    var mipmaps: Boolean = false

	val stride: Int get() = (width * bpp) / 8
	val area: Int get() = width * height
	fun index(x: Int, y: Int) = y * width + x
    fun inside(x: Int, y: Int) = x in 0 until width && y in 0 until height
	override val size: Size get() = Size(width, height)

    fun clearDirtyRegion() {
        if (dirtyRegion != null) {
            if (!kdsIsFrozen(this)) {
                dirtyRegion = null
            }
        }
    }

    open fun lock() = Unit
    open fun unlock(rect: Rectangle? = null): Int {
        if (rect != null) {
            if (dirtyRegion == null) {
                dirtyRegionObj.copyFrom(rect)
            } else {
                dirtyRegionObj.setToUnion(dirtyRegionObj, rect)
            }
        } else {
            dirtyRegionObj.setTo(0, 0, width, height)
        }
        dirtyRegion = dirtyRegionObj
        return ++contentVersion
    }

    inline fun lock(rect: Rectangle? = null, doLock: Boolean = true, block: () -> Unit): Int {
        if (doLock) lock()
        try {
            block()
        } finally {
            return if (doLock) unlock(rect) else 0
        }
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
    open fun setRgbaRaw(x: Int, y: Int, v: RGBA): Unit = TODO()
    /** UNSAFE: Gets the color [v] in the [x], [y] coordinates in the internal format of this Bitmap (either premultiplied or not) */
    open fun getRgbaRaw(x: Int, y: Int): RGBA = Colors.TRANSPARENT_BLACK

    /** Sets the color [v] in the [x], [y] coordinates in [RGBA] non-premultiplied */
    open fun setRgba(x: Int, y: Int, v: RGBA): Unit {
        if (premultiplied) setRgbaRaw(x, y, v.premultiplied.asNonPremultiplied()) else setRgbaRaw(x, y, v)
    }
    /** Sets the color [v] in the [x], [y] coordinates in [RGBAPremultiplied] */
    open fun setRgba(x: Int, y: Int, v: RGBAPremultiplied): Unit {
        if (premultiplied) setRgbaRaw(x, y, v.asNonPremultiplied()) else setRgbaRaw(x, y, v.depremultiplied)
    }

    /** Gets the color [v] in the [x], [y] coordinates in [RGBA] non-premultiplied */
    open fun getRgba(x: Int, y: Int): RGBA = if (premultiplied) getRgbaRaw(x, y).asPremultiplied().depremultiplied else getRgbaRaw(x, y)
    /** Gets the color [v] in the [x], [y] coordinates in [RGBAPremultiplied] */
    open fun getRgbaPremultiplied(x: Int, y: Int): RGBAPremultiplied = if (premultiplied) getRgbaRaw(x, y).asPremultiplied() else getRgbaRaw(x, y).premultiplied

    /** UNSAFE: Sets the color [color] in the [x], [y] coordinates in the internal format of this Bitmap */
	open fun setInt(x: Int, y: Int, color: Int): Unit = Unit
    /** UNSAFE: Gets the color in the [x], [y] coordinates in the internal format of this Bitmap */
	open fun getInt(x: Int, y: Int): Int = 0

	fun getRgbaClamped(x: Int, y: Int): RGBA = if (inBounds(x, y)) getRgbaRaw(x, y) else Colors.TRANSPARENT_BLACK

    fun getRgbaClampedBorder(x: Int, y: Int): RGBA = getRgbaRaw(x.clamp(0, width - 1), y.clamp(0, height - 1))

    @Deprecated("Use float version")
    fun getRgbaSampled(x: Double, y: Double): RGBA = getRgbaSampled(x.toFloat(), y.toFloat())

	fun getRgbaSampled(x: Float, y: Float): RGBA {
		val x0 = x.toIntFloor()
        val y0 = y.toIntFloor()
        if (x0 < 0 || y0 < 0 || x0 >= width || y0 > height) return Colors.TRANSPARENT_BLACK
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
		return RGBA.mixRgba4(c00, c10, c01, c11, xratio, yratio)
	}

    fun getRgbaSampled(x: Double, y: Double, count: Int, row: RgbaArray) {
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

    open fun flipX(): Bitmap {
		for (x in 0 until width / 2) swapColumns(x, width - x - 1)
        return this
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

    inline fun forEach(sx: Int = 0, sy: Int = 0, width: Int = this.width - sx, height: Int = this.height - sy, callback: (n: Int, x: Int, y: Int) -> Unit) {
        for (y in sy until sy + height) {
            var n = index(sx, sy + y)
            for (x in sx until sx + width) {
                callback(n++, x, y)
            }
        }
    }

    open fun getContext2d(antialiasing: Boolean = true): Context2d =
		throw UnsupportedOperationException("Not implemented context2d on Bitmap, please use NativeImage or Bitmap32 instead")

	open fun createWithThisFormat(width: Int, height: Int): Bitmap = invalidOp("Unsupported createWithThisFormat ($this)")

	open fun toBMP32(): Bitmap32 = Bitmap32(width, height, premultiplied = premultiplied).also { out ->
        this.readPixelsUnsafe(0, 0, width, height, out.ints, 0)
    }

    fun toBMP32IfRequired(): Bitmap32 = if (this is Bitmap32) this else this.toBMP32()

    fun contentEquals(other: Bitmap): Boolean {
        if (this.width != other.width) return false
        if (this.height != other.height) return false
        for (y in 0 until height) for (x in 0 until width) {
            if (this.getRgbaRaw(x, y) != other.getRgbaRaw(x, y)) return false
        }
        return true
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

fun Bitmap32Context2d(width: Int, height: Int, antialiased: Boolean = true, block: Context2d.() -> Unit): Bitmap32 {
    return Bitmap32(width, height, premultiplied = true).context2d(antialiased = antialiased, doLock = false) { block() }
}

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

/** Enable or disable mipmap generation for this [Bitmap] (Not used directly by KorIM, but KorGE) */
fun <T : Bitmap> T.mipmaps(enable: Boolean = true): T = this.apply { this.mipmaps = enable }


fun <T : Bitmap> T.asumePremultiplied(): T {
    this.premultiplied = true
    this.asumePremultiplied = true
    return this
}
