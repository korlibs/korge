package korlibs.image.atlas

import korlibs.datastructure.iterators.fastForEach
import korlibs.math.nextPowerOfTwo
import korlibs.image.bitmap.*
import korlibs.math.geom.*
import korlibs.math.geom.binpack.BinPacker
import kotlin.jvm.JvmName

object AtlasPacker {
    data class Entry<T>(val item: T, val originalSlice: BmpSlice, val slice: BmpSlice32, val rectWithBorder: Rectangle, val rect: Rectangle)

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

    fun pack(items: List<BmpSlice>, maxSide: Int = 2048, maxTextures: Int = 1, borderSize: Int = 2, fileName: String = "atlas.png"): Result<BmpSlice> =
        pack(items.map { it to it }, maxSide, maxTextures, borderSize, fileName)

    @JvmName("packPairs")
    fun <T> pack(items: List<Pair<T, BmpSlice>>, maxSide: Int = 2048, maxTextures: Int = 16, borderSize: Int = 2, fileName: String = "atlas.png"): Result<T> {
        val borderSize2 = borderSize * 2
        val packs = BinPacker.packSeveral(Size(maxSide, maxSide), items) {
            Size(it.second.width + borderSize2, it.second.height + borderSize2)
        }
        if (packs.size > maxTextures) error("textures:${packs.size} > maxTextures:${maxTextures}")
        return Result(packs.map { pack ->
            val out = Bitmap32(pack.width.toInt().nextPowerOfTwo, pack.height.toInt().nextPowerOfTwo, premultiplied = true)
            val packedItems = arrayListOf<Entry<T>>()
            for ((item, rectOrNull) in pack.items) {
                val rectWithBorder = rectOrNull ?: Rectangle(0, 0, 1, 1)
                val width = item.second.width
                val height = item.second.height
                val x0 = rectWithBorder.x.toInt() + borderSize
                val y0 = rectWithBorder.y.toInt() + borderSize
                val rect = Rectangle(x0, y0, width, height)
                val x1 = x0 + width - 1
                val y1 = y0 + height - 1
                val bmp = item.second.extract().toBMP32IfRequired().premultipliedIfRequired()
                out.draw(bmp, x0, y0)
                for (i in 1..borderSize) {
                    Bitmap32.copyRect(out, x0, y0, out, x0 - i, y0, 1, height)
                    Bitmap32.copyRect(out, x1, y0, out, x1 + i, y0, 1, height)
                }
                for (i in 1..borderSize) {
                    Bitmap32.copyRect(out, x0 - borderSize, y0, out, x0 - borderSize, y0 - i, width + borderSize2, 1)
                    Bitmap32.copyRect(out, x0 - borderSize, y1, out, x0 - borderSize, y1 + i, width + borderSize2, 1)
                }
                packedItems.add(Entry(item.first, item.second, out.slice(rect.toInt()), rectWithBorder, rect))
            }
            val atlasInfo = AtlasInfo(
                frames = packedItems.map {
                    val bmp = it.slice
                    val r = it.rectWithBorder
                    val rect = Rectangle(r.x.toInt() + 2, r.y.toInt() + 2, bmp.width, bmp.height).toInt()
                    val filename = it.originalSlice.name
                    AtlasInfo.Region(
                        name = filename ?: "unknown",
                        frame = rect,
                        rotated = false,
                        sourceSize = rect.size,
                        spriteSourceSize = RectangleInt(0, 0, rect.width, rect.height),
                        trimmed = false
                    )
                },
                meta = AtlasInfo.Meta(
                    app = "korge",
                    format = "RGBA8888",
                    image = fileName,
                    scale = 1.0,
                    size = SizeInt(out.width, out.height),
                    version = AtlasInfo.Meta.VERSION
                )
            )
            AtlasResult(out, Atlas(out.slice(), atlasInfo), packedItems)
        })
    }
}
