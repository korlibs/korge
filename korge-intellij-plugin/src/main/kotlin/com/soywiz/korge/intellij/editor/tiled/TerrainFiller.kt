package com.soywiz.korge.intellij.editor.tiled

import com.soywiz.korge.tiled.*

object TerrainFiller {
	fun updateTile(layer: TiledMap.Layer.Tiles, x: Int, y: Int, tileset: TiledMap.TiledTileset) {
		val topLeft = layer[x - 1, y - 1]
		val top = layer[x, y - 1]
		val topRight = layer[x + 1, y - 1]
		val left = layer[x - 1, 0]
		val right = layer[x + 1, 0]
		val bottomLeft = layer[x - 1, y + 1]
		val bottom = layer[x, y + 1]
		val bottomRight = layer[x + 1, y + 1]
		tileset.data.tiles
	}
}
