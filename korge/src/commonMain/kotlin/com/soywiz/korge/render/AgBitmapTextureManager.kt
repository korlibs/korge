package com.soywiz.korge.render

import com.soywiz.kds.*
import com.soywiz.korag.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.Rectangle

var Bitmap.texMipmaps: Boolean by Extra.Property { false }
fun <T : Bitmap> T.mipmaps(enable: Boolean = true): T = this.apply { this.texMipmaps = enable }

class AgBitmapTextureManager(val ag: AG) {
	val referencedBitmapsSinceGC = LinkedHashSet<Bitmap>()
	var referencedBitmaps = setOf<Bitmap>()

	//var Bitmap._textureBase: Texture.Base? by Extra.Property { null }
	//var Bitmap._slices by Extra.Property { LinkedHashSet<BmpSlice>() }
	//var BmpSlice._texture: Texture? by Extra.Property { null }

	class BitmapTextureInfo(val textureBase: Texture.Base) {
		val slices = FastIdentityMap<BmpSlice, Texture>()
	}

	private val bitmapsToTextureBase = FastIdentityMap<Bitmap, BitmapTextureInfo>()

	private var cachedBitmap: Bitmap? = null
	private var cachedBitmapTextureInfo: BitmapTextureInfo? = null

	private var cachedBmpSlice: BmpSlice? = null
	private var cachedBmpSliceTexture: Texture? = null

	fun getTextureInfo(bitmap: Bitmap): BitmapTextureInfo {
		referencedBitmapsSinceGC += bitmap

		if (cachedBitmap == bitmap) return cachedBitmapTextureInfo!!

		val textureInfo = bitmapsToTextureBase.getOrPut(bitmap) { BitmapTextureInfo(Texture.Base(ag.createTexture(bitmap, bitmap.texMipmaps, bitmap.premultiplied), bitmap.width, bitmap.height)) }

		cachedBitmap = bitmap
		cachedBitmapTextureInfo = textureInfo

		return textureInfo
	}

	fun getTextureBase(bitmap: Bitmap): Texture.Base = getTextureInfo(bitmap).textureBase

	fun getTexture(slice: BmpSlice): Texture {
		referencedBitmapsSinceGC += slice.bmp

		if (cachedBmpSlice == slice) return cachedBmpSliceTexture!!

		val info = getTextureInfo(slice.bmp)

		val texture = info.slices.getOrPut(slice) { Texture(info.textureBase).slice(Rectangle(slice.left, slice.top, slice.width, slice.height)) }

		cachedBmpSlice = slice
		cachedBmpSliceTexture = texture

		return texture
	}

	var fcount = 0
	fun afterRender() {
		fcount++
		if (fcount >= 60) {
			fcount = 0
			gc()
		}
		// Prevent leaks when not referenced anymore
		cachedBitmap = null
		cachedBitmapTextureInfo = null
		cachedBmpSlice = null
		cachedBmpSliceTexture = null
	}

	fun gc() {
		val toRemove = referencedBitmaps - referencedBitmapsSinceGC
		for (bmp in toRemove) {
			val info = bitmapsToTextureBase.getAndRemove(bmp)
			info?.textureBase?.close()
		}
		referencedBitmaps = referencedBitmapsSinceGC.toSet()
	}

	private fun <K, V> FastIdentityMap<K, V>.getAndRemove(key: K): V? {
		return get(key).also { remove(key) }
	}
}
