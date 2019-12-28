package com.soywiz.korge.render

import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*

/**
 * A [Texture] is a region (delimited by [left], [top], [right] and [bottom]) of a [Texture.Base].
 * A [Texture.Base] wraps a [AG.Texture] but adds [width] and [height] information.
 */
class Texture(
	val base: Base,
    /** Left position of the region of the texture in pixels */
	val left: Int = 0,
    /** Top position of the region of the texture in pixels */
	val top: Int = 0,
    /** Right position of the region of the texture in pixels */
	val right: Int = base.width,
    /** Bottom position of the region of the texture in pixels */
	val bottom: Int = base.height
) : Closeable {
    /** Wether the texture is multiplied or not */
	val premultiplied get() = base.premultiplied
    /** Left position of the region of the texture in pixels */
	val x = left
    /** Top position of the region of the texture in pixels */
	val y = top
    /** Width of this texture region in pixels */
	val width = right - left
    /** Height of this texture region in pixels */
	val height = bottom - top

    /** Left coord of the texture region as a ratio (a value between 0 and 1) */
	val x0: Float = (left).toFloat() / base.width.toFloat()
    /** Right coord of the texture region as a ratio (a value between 0 and 1) */
	val x1: Float = (right).toFloat() / base.width.toFloat()
    /** Top coord of the texture region as a ratio (a value between 0 and 1) */
	val y0: Float = (top).toFloat() / base.height.toFloat()
    /** Bottom coord of the texture region as a ratio (a value between 0 and 1) */
	val y1: Float = (bottom).toFloat() / base.height.toFloat()

    /**
     * Creates a slice of this texture, by [x], [y], [width] and [height].
     */
	fun slice(x: Int, y: Int, width: Int, height: Int) = sliceBounds(x, y, x + width, y + height)

    /**
     * Createa a slice of this texture by [rect].
     */
	fun slice(rect: Rectangle) = slice(rect.x.toInt(), rect.y.toInt(), rect.width.toInt(), rect.height.toInt())

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

	companion object {
        /**
         * Creates a [Texture] from a texture [agBase] and its wanted size [width], [height].
         */
		operator fun invoke(agBase: AG.Texture, width: Int, height: Int): Texture =
			Texture(Base(agBase, width, height), 0, 0, width, height)
	}

    /**
     * Represents a full texture region wraping a [base] [AG.Texture] and specifying its [width] and [height]
     */
	class Base(val base: AG.Texture, val width: Int, val height: Int) : Closeable {
		val premultiplied get() = base.premultiplied
		override fun close() = base.close()
		fun update(bmp: Bitmap32, mipmaps: Boolean = false) {
			base.upload(bmp, mipmaps)
		}
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
}

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
