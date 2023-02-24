package com.soywiz.korim.tiles

import com.soywiz.kds.*
import com.soywiz.kds.iterators.fastForEachWithIndex
import com.soywiz.klock.TimeSpan
import com.soywiz.kmem.nextPowerOfTwo
import com.soywiz.kmem.toIntCeil
import com.soywiz.korim.bitmap.*
import kotlin.math.sqrt

data class TileSetAnimationFrame(
    val tileId: Int,
    val duration: TimeSpan,
)

data class TileSetTileInfo(
    val id: Int,
    val slice: BmpSlice,
    val frames: List<TileSetAnimationFrame> = emptyList(),
    val collision: TileShapeInfo? = null,
) : Extra by Extra.Mixin()

class TileSet(
    val tilesMap: IntMap<TileSetTileInfo>,
	//val textures: List<BmpSlice?>,
    val width: Int = if (tilesMap.size == 0) 0 else tilesMap.firstValue().slice.width,
    val height: Int = if (tilesMap.size == 0) 0 else tilesMap.firstValue().slice.height,
) {
    override fun toString(): String = "TileSet(size=${width}x$height, tiles=${tilesMap.keys.toList()})"

    val base: Bitmap by lazy { if (tilesMap.size == 0) Bitmaps.transparent.bmpBase else tilesMap.firstValue().slice.bmpBase }
    val hasMultipleBaseBitmaps by lazy { tilesMap.values.any { it !== null && it.slice.bmpBase !== base } }
    val infos by lazy { Array<TileSetTileInfo?>(tilesMap.keys.maxOrNull()?.plus(1) ?: 0) { tilesMap[it] } }
    val textures by lazy { Array<BitmapCoords?>(tilesMap.keys.maxOrNull()?.plus(1) ?: 0) { tilesMap[it]?.slice } }
	//init { if (hasMultipleBaseBitmaps) throw RuntimeException("All tiles in the set must have the same base texture") }

    //init {
    //    println("texturesMap: ${texturesMap.toMap()}")
    //    println("textures: ${textures.size}")
    //}

    fun getInfo(index: Int): TileSetTileInfo? = infos.getOrNull(index)
    fun getSlice(index: Int): BmpSlice? = getInfo(index)?.slice
	operator fun get(index: Int): BmpSlice? = getSlice(index)

    fun clone(): TileSet = TileSet(this.tilesMap.clone(), this.width, this.height)

	companion object {
        val EMPTY = TileSet(IntMap())

        operator fun invoke(
            tiles: List<TileSetTileInfo>,
            width: Int = tiles.first().slice.width,
            height: Int = tiles.first().slice.height,
        ): TileSet {
            val map = IntMap<TileSetTileInfo>()
            tiles.fastForEachWithIndex { index, value -> map[index] = value }
            return TileSet(map, width, height)
        }

        operator fun invoke(
            vararg tiles: TileSetTileInfo,
            width: Int = tiles.first().slice.width,
            height: Int = tiles.first().slice.height,
        ): TileSet = invoke(tiles.toList(), width, height)

		operator fun invoke(
            base: BmpSlice,
            tileWidth: Int = base.width,
            tileHeight: Int = base.height,
            columns: Int = -1,
            totalTiles: Int = -1,
		): TileSet {
			val out = IntMap<TileSetTileInfo>()
			val rows = base.height / tileHeight
			val actualColumns = if (columns < 0) base.width / tileWidth else columns
			val actualTotalTiles = if (totalTiles < 0) rows * actualColumns else totalTiles
            var n = 0

			complete@ for (y in 0 until rows) {
				for (x in 0 until actualColumns) {
					out[n] = TileSetTileInfo(n + 1, base.sliceWithSize(x * tileWidth, y * tileHeight, tileWidth, tileHeight))
                    n++
					if (out.size >= actualTotalTiles) break@complete
				}
			}

			return TileSet(out, tileWidth, tileHeight)
		}

        fun extractBmpSlices(
            bmp: Bitmap32,
            tilewidth: Int,
            tileheight: Int,
            columns: Int,
            tilecount: Int,
            spacing: Int,
            margin :Int
        ): List<BmpSlice32> {
            return ArrayList<BmpSlice32>().apply {
                loop@ for (y in 0 until bmp.height / tileheight) {
                    for (x in 0 until columns) {
                        add(bmp.sliceWithSize(
                            margin + x * (tilewidth + spacing),
                            margin + y * (tileheight + spacing),
                            tilewidth, tileheight
                        ))
                        if (this.size >= tilecount) break@loop
                    }
                }
            }
        }

		fun extractBitmaps(
			bmp: Bitmap32,
			tilewidth: Int,
			tileheight: Int,
			columns: Int,
			tilecount: Int,
            spacing: Int,
            margin :Int
		): List<Bitmap32> = extractBmpSlices(bmp, tilewidth, tileheight, columns, tilecount, spacing, margin).map { it.extract() }

        fun fromBitmaps(
            tilewidth: Int,
            tileheight: Int,
            bitmaps: List<Bitmap32>,
            border: Int = 1,
            mipmaps: Boolean = false,
        ): TileSet {
            return fromBitmapSlices(tilewidth, tileheight, bitmaps.map { it.slice() }, border, mipmaps)
        }

		fun fromBitmapSlices(
            tilewidth: Int,
            tileheight: Int,
            bmpSlices: List<BmpSlice32>,
            border: Int = 1,
            mipmaps: Boolean = false,
        ): TileSet {
			check(bmpSlices.all { it.width == tilewidth && it.height == tileheight })
			if (bmpSlices.isEmpty()) return TileSet(IntMap(), tilewidth, tileheight)

			//sqrt(bitmaps.size.toDouble()).toIntCeil() * tilewidth

			val border2 = border * 2
			val btilewidth = tilewidth + border2
			val btileheight = tileheight + border2
			val barea = btilewidth * btileheight
			val fullArea = bmpSlices.size.nextPowerOfTwo * barea
			val expectedSide = sqrt(fullArea.toDouble()).toIntCeil().nextPowerOfTwo

            val premultiplied = bmpSlices.any { it.base.premultiplied }

			val out = Bitmap32(expectedSide, expectedSide, premultiplied = premultiplied).mipmaps(mipmaps)
			val texs = IntMap<TileSetTileInfo>()

			val columns = (out.width / btilewidth)

			lateinit var tex: Bitmap
			//val tex = views.texture(out, mipmaps = mipmaps)
            var nn = 0
			for (m in 0 until 2) {
				for (n in 0 until bmpSlices.size) {
					val y = n / columns
					val x = n % columns
					val px = x * btilewidth + border
					val py = y * btileheight + border
					if (m == 0) {
						out.putSliceWithBorder(px, py, bmpSlices[n], border)
					} else {
						texs[nn] = TileSetTileInfo(nn, tex.sliceWithSize(px, py, tilewidth, tileheight, name = bmpSlices[n].name))
                        nn++
					}
				}
				if (m == 0) {
					tex = out
				}
			}

			return TileSet(texs, tilewidth, tileheight)
		}
	}
}
