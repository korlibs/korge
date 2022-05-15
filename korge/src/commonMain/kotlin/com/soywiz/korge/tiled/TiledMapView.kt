package com.soywiz.korge.tiled

import com.soywiz.kds.iterators.fastForEachWithIndex
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.HitTestDirection
import com.soywiz.korge.view.View
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.alpha
import com.soywiz.korge.view.container
import com.soywiz.korge.view.dummyView
import com.soywiz.korge.view.ellipse
import com.soywiz.korge.view.graphics
import com.soywiz.korge.view.image
import com.soywiz.korge.view.name
import com.soywiz.korge.view.rotation
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.tiles.TileMap
import com.soywiz.korge.view.tiles.tileMap
import com.soywiz.korge.view.visible
import com.soywiz.korge.view.xy
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.Size
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.shape.Shape2d
import kotlin.math.round

inline fun Container.tiledMapView(tiledMap: TiledMap, showShapes: Boolean = true, smoothing: Boolean = true, callback: TiledMapView.() -> Unit = {}) =
	TiledMapView(tiledMap, showShapes, smoothing).addTo(this, callback)

class TiledMapView(val tiledMap: TiledMap, showShapes: Boolean = true, smoothing: Boolean = true) : Container() {
    val tileset = tiledMap.tilesets.toTileSet()

    override fun hitTest(x: Double, y: Double, direction: HitTestDirection): View? {
        //return super.hitTest(x, y, direction)
        return globalPixelHitTest(x, y, direction)
    }

    override val customHitShape get() = true
    override protected fun hitTestShapeInternal(shape: Shape2d, matrix: Matrix, direction: HitTestDirection): View? {
        // @TODO: Use shape
        val p = matrix.transform(shape.getCenter())
        return globalPixelHitTest(p.x, p.y, direction)
        //println("TiledMapView.hitTestShapeInternal: $shape, $matrix")
        //return super.hitTestShapeInternal(shape, matrix, direction)
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
