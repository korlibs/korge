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

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(TextureAsyncFactory::class)
class Texture(
	val base: Base,
	val left: Int = 0,
	val top: Int = 0,
	val right: Int = base.width,
	val bottom: Int = base.height
) : Closeable {
	val premultiplied get() = base.premultiplied
	val x = left
	val y = top
	val width = right - left
	val height = bottom - top

	val x0: Float = (left).toFloat() / base.width.toFloat()
	val x1: Float = (right).toFloat() / base.width.toFloat()
	val y0: Float = (top).toFloat() / base.height.toFloat()
	val y1: Float = (bottom).toFloat() / base.height.toFloat()

	override fun toString(): String = "Texture($base, (x=$x, y=$y, width=$width, height=$height))"

	fun slice(x: Int, y: Int, width: Int, height: Int) = sliceBounds(x, y, x + width, y + height)

	fun slice(rect: Rectangle) = slice(rect.x.toInt(), rect.y.toInt(), rect.width.toInt(), rect.height.toInt())

	fun sliceBounds(left: Int, top: Int, right: Int, bottom: Int): Texture {
		val tleft = (this.x + left).clamp(this.left, this.right)
		val tright = (this.x + right).clamp(this.left, this.right)
		val ttop = (this.y + top).clamp(this.top, this.bottom)
		val tbottom = (this.y + bottom).clamp(this.top, this.bottom)
		return Texture(base, tleft, ttop, tright, tbottom)
	}

	companion object {
		operator fun invoke(agBase: AG.Texture, width: Int, height: Int): Texture =
			Texture(Base(agBase, width, height), 0, 0, width, height)
	}

	class Base(val base: AG.Texture, val width: Int, val height: Int) : Closeable {
		val premultiplied get() = base.premultiplied
		override fun close() = base.close()
		fun update(bmp: Bitmap32, mipmaps: Boolean = false) {
			base.upload(bmp, mipmaps)
		}
	}

	fun update(bmp: Bitmap32, mipmaps: Boolean = false) {
		base.update(bmp, mipmaps)
	}

	override fun close() = base.close()
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
