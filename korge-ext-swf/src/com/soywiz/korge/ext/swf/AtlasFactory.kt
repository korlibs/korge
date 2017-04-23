package com.soywiz.korge.ext.swf

import com.soywiz.korge.render.TextureWithBitmapSlice
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.texture
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.slice
import com.soywiz.korma.geom.RectangleInt
import com.soywiz.korma.geom.Size
import com.soywiz.korma.geom.binpack.BinPacker
import com.soywiz.korma.numeric.nextPowerOfTwo

data class BitmapWithScale(val bitmap: Bitmap, val scale: Double) {
	val width: Int = bitmap.width
	val height: Int = bitmap.height
}

//suspend fun List<BitmapWithScale>.toAtlas(views: Views): List<TextureWithBitmapSlice> {
//	return this.map {
//		TextureWithBitmapSlice(views.texture(it.bitmap), it.bitmap.slice(RectangleInt(0, 0, it.bitmap.width, it.bitmap.height)), it.scale)
//	}
//}


suspend fun List<BitmapWithScale>.toAtlas(views: Views): List<TextureWithBitmapSlice> {
	//val packs = BinPacker.packSeveral(2048.0, 2048.0, this) { Size(it.width + 4, it.height + 4) }
	val packs = BinPacker.packSeveral(4096.0, 4096.0, this) { Size(it.width + 4, it.height + 4) }
	val bitmapsToTextures = hashMapOf<BitmapWithScale, TextureWithBitmapSlice>()
	for (pack in packs) {
		val width = pack.width.toInt().nextPowerOfTwo
		val height = pack.height.toInt().nextPowerOfTwo
		val bmp = Bitmap32(width, height)
		for ((ibmp, rect) in pack.items) {
			bmp.put(ibmp.bitmap.toBMP32(), rect.x.toInt() + 2, rect.y.toInt() + 2)
		}
		//showImageAndWait(bmp)
		val texture = views.texture(bmp, mipmaps = false)
		for ((ibmp, rect) in pack.items) {
			val rect2 = rect.copy(rect.x + 2, rect.y + 2)
			bitmapsToTextures[ibmp] = TextureWithBitmapSlice(texture.slice(rect2), bmp.slice(rect2.toInt()), scale = ibmp.scale)
		}
	}
	return this.map { bitmapsToTextures[it]!! }
}

