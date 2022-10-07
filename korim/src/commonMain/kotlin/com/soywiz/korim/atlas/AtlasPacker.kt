package com.soywiz.korim.atlas

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kmem.nextPowerOfTwo
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.BitmapSlice
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.bitmap.extract
import com.soywiz.korim.bitmap.slice
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.RectangleInt
import com.soywiz.korma.geom.Size
import com.soywiz.korma.geom.binpack.BinPacker
import kotlin.jvm.JvmName

object AtlasPacker {
    data class Entry<T>(
        val item: T,
        val originalSlice: BmpSlice,
        val slice: BitmapSlice<Bitmap32>,
        val rectWithBorder: Rectangle,
        val rect: Rectangle
    )

    data class AtlasResult<T>(val tex: Bitmap32, val atlas: Atlas, val packedItems: List<Entry<T>>) : AtlasLookup {
        val packedItemsByItem = packedItems.associateBy { it.item }

        fun tryGetEntryByKey(key: T) = packedItemsByItem[key]
        override fun tryGetEntryByName(name: String): Atlas.Entry? = atlas.tryGetEntryByName(name)

        val atlasInfo: AtlasInfo get() = atlas.info
    }

    data class Result<T>(val atlases: List<AtlasResult<T>>) : AtlasLookup {
        fun tryGetEntryByKey(key: T): Entry<T>? {
            atlases.fastForEach {
                val result = it.tryGetEntryByKey(key)
                if (result != null) return result
            }
            return null
        }

        override fun tryGetEntryByName(name: String): Atlas.Entry? {
            atlases.fastForEach {
                val result = it.tryGetEntryByName(name)
                if (result != null) return result
            }
            return null
        }
    }

    fun pack(
        items: List<BmpSlice>,
        maxSide: Int = 2048,
        maxTextures: Int = 1,
        borderSize: Int = 2,
        fileName: String = "atlas.png",
        premultiplied: Boolean = true,
    ): Result<BmpSlice> = pack(
        items.map { it to it },
        maxSide, maxTextures, borderSize, fileName,
        premultiplied = premultiplied
    )

    fun expandBitmapBorder(out: Bitmap32, rect: RectangleInt, borderSize: Int) {
        val borderSize2 = borderSize * 2
        val x0 = rect.left
        val y0 = rect.top
        val width = rect.width
        val height = rect.height
        val x1 = x0 + width - 1
        val y1 = y0 + height - 1
        for (i in 1..borderSize) {
            Bitmap32.copyRect(out, x0, y0, out, x0 - i, y0, 1, height)
            Bitmap32.copyRect(out, x1, y0, out, x1 + i, y0, 1, height)
        }
        for (i in 1..borderSize) {
            Bitmap32.copyRect(out, x0 - borderSize, y0, out, x0 - borderSize, y0 - i, width + borderSize2, 1)
            Bitmap32.copyRect(out, x0 - borderSize, y1, out, x0 - borderSize, y1 + i, width + borderSize2, 1)
        }
    }

    @JvmName("packPairs")
    fun <T> pack(
        items: List<Pair<T, BmpSlice>>,
        maxSide: Int = 2048,
        maxTextures: Int = 16,
        borderSize: Int = 2,
        fileName: String = "atlas.png",
        premultiplied: Boolean = true,
    ): Result<T> {
        val borderSize2 = borderSize * 2
        val packs = BinPacker.packSeveral(maxSide.toDouble(), maxSide.toDouble(), items) {
            //println("it.second.width=${it.second.width}, borderSize2=$borderSize2")
            Size(it.second.width + borderSize2, it.second.height + borderSize2)
        }
        if (packs.size > maxTextures) error("textures:${packs.size} > maxTextures:${maxTextures}")
        return Result(packs.map { pack ->
            val out = Bitmap32(pack.width.toInt().nextPowerOfTwo, pack.height.toInt().nextPowerOfTwo, premultiplied = premultiplied)
            val packedItems = arrayListOf<Entry<T>>()
            for ((item, rectOrNull) in pack.items) {
                val rectWithBorder = rectOrNull ?: Rectangle(0, 0, 1, 1)
                val width = item.second.width
                val height = item.second.height
                val x0 = rectWithBorder.x.toInt() + borderSize
                val y0 = rectWithBorder.y.toInt() + borderSize
                val rect = Rectangle(x0, y0, width, height)
                val bmp2 = item.second.copyWith(virtFrame = null).extract().toBMP32IfRequired()
                val bmp = if (premultiplied) bmp2.premultipliedIfRequired() else bmp2.depremultipliedIfRequired()
                out.draw(bmp, x0, y0)
                val colorRect = RectangleInt(x0, y0, width, height)
                //println("colorRect=$colorRect, x0=$x0, y0=$y0, bmp=${bmp.width}x${bmp.height}")
                expandBitmapBorder(bmp, colorRect, borderSize)
                packedItems.add(Entry(
                    item = item.first,
                    originalSlice = item.second,
                    slice = out.slice(rect.toInt()),
                    rectWithBorder = rectWithBorder,
                    rect = rect,
                ))
            }
            val atlasInfo = AtlasInfo(
                frames = packedItems.map {
                    val filename = it.originalSlice.name
                    AtlasInfo.Region(
                        name = filename ?: "unknown",
                        frame = AtlasInfo.Rect(it.rect),
                        virtFrame = it.originalSlice.virtFrame?.let { AtlasInfo.Rect(it) },
                    )
                },
                meta = AtlasInfo.Meta(
                    app = "korge",
                    format = "RGBA8888",
                    image = fileName,
                    scale = 1.0,
                    size = AtlasInfo.Size(out.width, out.height),
                    version = AtlasInfo.Meta.VERSION
                )
            )
            AtlasResult(out, Atlas(out.slice(), atlasInfo), packedItems)
        })
    }
}

/*

ATLAS EXAMPLE:

two 512x512 trimmed images

{"frames": {
	"atlas1.png":{
		"rotated": false,"trimmed": true,
		"frame": {"x":0,"y":381,"w":376,"h":379},
		"spriteSourceSize": {"x":74,"y":71,"w":512,"h":512},
		"sourceSize": {"w":512,"h":512}
	},
	"atlas2.png":{
		"rotated": false,"trimmed": true,
		"frame": {"x":0,"y":0,"w":376,"h":379},
		"spriteSourceSize": {"x":74,"y":71,"w":512,"h":512},
		"sourceSize": {"w":512,"h":512}
	}
},
"meta": {
	"app": "ShoeBox",
	"size": {"w":1024,"h":1024}
}
}

 */
