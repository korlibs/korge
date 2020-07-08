package com.soywiz.korge.tiled

import com.soywiz.kds.iterators.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.tiles.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*

inline fun Container.tiledMapView(tiledMap: TiledMap, showShapes: Boolean = true, callback: TiledMapView.() -> Unit = {}) =
	TiledMapView(tiledMap, showShapes).addTo(this, callback)

class TiledMapView(val tiledMap: TiledMap, showShapes: Boolean = true) : Container() {
	init {
		tiledMap.allLayers.fastForEachWithIndex { index, layer ->
            val view: View = when (layer) {
                is TiledMap.Layer.Patterns -> tileMap(layer.map, tiledMap.tileset)
                is TiledMap.Layer.Image -> image(layer.image)
                is TiledMap.Layer.Objects -> {
                    container {
                        for (obj in layer.objects) {
                            val info = obj.info
                            val bounds = info.bounds
                            val gid = obj.info.gid
                            //println("ID:${obj.id} : ${obj::class}")
                            var shouldShow = showShapes
                            val view: View = when (obj) {
                                is TiledMap.Layer.Objects.PPoint -> {
                                    solidRect(1.0, 1.0, Colors.WHITE)
                                }
                                is TiledMap.Layer.Objects.Ellipse -> {
                                    ellipse(bounds.width / 2, bounds.height / 2)
                                    //solidRect(bounds.width, bounds.width, Colors.RED)
                                }
                                is TiledMap.Layer.Objects.Rect -> {
                                    if (gid != null) {
                                        val tileTex = tiledMap.tileset[gid] ?: Bitmaps.transparent
                                        //println("tileTex[gid=$gid]: $tileTex!")
                                        shouldShow = true
                                        image(tileTex)
                                    } else {
                                        //println("tileTex[gid=$gid]!")
                                        solidRect(bounds.width, bounds.height, Colors.WHITE)
                                    }
                                }
                                is TiledMap.Layer.Objects.Poly -> graphics {
                                    fill(Colors.WHITE) {
                                        var first = true
                                        for (point in obj.points) {
                                            if (first) {
                                                first = false
                                                moveTo(point.x, point.y)
                                            } else {
                                                lineTo(point.x, point.y)
                                            }
                                        }
                                        close()
                                    }
                                }
                                else -> {
                                    println("WARNING: Unsupported tiled object $obj")
                                    dummyView()
                                }
                            }
                            view
                                .visible(shouldShow)
                                .name(info.name.takeIf { it.isNotEmpty() })
                                .xy(bounds.x, bounds.y)
                                .rotation(info.rotation.degrees)
                                .also { it.addProp("type", info.type) }
                                .also { it.addProps(info.objprops) }
                        }
                    }
                }
                else -> dummyView()
            }
            view
                .visible(layer.visible)
                .name(layer.name.takeIf { it.isNotEmpty() })
                .xy(layer.offsetx, layer.offsety)
                .alpha(layer.opacity)
                .also { it.addProps(layer.properties) }
		}
	}
}

fun TiledMap.createView() = TiledMapView(this)
