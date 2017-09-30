package com.soywiz.korge.view.tiles

import com.soywiz.korge.render.Texture
import com.soywiz.korge.view.Views

class TileSet(val views: Views, val textures: List<Texture?>, val width: Int, val height: Int, val base: Texture.Base = textures.filterNotNull().first().base) {
	init {
		if (textures.any { if (it != null) it.base != base else false }) {
			throw RuntimeException("All tiles in the set must have the same base texture")
		}
	}

	operator fun get(index: Int): Texture? = textures.getOrNull(index)

	companion object {
		operator fun invoke(views: Views, base: Texture, tileWidth: Int, tileHeight: Int, columns: Int = -1, totalTiles: Int = -1): TileSet {
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

			return TileSet(views, out, tileWidth, tileHeight)
		}
	}
}

fun Views.tileSet(textures: List<Texture?>, width: Int, height: Int, base: Texture.Base = textures.filterNotNull().first().base): TileSet {
	return TileSet(this, textures, width, height, base)
}

fun Views.tileSet(textureMap: Map<Int, Texture?>): TileSet {
	val views = this
	val maxKey = textureMap.keys.max() ?: 0
	val textures = (0 .. maxKey).map { textureMap[it] }
	val firstTexture = textures.first() ?: views.transparentTexture
	return TileSet(this, textures, firstTexture.width, firstTexture.height, firstTexture.base)
}
