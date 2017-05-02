package com.soywiz.korge.ext.tiled

import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.tiles.TileMap
import com.soywiz.korma.Matrix2d

class TiledMapView(views: Views, val tiledMap: TiledMap) : Container(views) {
	init {
		for ((index, layer) in tiledMap.allLayers.withIndex()) {
			if (layer is TiledMap.Layer.Patterns) {
				this += TileMap(layer.map, tiledMap.tileset, views)
			}
		}
	}
}

fun TiledMap.createView(views: Views) = TiledMapView(views, this)
fun Views.tiledMap(tiledMap: TiledMap) = TiledMapView(this, tiledMap)
