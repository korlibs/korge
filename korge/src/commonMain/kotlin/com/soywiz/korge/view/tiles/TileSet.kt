package com.soywiz.korge.view.tiles

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.*
import kotlin.math.*

class TileSet(
    val texturesMap: IntMap<BmpSlice>,
	//val textures: List<BmpSlice?>,
	val width: Int = texturesMap.firstValue().width,
	val height: Int = texturesMap.firstValue().height
) {
    val base: Bitmap by lazy { if (texturesMap.size == 0) Bitmaps.transparent.bmpBase else texturesMap.firstValue().bmpBase }
    val hasMultipleBaseBitmaps by lazy { texturesMap.values.any { it !== null && it.bmpBase !== base } }
    val textures by lazy { Array<BmpSlice?>(texturesMap.keys.maxOrNull()?.plus(1) ?: 0) { texturesMap[it] } }
	//init { if (hasMultipleBaseBitmaps) throw RuntimeException("All tiles in the set must have the same base texture") }

    //init {
    //    println("texturesMap: ${texturesMap.toMap()}")
    //    println("textures: ${textures.size}")
    //}

	operator fun get(index: Int): BmpSlice? = textures.getOrNull(index)

    fun clone(): TileSet = TileSet(this.texturesMap.clone(), this.width, this.height)

	companion object {
		operator fun invoke(textureMap: Map<Int, BmpSlice>): TileSet = TileSet(textureMap.toIntMap())

        operator fun invoke(tileSets: List<TileSet>): TileSet {
            val map = IntMap<BmpSlice>()
            tileSets.fastForEach { tileSet ->
                map.putAll(tileSet.texturesMap)
            }
            return TileSet(map)
        }

        operator fun invoke(tiles: List<BmpSlice>, width: Int, height: Int): TileSet {
            val map = IntMap<BmpSlice>()
            tiles.fastForEachWithIndex { index, value -> map[index] = value }
            return TileSet(map, width, height)
        }

		operator fun invoke(
			base: BitmapSlice<Bitmap>,
			tileWidth: Int,
			tileHeight: Int,
			columns: Int = -1,
			totalTiles: Int = -1
		): TileSet {
			val out = IntMap<BmpSlice>()
			val rows = base.height / tileHeight
			val actualColumns = if (columns < 0) base.width / tileWidth else columns
			val actualTotalTiles = if (totalTiles < 0) rows * actualColumns else totalTiles
            var n = 0

			complete@ for (y in 0 until rows) {
				for (x in 0 until actualColumns) {
					out[n++] = base.sliceWithSize(x * tileWidth, y * tileHeight, tileWidth, tileHeight)
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
            mipmaps: Boolean = false
        ): TileSet {
            return fromBitmapSlices(tilewidth, tileheight, bitmaps.map { it.slice() }, border, mipmaps)
        }

		fun fromBitmapSlices(
            tilewidth: Int,
            tileheight: Int,
            bmpSlices: List<BitmapSlice<Bitmap32>>,
            border: Int = 1,
            mipmaps: Boolean = false
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

			val out = Bitmap32(expectedSide, expectedSide).mipmaps(mipmaps)
			val texs = IntMap<BmpSlice>()

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
						texs[nn++] = tex.sliceWithSize(px, py, tilewidth, tileheight, name = bmpSlices[n].name)
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
