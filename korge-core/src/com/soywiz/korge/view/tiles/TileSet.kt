package com.soywiz.korge.view.tiles

import com.soywiz.korge.render.Texture

class TileSet(val textures: List<Texture>, val width: Int, val height: Int, val base: Texture.Base = textures.first().base) {
	init {
		if (textures.any { it.base != base }) {
			throw RuntimeException("All tiles in the set must have the same base texture")
		}
	}

	operator fun get(index: Int): Texture = textures[index]

	companion object {
		operator fun invoke(base: Texture, tileWidth: Int, tileHeight: Int, columns: Int = -1, totalTiles: Int = -1): TileSet {
			val out = arrayListOf<Texture>()
			val rows = base.height / tileHeight
			val actualColumns = if (columns < 0) base.width / tileWidth else columns
			val actualTotalTiles = if (totalTiles < 0) rows * actualColumns else totalTiles

			complete@ for (y in 0 until rows) {
				for (x in 0 until actualColumns) {
					out += base.slice(x * tileWidth, y * tileHeight, tileWidth, tileHeight)
					if (out.size >= actualTotalTiles) break@complete
				}
			}

			return TileSet(out, tileWidth, tileHeight)
		}
	}
}