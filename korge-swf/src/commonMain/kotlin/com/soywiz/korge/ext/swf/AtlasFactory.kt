package com.soywiz.korge.ext.swf

import com.soywiz.kds.Extra
import com.soywiz.korge.animate.*
import com.soywiz.korim.atlas.AtlasPacker
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*
import kotlin.collections.set

data class BitmapWithScale(val bitmap: Bitmap, val scale: Double, val bounds: Rectangle) : Extra by Extra.Mixin() {
	val width: Int = bitmap.width
	val height: Int = bitmap.height
}

/*
suspend fun List<BitmapWithScale>.toAtlas(context: AnLibrary.Context, mipmaps: Boolean): List<TextureWithBitmapSlice> {
	return this.map {
		TextureWithBitmapSlice(views.texture(it.bitmap), it.bitmap.slice(RectangleInt(0, 0, it.bitmap.width, it.bitmap.height)), it.scale)
	}
}
*/


suspend fun <T> Map<T, BitmapWithScale>.toAtlas(
    context: AnLibrary.Context,
    maxTextureSide: Int,
    mipmaps: Boolean,
    atlasPacking: Boolean
): Map<T, TextureWithBitmapSlice> {
    if (atlasPacking) {
        val atlas = AtlasPacker.pack(this.entries.toList().map { it to it.value.bitmap.slice() }, maxSide = maxTextureSide)
        val out = LinkedHashMap<T, TextureWithBitmapSlice>()
        //println("NUMBER OF ATLAS: ${atlas.atlases.map { "" + it.tex.width + "x" + it.tex.height  }}")
        for (at in atlas.atlases) {
            val texture = at.tex.slice()
            for (item in at.packedItems) {
                val ibmp = item.item.value
                val rect2 = item.rect
                out[item.item.key] = TextureWithBitmapSlice(
                    texture = texture.slice(rect2),
                    bitmapSlice = item.slice,
                    scale = ibmp.scale,
                    bounds = ibmp.bounds
                )
            }
        }
        return out
    } else {
        return this.entries.associate { it.key to TextureWithBitmapSlice(it.value.bitmap.slice(), it.value.bitmap.slice(), it.value.scale, it.value.bounds) }
    }
    //val borderSize = 4
    /*
    val borderSize = 8
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
     */
}
