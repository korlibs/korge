package com.soywiz.korge.ext.swf

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.binpack.*
import kotlin.collections.set

data class BitmapWithScale(val bitmap: Bitmap, val scale: Double, val bounds: Rectangle) : Extra by Extra.Mixin() {
	val width: Int = bitmap.width
	val height: Int = bitmap.height
}

/*
suspend fun List<BitmapWithScale>.toAtlas(views: Views, mipmaps: Boolean): List<TextureWithBitmapSlice> {
	return this.map {
		TextureWithBitmapSlice(views.texture(it.bitmap), it.bitmap.slice(RectangleInt(0, 0, it.bitmap.width, it.bitmap.height)), it.scale)
	}
}
*/


suspend fun <T> Map<T, BitmapWithScale>.toAtlas(
	views: Views,
	maxTextureSide: Int,
	mipmaps: Boolean
): Map<T, TextureWithBitmapSlice> {
	//val packs = BinPacker.packSeveral(2048.0, 2048.0, this) { Size(it.width + 4, it.height + 4) }
	val values = this.values.toList()
	val packs = BinPacker.packSeveral(
		maxTextureSide.toDouble(),
		maxTextureSide.toDouble(),
		values
	) { Size((it.width + 4).nextAlignedTo(4), (it.height + 4).nextAlignedTo(4)) }
	val bitmapsToTextures = hashMapOf<BitmapWithScale, TextureWithBitmapSlice>()
	val premultiplied = this.values.firstOrNull()?.bitmap?.premultiplied ?: true
	for (pack in packs) {
		val width = pack.width.toInt().nextPowerOfTwo
		val height = pack.height.toInt().nextPowerOfTwo
		val bmp = Bitmap32(width, height, premultiplied = premultiplied)
		for ((ibmp, rect) in pack.items) {
			val r = rect ?: continue
			val dx = r.x.toInt() + 2
			val dy = r.y.toInt() + 2

			bmp.put(ibmp.bitmap.toBMP32(), dx, dy)

			//val dwidth = rect.width.toInt()
			//val dheight = rect.height.toInt()
			//val dxr = dx + width - 1
			//Bitmap32.copyRect(bmp, dx, dy, bmp, dx - 1, dy, 1, dheight)
			//Bitmap32.copyRect(bmp, dx, dy, bmp, dx - 2, dy, 1, dheight)
			//Bitmap32.copyRect(bmp, dxr, dy, bmp, dxr + 1, dy, 1, dheight)
			//Bitmap32.copyRect(bmp, dxr, dy, bmp, dxr + 2, dy, 1, dheight)
		}

		val texture = bmp.slice()

		for ((ibmp, rect) in pack.items) {
			val r = rect ?: continue
			val rect2 = Rectangle(r.x + 2, r.y + 2, r.width - 4, r.height - 4)
			bitmapsToTextures[ibmp] = TextureWithBitmapSlice(
				texture = texture.slice(rect2),
				bitmapSlice = bmp.slice(rect2.toInt()),
				scale = ibmp.scale,
				bounds = ibmp.bounds
			)
		}
	}
	return this.mapValues { bitmapsToTextures[it.value]!! }
}
