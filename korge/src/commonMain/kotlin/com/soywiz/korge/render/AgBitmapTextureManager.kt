package com.soywiz.korge.render

import com.soywiz.kds.*
import com.soywiz.korag.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*

var Bitmap.texMipmaps: Boolean by Extra.Property { false }
fun <T : Bitmap> T.mipmaps(enable: Boolean = true): T = this.apply { this.texMipmaps = enable }

class AgBitmapTextureManager(val ag: AG) {
	val referencedBitmapsSinceGC = LinkedHashSet<Bitmap>()
	var referencedBitmaps = setOf<Bitmap>()

	var Bitmap._textureBase: Texture.Base? by Extra.Property { null }
	var Bitmap._slices by Extra.Property { LinkedHashSet<BmpSlice>() }
	var BmpSlice._texture: Texture? by Extra.Property { null }

	fun getTextureBase(bitmap: Bitmap): Texture.Base {
		referencedBitmapsSinceGC += bitmap
		if (bitmap._textureBase == null) {
			bitmap._textureBase = Texture.Base(ag.createTexture(bitmap, bitmap.texMipmaps, bitmap.premult), bitmap.width, bitmap.height)
		}
		return bitmap._textureBase!!
	}

	fun getTexture(slice: BmpSlice): Texture {
		referencedBitmapsSinceGC += slice.bmp
		slice.bmp._slices.add(slice)

		if (slice._texture == null) {
			slice._texture = Texture(getTextureBase(slice.bmp)).slice(Rectangle(slice.left, slice.top, slice.width, slice.height))
		}
		return slice._texture!!
	}

	var fcount = 0
	fun afterRender() {
		fcount++
		if (fcount >= 60) {
			fcount = 0
			gc()
		}
	}

	fun gc() {
		val toRemove = referencedBitmaps - referencedBitmapsSinceGC
		for (bmp in toRemove) {
			for (slice in bmp._slices) {
				slice._texture = null
			}

			bmp._textureBase?.close()
			bmp._textureBase = null
		}
		referencedBitmaps = referencedBitmapsSinceGC.toSet()
	}
}
