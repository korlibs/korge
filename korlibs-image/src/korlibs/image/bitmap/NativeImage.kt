package korlibs.image.bitmap

import korlibs.image.color.RGBA
import korlibs.image.color.RGBAPremultiplied
import korlibs.image.color.RgbaArray
import korlibs.image.color.RgbaPremultipliedArray
import korlibs.image.format.*
import korlibs.image.vector.Drawable
import korlibs.image.vector.SizedDrawable
import korlibs.encoding.toBase64

abstract class NativeImage(width: Int, height: Int, val data: Any?, premultiplied: Boolean) : Bitmap(width, height, 32, premultiplied, null), NativeImageRef {
	abstract val name: String
    open fun toUri(): String = "data:image/png;base64," + PNG.encode(this, ImageEncodingProps("out.png")).toBase64()

	fun toNonNativeBmp(): Bitmap = toBMP32()
    override fun toBMP32(): Bitmap32 = Bitmap32(width, height, premultiplied).also { readPixelsUnsafe(0, 0, width, height, it.ints, 0) }

    abstract override fun readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: IntArray, offset: Int)
    abstract override fun writePixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: IntArray, offset: Int)

    fun readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int): IntArray {
        val out = IntArray(width * height)
        readPixelsUnsafe(x, y, width, height, out)
        return out
    }

    override fun setRgbaRaw(x: Int, y: Int, v: RGBA) {
        this.tempRgba[0] = v
        writePixelsUnsafe(x, y, 1, 1, tempInts, 0)
    }

    override fun getRgbaRaw(x: Int, y: Int): RGBA {
        readPixelsUnsafe(x, y, 1, 1, tempInts, 0)
        return tempRgba[0]
    }

    override fun setInt(x: Int, y: Int, color: Int) = setRgbaRaw(x, y, RGBA(color))
    override fun getInt(x: Int, y: Int): Int = getRgbaRaw(x, y).value

    override fun flipY(): Bitmap {
        writePixelsUnsafe(0, 0, width, height, (toBMP32().flipY() as Bitmap32).ints)
        return this
    }
    override fun flipX(): Bitmap {
        writePixelsUnsafe(0, 0, width, height, (toBMP32().flipX() as Bitmap32).ints)
        return this
    }

    override fun swapRows(y0: Int, y1: Int) {
        readPixelsUnsafe(0, y0, width, 1, tempInts, 0)
        readPixelsUnsafe(0, y1, width, 1, tempInts, width)
        writePixelsUnsafe(0, y1, width, 1, tempInts, 0)
        writePixelsUnsafe(0, y0, width, 1, tempInts, width)
    }

    override fun swapColumns(x0: Int, x1: Int) {
        readPixelsUnsafe(x0, 0, 1, height, tempInts, 0)
        readPixelsUnsafe(x1, 0, 1, height, tempInts, width)
        writePixelsUnsafe(x1, 0, 1, height, tempInts, 0)
        writePixelsUnsafe(x0, 0, 1, height, tempInts, width)
    }

    override fun contentEquals(other: Bitmap): Boolean = toBMP32().contentEquals(other)
    override fun contentHashCode(): Int = toBMP32().contentHashCode()

    override fun createWithThisFormat(width: Int, height: Int): Bitmap = NativeImage(width, height)
    override fun toString(): String = "$name($width, $height)"
}

interface ForcedTexId {
    /** Allow to force to use a texture id from OpenGL. For example a video texture from Android */
    val forcedTexId: Int
    /** Allow to force to use a texture target from OpenGL. For example a video texture from Android (-1 means GL_TEXTURE_2D) */
    val forcedTexTarget: Int get() = TEXTURE_2D

    data class Fixed(
        override val forcedTexId: Int,
        override val forcedTexTarget: Int = TEXTURE_2D
    ) : ForcedTexId

    companion object {
        // OpenGL typical constants
        const val TEXTURE_2D: Int = 0x0DE1
        const val TEXTURE_EXTERNAL = 0x8D65
    }
}

abstract class ForcedTexNativeImage(width: Int, height: Int, premultiplied: Boolean = true) : NativeImage(width, height, null, premultiplied), ForcedTexId {
    override val name: String get() = "ForcedTexNativeImage"

    override fun readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: IntArray, offset: Int) {
        TODO("Not yet implemented")
    }

    override fun writePixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: IntArray, offset: Int) {
        TODO("Not yet implemented")
    }
}

fun Bitmap.mipmap(levels: Int): NativeImage = nativeImageFormatProvider.mipmap(this, levels)

fun Bitmap.toUri(): String {
	if (this is NativeImage) return this.toUri()
	return "data:image/png;base64," + PNG.encode(this, ImageEncodingProps("out.png")).toBase64()
}

fun NativeImageOrBitmap32(width: Int, height: Int, native: Boolean = true, premultiplied: Boolean? = null) =
    if (native) NativeImage(width, height, premultiplied) else Bitmap32(width, height, premultiplied = premultiplied ?: true)
fun NativeImage(width: Int, height: Int, premultiplied: Boolean? = null) =
    nativeImageFormatProvider.create(width, height, premultiplied)
fun NativeImage(width: Int, height: Int, pixels: IntArray, premultiplied: Boolean? = null): NativeImage =
    nativeImageFormatProvider.create(width, height, pixels, premultiplied)
fun NativeImage(width: Int, height: Int, pixels: RgbaArray): NativeImage =
    nativeImageFormatProvider.create(width, height, pixels.ints, premultiplied = false)
fun NativeImage(width: Int, height: Int, pixels: RgbaPremultipliedArray): NativeImage =
    nativeImageFormatProvider.create(width, height, pixels.ints, premultiplied = true)

fun NativeImage(
	width: Int,
	height: Int,
	d: Drawable,
	scaleX: Double = 1.0,
	scaleY: Double = scaleX
): NativeImage {
	val bmp = NativeImage(width, height)
	try {
		bmp.context2d {
            keep {
                scale(scaleX, scaleY)
                draw(d)
            }
        }
	} catch (e: Throwable) {
        imageLoadingLogger.error { e }
	}
	return bmp
}

fun NativeImage(d: SizedDrawable, scaleX: Double = 1.0, scaleY: Double = scaleX): NativeImage =
    NativeImage((d.width * scaleX).toInt(), (d.height * scaleY).toInt(), d, scaleX, scaleY)

fun Bitmap.ensureNative() = when (this) {
	is NativeImage -> this
	else -> nativeImageFormatProvider.copy(this)
}

fun SizedDrawable.raster(scaleX: Double = 1.0, scaleY: Double = scaleX) = NativeImage(this, scaleX, scaleY)
