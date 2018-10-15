package com.soywiz.korge.tiled

import com.soywiz.korge.view.*
import com.soywiz.korge.view.tiles.*

inline fun Container.tiledMapView(tiledMap: TiledMap, callback: @ViewsDslMarker TiledMapView.() -> Unit = {}) =
	TiledMapView(tiledMap).addTo(this).apply(callback)

class TiledMapView(val tiledMap: TiledMap) : Container() {
	init {
		for ((index, layer) in tiledMap.allLayers.withIndex()) {
			if (layer is TiledMap.Layer.Patterns) {
				this += TileMap(layer.map, tiledMap.tileset).apply {
					this.name = layer.name?.takeIf { it.isNotEmpty() }
				}
			}
		}
	}
}

fun TiledMap.createView() = TiledMapView(this)
