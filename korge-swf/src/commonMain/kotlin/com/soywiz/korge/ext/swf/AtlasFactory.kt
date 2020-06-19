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
    val borderSize = 4
    val alignTo = 8
    val borderSize2 = borderSize * 2

    //val packs = BinPacker.packSeveral(2048.0, 2048.0, this) { Size(it.width + 4, it.height + 4) }
	val values = this.values.toList()
	val packs = BinPacker.packSeveral(
		maxTextureSide.toDouble(),
		maxTextureSide.toDouble(),
		values
	) { Size((it.width + borderSize2).nextAlignedTo(alignTo), (it.height + borderSize2).nextAlignedTo(alignTo)) }
	val bitmapsToTextures = hashMapOf<BitmapWithScale, TextureWithBitmapSlice>()
	val premultiplied = this.values.firstOrNull()?.bitmap?.premultiplied ?: true
	for (pack in packs) {
		val atlasWidth = pack.width.toInt().nextPowerOfTwo
		val atlasHeight = pack.height.toInt().nextPowerOfTwo
		val bmp = Bitmap32(atlasWidth, atlasHeight, premultiplied = premultiplied)
		for ((ibmp, rect) in pack.items) {
			val r = rect ?: continue
            val width = ibmp.width
            val height = ibmp.height
			val dx0 = r.x.toInt() + borderSize
			val dy0 = r.y.toInt() + borderSize
            val dx1 = dx0 + width - 1
            val dy1 = dy0 + height - 1

			bmp.put(ibmp.bitmap.toBMP32(), dx0, dy0)

            //println("dx=$dx0,dy=$dy0 - $dx1,$dy1 -- $width, $height, [[${bmp.width}x${bmp.height}]]")

            val alphaThresold = 0.75

            for (y in 0 until height) {
                val py = dy0 + y
                val v0 = bmp[dx0, py]
                val v1 = bmp[dx1, py]
                if (v0.ad >= alphaThresold) for (i in 1..borderSize) bmp[dx0 - i, py] = v0
                if (v1.ad >= alphaThresold) for (i in 1..borderSize) bmp[dx1 + i, py] = v1
            }

            for (x in -2 until width + 2) {
                val px = dx0 + x
                val v0 = bmp[px, dy0]
                val v1 = bmp[px, dy1]
                if (v0.ad >= alphaThresold) for (i in 1..borderSize) bmp[px, dy0 - i] = v0
                if (v1.ad >= alphaThresold) for (i in 1..borderSize) bmp[px, dy1 + i] = v1
            }
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
