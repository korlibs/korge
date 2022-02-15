package com.soywiz.korge.view.tiles

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import kotlin.math.*

data class TileSetAnimationFrame(
    val tileId: Int,
    val duration: TimeSpan,
)

data class TileSetTileInfo(
    val id: Int,
    val slice: BmpSlice,
    val frames: List<TileSetAnimationFrame> = emptyList()
)

class TileSet(
    val texturesMap: IntMap<TileSetTileInfo>,
	//val textures: List<BmpSlice?>,
    val width: Int = texturesMap.firstValue().slice.width,
    val height: Int = texturesMap.firstValue().slice.height,
    val collisionsMap: IntMap<TileShapeInfo> = IntMap(),
) {
    val base: Bitmap by lazy { if (texturesMap.size == 0) Bitmaps.transparent.bmpBase else texturesMap.firstValue().slice.bmpBase }
    val hasMultipleBaseBitmaps by lazy { texturesMap.values.any { it !== null && it.slice.bmpBase !== base } }
    val infos by lazy { Array<TileSetTileInfo?>(texturesMap.keys.maxOrNull()?.plus(1) ?: 0) { texturesMap[it] } }
    val textures by lazy { Array<BitmapCoords?>(texturesMap.keys.maxOrNull()?.plus(1) ?: 0) { texturesMap[it]?.slice } }
    val collisions by lazy { Array<TileShapeInfo?>(texturesMap.keys.maxOrNull()?.plus(1) ?: 0) { collisionsMap[it] } }
	//init { if (hasMultipleBaseBitmaps) throw RuntimeException("All tiles in the set must have the same base texture") }

    //init {
    //    println("texturesMap: ${texturesMap.toMap()}")
    //    println("textures: ${textures.size}")
    //}

    fun getInfo(index: Int): TileSetTileInfo? = infos.getOrNull(index)
    fun getSlice(index: Int): BmpSlice? = getInfo(index)?.slice
	operator fun get(index: Int): BmpSlice? = getSlice(index)

    fun clone(): TileSet = TileSet(this.texturesMap.clone(), this.width, this.height)

	companion object {
        @Deprecated("")
        operator fun invoke(
            texturesMap: Map<Int, BmpSlice>,
            width: Int = texturesMap.values.first().width,
            height: Int = texturesMap.values.first().height,
            collisionsMap: IntMap<TileShapeInfo> = IntMap(),
        ) = TileSet(texturesMap.map { (key, value) -> key to TileSetTileInfo(key, value) }.toMap().toIntMap(), width, height, collisionsMap)

		operator fun invoke(
            textureMap: Map<Int, TileSetTileInfo>,
            collisionsMap: IntMap<TileShapeInfo> = IntMap(),
        ): TileSet = TileSet(textureMap.toIntMap(), collisionsMap = collisionsMap)

        operator fun invoke(
            tileSets: List<TileSet>,
            collisionsMap: IntMap<TileShapeInfo> = IntMap(),
        ): TileSet {
            val map = IntMap<TileSetTileInfo>()
            tileSets.fastForEach { tileSet ->
                map.putAll(tileSet.texturesMap)
            }
            return TileSet(map, collisionsMap = collisionsMap)
        }

        operator fun invoke(
            tiles: List<TileSetTileInfo>,
            width: Int, height: Int,
            collisionsMap: IntMap<TileShapeInfo> = IntMap(),
        ): TileSet {
            val map = IntMap<TileSetTileInfo>()
            tiles.fastForEachWithIndex { index, value -> map[index] = value }
            return TileSet(map, width, height, collisionsMap = collisionsMap)
        }

		operator fun invoke(
            base: BitmapSlice<Bitmap>,
            tileWidth: Int = base.width,
            tileHeight: Int = base.height,
            columns: Int = -1,
            totalTiles: Int = -1,
            collisionsMap: IntMap<TileShapeInfo> = IntMap(),
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

			return TileSet(out, tileWidth, tileHeight, collisionsMap = collisionsMap)
		}

        fun extractBmpSlices(
            bmp: Bitmap32,
            tilewidth: Int,
            tileheight: Int,
            columns: Int,
            tilecount: Int,
            spacing: Int,
            margin :Int
        ): List<BitmapSlice<Bitmap32>> {
            return ArrayList<BitmapSlice<Bitmap32>>().apply {
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
            collisionsMap: IntMap<TileShapeInfo> = IntMap(),
        ): TileSet {
            return fromBitmapSlices(tilewidth, tileheight, bitmaps.map { it.slice() }, border, mipmaps, collisionsMap = collisionsMap)
        }

		fun fromBitmapSlices(
            tilewidth: Int,
            tileheight: Int,
            bmpSlices: List<BitmapSlice<Bitmap32>>,
            border: Int = 1,
            mipmaps: Boolean = false,
            collisionsMap: IntMap<TileShapeInfo> = IntMap(),
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

            val premultiplied = bmpSlices.any { it.premultiplied }

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

			return TileSet(texs, tilewidth, tileheight, collisionsMap = collisionsMap)
		}
	}
}
