package com.soywiz.korge.tiled

import com.soywiz.kds.iterators.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.tiles.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlin.math.*

inline fun Container.tiledMapView(tiledMap: TiledMap, showShapes: Boolean = true, smoothing: Boolean = true, callback: TiledMapView.() -> Unit = {}) =
	TiledMapView(tiledMap, showShapes, smoothing).addTo(this, callback)

class TiledMapView(val tiledMap: TiledMap, showShapes: Boolean = true, smoothing: Boolean = true) : Container() {
    val tileset = tiledMap.tilesets.toTileSet()

    override fun hitTest(x: Double, y: Double, direction: HitTestDirection): View? {
        //return super.hitTest(x, y, direction)
        return globalPixelHitTest(x, y, direction)
    }

    //protected override fun hitTestInternal(x: Double, y: Double, direction: HitTestDirection): View? = globalPixelHitTest(x, y, direction)

    //fun globalPixelHitTest(globalXY: IPoint, direction: HitTestDirection = HitTestDirection.ANY): View? = globalPixelHitTest(globalXY.x, globalXY.y, direction)

    fun globalPixelHitTest(globalX: Double, globalY: Double, direction: HitTestDirection = HitTestDirection.ANY): View? {
        return pixelHitTest(
            round(globalToLocalX(globalX, globalY) / scaleX).toInt(),
            round(globalToLocalY(globalX, globalY) / scaleY).toInt(),
            direction
        )
    }

    fun pixelHitTest(x: Int, y: Int, direction: HitTestDirection = HitTestDirection.ANY): View? {
        fastForEachChild { child ->
            when (child) {
                is TileMap -> {
                    if (child.pixelHitTest(x, y, direction)) return child
                }
            }
        }
        return null
    }

    init {
		tiledMap.allLayers.fastForEachWithIndex { index, layer ->
            val view: View = when (layer) {
                is TiledMap.Layer.Tiles -> tileMap(
                    map = layer.map,
                    tileset = tileset,
                    smoothing = smoothing,
                    orientation = tiledMap.data.orientation,
                    staggerAxis = tiledMap.data.staggerAxis,
                    staggerIndex = tiledMap.data.staggerIndex,
                    tileSize = Size(tiledMap.tilewidth.toDouble(), tiledMap.tileheight.toDouble()),
                )
                //is TiledMap.Layer.Image -> image(layer.image)
                is TiledMap.Layer.Objects -> {
                    container {
                        for (obj in layer.objects) {
                            val bounds = obj.bounds
                            val gid = obj.gid
                            //println("ID:${obj.id} : ${obj::class}")
                            var shouldShow = showShapes
                            val view: View = when (val type = obj.objectShape) {
                                is TiledMap.Object.Shape.PPoint -> {
                                    solidRect(1.0, 1.0, Colors.WHITE)
                                }
                                is TiledMap.Object.Shape.Ellipse -> {
                                    ellipse(bounds.width / 2, bounds.height / 2)
                                    //solidRect(bounds.width, bounds.width, Colors.RED)
                                }
                                is TiledMap.Object.Shape.Rectangle -> {
                                    if (gid != null) {
                                        val tileTex = tileset[gid] ?: Bitmaps.transparent
                                        //println("tileTex[gid=$gid]: $tileTex!")
                                        shouldShow = true
                                        image(tileTex)
                                    } else {
                                        //println("tileTex[gid=$gid]!")
                                        solidRect(bounds.width, bounds.height, Colors.WHITE)
                                    }
                                }
                                is TiledMap.Object.Shape.Polygon -> graphics {
                                    fill(Colors.WHITE) {
                                        var first = true
                                        var firstPoint: Point? = null
                                        for (point in type.points) {
                                            if (first) {
                                                first = false
                                                firstPoint = point
                                                moveTo(point.x, point.y)
                                            } else {
                                                lineTo(point.x, point.y)
                                            }
                                        }
                                        firstPoint?.let { lineTo(it.x, it.y) }
                                        close()
                                    }
                                }
                                is TiledMap.Object.Shape.Polyline -> graphics {
                                    fill(Colors.WHITE) {
                                        var first = true
                                        for (point in type.points) {
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
                                is TiledMap.Object.Shape.Text -> {
                                    TODO("Unsupported tiled object $obj")
                                }
                            }
                            view
                                .visible(shouldShow)
                                .name(obj.name.takeIf { it.isNotEmpty() })
                                .xy(bounds.x, bounds.y)
                                .rotation(obj.rotation.degrees)
                                .also { it.addProp("type", obj.type) }
                                .also { it.addProps(obj.properties) }
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
