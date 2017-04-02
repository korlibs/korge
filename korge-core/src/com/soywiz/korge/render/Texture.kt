package com.soywiz.korge.render

import com.jtransc.annotation.JTranscKeep
import com.soywiz.korag.AG
import com.soywiz.korge.resources.Path
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.inject.AsyncFactory
import com.soywiz.korio.inject.AsyncFactoryClass
import com.soywiz.korio.util.clamp
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korma.geom.Rectangle

@AsyncFactoryClass(TextureAsyncFactory::class)
class Texture(val base: Base, val left: Int = 0, val top: Int = 0, val right: Int = base.width, val bottom: Int = base.height) {
    val x = left
    val y = top
    val width = right - left
    val height = bottom - top

    val x0: Float = (left).toFloat() / base.width.toFloat()
    val x1: Float = (right).toFloat() / base.width.toFloat()
    val y0: Float = (top).toFloat() / base.height.toFloat()
    val y1: Float = (bottom).toFloat() / base.height.toFloat()

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
    }

    class Base(val base: AG.Texture, val width: Int, val height: Int)
}

suspend fun VfsFile.readTexture(ag: AG, mipmaps: Boolean = true): Texture {
    val tex = ag.createTexture()
    val bmp = this.readBitmap()
    tex.upload(bmp, mipmaps = mipmaps)
    return Texture(tex, bmp.width, bmp.height)
}

@JTranscKeep
class TextureAsyncFactory(
        private val ag: AG,
        private val path: Path
) : AsyncFactory<Texture> {
    override suspend fun create() = ResourcesVfs[path.path].readTexture(ag)
}
