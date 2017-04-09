package com.soywiz.korge.ext.swf

import com.soywiz.korge.render.TextureWithBitmapSlice
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.texture
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.slice
import com.soywiz.korma.geom.binpack.BinPacker
import com.soywiz.korma.numeric.nextPowerOfTwo

// @TODO: Add borders to avoid scaling issues + align to 4 pixels to avoid compression artifacts
suspend fun List<Bitmap>.toAtlas(views: Views): List<TextureWithBitmapSlice> {
	val packs = BinPacker.packSeveral(2048, 2048, this)
	val bitmapsToTextures = hashMapOf<Bitmap, TextureWithBitmapSlice>()
	for (pack in packs) {
		val width = pack.width.toInt().nextPowerOfTwo
		val height = pack.height.toInt().nextPowerOfTwo
		val bmp = Bitmap32(width, height)
		for ((ibmp, rect) in pack.items) {
			bmp.put(ibmp.toBMP32(), rect.x.toInt(), rect.y.toInt())
		}
		//showImageAndWait(bmp)
		val texture = views.texture(bmp, mipmaps = false)
		for ((ibmp, rect) in pack.items) {
			bitmapsToTextures[ibmp] = TextureWithBitmapSlice(texture.slice(rect), bmp.slice(rect.toInt()))
		}
	}
	return this.map { bitmapsToTextures[it]!! }
}
