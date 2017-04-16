package com.soywiz.korge.ext.swf

import com.soywiz.korge.render.TextureWithBitmapSlice
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.texture
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.slice
import com.soywiz.korma.geom.Size
import com.soywiz.korma.geom.binpack.BinPacker
import com.soywiz.korma.numeric.nextPowerOfTwo

suspend fun List<Bitmap>.toAtlas(views: Views): List<TextureWithBitmapSlice> {
	val packs = BinPacker.packSeveral(2048.0, 2048.0, this) { Size(it.width + 4, it.height + 4) }
	val bitmapsToTextures = hashMapOf<Bitmap, TextureWithBitmapSlice>()
	for (pack in packs) {
		val width = pack.width.toInt().nextPowerOfTwo
		val height = pack.height.toInt().nextPowerOfTwo
		val bmp = Bitmap32(width, height)
		for ((ibmp, rect) in pack.items) {
			bmp.put(ibmp.toBMP32(), rect.x.toInt() + 2, rect.y.toInt() + 2)
		}
		//showImageAndWait(bmp)
		val texture = views.texture(bmp, mipmaps = false)
		for ((ibmp, rect) in pack.items) {
			val rect2 = rect.copy(rect.x + 2, rect.y + 2)
			bitmapsToTextures[ibmp] = TextureWithBitmapSlice(texture.slice(rect2), bmp.slice(rect2.toInt()))
		}
	}
	return this.map { bitmapsToTextures[it]!! }
}
