package korlibs.korge.render

import korlibs.graphics.*
import korlibs.image.bitmap.*
import korlibs.io.lang.*
import korlibs.math.geom.*
import korlibs.math.geom.slice.*

/**
 * Represents a full texture region wraping a [base] [AGTexture] and specifying its [width] and [height]
 */
class TextureBase(
    var base: AGTexture?,
    override var size: SizeInt,
) : Closeable, SizeableInt {
    constructor(base: AGTexture?, width: Int, height: Int) : this(base, SizeInt(width, height))
    var width: Int get() = size.width; set(width) { size = SizeInt(width, height) }
    var height: Int get() = size.height; set(height) { size = SizeInt(width, height) }

    var version = -1
    override fun close() {
        base?.close()
        base = null
    }
    fun update(bmp: Bitmap, mipmaps: Boolean, baseMipmapLevel: Int?, maxMipmapLevel: Int?) {
        base?.upload(bmp, mipmaps, baseMipmapLevel, maxMipmapLevel)
    }

    override fun toString(): String = "TextureBase($base)"
}

typealias TextureCoords = SliceCoordsWithBase<TextureBase>
typealias Texture = RectSlice<TextureBase>

/**
 * Updates this texture from a [bmp] and optionally generates [mipmaps].
 */
fun Texture.update(bmp: Bitmap32, mipmaps: Boolean, baseMipmapLevel: Int, maxMipmapLevel: Int) {
    container.update(bmp, mipmaps, baseMipmapLevel, maxMipmapLevel)
}

//fun TextureNew.xcoord(x: Int): Float = (this.rect.x + x).toFloat() / width.toFloat()
//fun TextureNew.ycoord(y: Int): Float = (this.rect.y + y).toFloat() / height.toFloat()

fun Texture.close() {
    container.close()
}

/**
 * Creates a [Texture] from a texture [agBase] and its wanted size [width], [height].
 */
fun Texture(agBase: AGTexture, width: Int, height: Int): Texture =
    Texture(TextureBase(agBase, SizeInt(width, height)), RectangleInt(0, 0, width, height))

fun Texture(base: TextureBase, left: Int = 0, top: Int = 0, right: Int = base.width, bottom: Int = base.height): Texture =
    Texture(base, RectangleInt.fromBounds(left, top, right, bottom))

/**
 * Creates a [Texture] from a frame buffer [frameBuffer] with the right size of the frameBuffer.
 */
fun Texture(frameBuffer: AGFrameBuffer): Texture = Texture(frameBuffer.tex, frameBuffer.width, frameBuffer.height)

/*
/**
 * A [Texture] is a region (delimited by [left], [top], [right] and [bottom]) of a [Texture.Base].
 * A [Texture.Base] wraps a [AGTexture] but adds [width] and [height] information.
 */
class Texture(
	override val base: TextureBase,
    /** Left position of the region of the texture in pixels */
	override val left: Int = 0,
    /** Top position of the region of the texture in pixels */
	override val top: Int = 0,
    /** Right position of the region of the texture in pixels */
	val right: Int = base.width,
    /** Bottom position of the region of the texture in pixels */
	val bottom: Int = base.height
) : Closeable, TextureCoords, NewTextureCoords {

    //override val container: TextureBase get() = base

    /** Whether the texture is multiplied or not */
	override val premultiplied get() = base.premultiplied
    /** Left position of the region of the texture in pixels */
	val x: Int get() = left
    /** Top position of the region of the texture in pixels */
	val y: Int get() = top
    /** Width of this texture region in pixels */
    override val width: Int get() = right - left
    /** Height of this texture region in pixels */
    override val height: Int get() = bottom - top

    /** Left coord of the texture region as a ratio (a value between 0 and 1) */
	val x0: Float = (left).toFloat() / base.width.toFloat()
    /** Right coord of the texture region as a ratio (a value between 0 and 1) */
	val x1: Float = (right).toFloat() / base.width.toFloat()
    /** Top coord of the texture region as a ratio (a value between 0 and 1) */
	val y0: Float = (top).toFloat() / base.height.toFloat()
    /** Bottom coord of the texture region as a ratio (a value between 0 and 1) */
	val y1: Float = (bottom).toFloat() / base.height.toFloat()

    override val tlX get() = x0
    override val tlY get() = y0

    override val trX get() = x1
    override val trY get() = y0

    override val blX get() = x0
    override val blY get() = y1

    override val brX get() = x1
    override val brY get() = y1

    /**
     * Creates a slice of this texture, by [x], [y], [width] and [height].
     */
	fun slice(x: Int, y: Int, width: Int, height: Int): Texture = sliceBounds(x, y, x + width, y + height)

    /**
     * Createa a slice of this texture by [rect].
     */
	fun slice(rect: Rectangle): Texture = slice(rect.x.toInt(), rect.y.toInt(), rect.width.toInt(), rect.height.toInt())

    /**
     * Creates a slice of this texture by its bounds [left], [top], [right], [bottom].
     */
	fun sliceBounds(left: Int, top: Int, right: Int, bottom: Int): Texture {
		val tleft = (this.x + left).clamp(this.left, this.right)
		val tright = (this.x + right).clamp(this.left, this.right)
		val ttop = (this.y + top).clamp(this.top, this.bottom)
		val tbottom = (this.y + bottom).clamp(this.top, this.bottom)
		return Texture(base, tleft, ttop, tright, tbottom)
	}

    fun sliceBoundsUnclamped(left: Int, top: Int, right: Int, bottom: Int): Texture {
        val tleft = (this.x + left)
        val tright = (this.x + right)
        val ttop = (this.y + top)
        val tbottom = (this.y + bottom)
        return Texture(base, tleft, ttop, tright, tbottom)
    }

	companion object {
        /**
         * Creates a [Texture] from a texture [agBase] and its wanted size [width], [height].
         */
		operator fun invoke(agBase: AGTexture, width: Int, height: Int): Texture =
			Texture(TextureBase(agBase, width, height), 0, 0, width, height)

        /**
         * Creates a [Texture] from a frame buffer [frameBuffer] with the right size of the frameBuffer.
         */
        operator fun invoke(frameBuffer: AGFrameBuffer): Texture = invoke(frameBuffer.tex, frameBuffer.width, frameBuffer.height)
	}

    /**
     * Updates this texture from a [bmp] and optionally generates [mipmaps].
     */
	fun update(bmp: Bitmap32, mipmaps: Boolean = false) {
		base.update(bmp, mipmaps)
	}

    /**
     * Closes the texture
     */
	override fun close() = base.close()

    override fun toString(): String = "Texture($base, (x=$x, y=$y, width=$width, height=$height))"

    fun xcoord(x: Int): Float = (this.x + x).toFloat() / base.width.toFloat()
    fun ycoord(y: Int): Float = (this.y + y).toFloat() / base.height.toFloat()
}
*/

//suspend fun VfsFile.readTexture(ag: AG, imageFormats: ImageFormats, mipmaps: Boolean = true): Texture {
//	//println("VfsFile.readTexture[1]")
//	val tex = ag.createTexture()
//	//println("VfsFile.readTexture[2]")
//	val bmp = this.readBitmapOptimized(imageFormats)
//	//val bmp = this.readBitmapNoNative()
//	//println("VfsFile.readTexture[3]")
//	val canHasMipmaps = bmp.width.isPowerOfTwo && bmp.height.isPowerOfTwo
//	//println("VfsFile.readTexture[4]")
//	tex.upload(bmp, mipmaps = canHasMipmaps && mipmaps)
//	//println("VfsFile.readTexture[5]")
//	return Texture(tex, bmp.width, bmp.height)
//}