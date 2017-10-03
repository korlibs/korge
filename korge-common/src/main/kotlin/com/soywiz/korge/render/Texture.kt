package com.soywiz.korge.render

import com.soywiz.korag.AG
import com.soywiz.korge.resources.Path
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.view.Views
import com.soywiz.korim.format.readBitmapOptimized
import com.soywiz.korio.inject.AsyncFactory
import com.soywiz.korio.inject.AsyncFactoryClass
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.lang.JvmField
import com.soywiz.korio.util.clamp
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.numeric.isPowerOfTwo

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(TextureAsyncFactory::class)
class Texture(val base: Base, val left: Int = 0, val top: Int = 0, val right: Int = base.width, val bottom: Int = base.height) : Closeable {
	val x = left
	val y = top
	val width = right - left
	val height = bottom - top

	@JvmField
	val x0: Float = (left).toFloat() / base.width.toFloat()
	@JvmField
	val x1: Float = (right).toFloat() / base.width.toFloat()
	@JvmField
	val y0: Float = (top).toFloat() / base.height.toFloat()
	@JvmField
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
		operator fun invoke(agBase: AG.Texture, width: Int, height: Int): Texture = Texture(Base(agBase, width, height), 0, 0, width, height)
		operator fun invoke(rtex: AG.RenderTexture): Texture = Texture(Base(rtex.tex, rtex.width, rtex.height), 0, 0, rtex.width, rtex.height)
	}

	class Base(val base: AG.Texture, val width: Int, val height: Int): Closeable {
		override fun close() = base.close()
	}

	override fun close() = base.close()
}

suspend fun VfsFile.readTexture(views: Views, mipmaps: Boolean = true): Texture = readTexture(views.ag, mipmaps)

suspend fun VfsFile.readTexture(ag: AG, mipmaps: Boolean = true): Texture {
	//println("VfsFile.readTexture[1]")
	val tex = ag.createTexture()
	//println("VfsFile.readTexture[2]")
	val bmp = this.readBitmapOptimized()
	//println("VfsFile.readTexture[3]")
	val canHasMipmaps = bmp.width.isPowerOfTwo && bmp.height.isPowerOfTwo
	//println("VfsFile.readTexture[4]")
	tex.upload(bmp, mipmaps = canHasMipmaps && mipmaps)
	//println("VfsFile.readTexture[5]")
	return Texture(tex, bmp.width, bmp.height)
}

//@JTranscKeep
class TextureAsyncFactory(
	private val ag: AG,
	private val resourcesRoot: ResourcesRoot,
	private val path: Path
) : AsyncFactory<Texture> {
	override suspend fun create() = resourcesRoot[path].readTexture(ag)
}
