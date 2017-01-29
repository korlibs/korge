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
		operator fun invoke(base: Texture, tileWidth: Int, tileHeight: Int): TileSet {
			val out = arrayListOf<Texture>()
			for (y in 0 until base.height / tileHeight) {
				for (x in 0 until base.width / tileWidth) {
					out += base.slice(x * tileWidth, y * tileHeight, tileWidth, tileHeight)
				}
			}
			return TileSet(out, tileWidth, tileHeight)
		}
	}
}