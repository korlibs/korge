package com.soywiz.korge.view.tiles

import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture
import com.soywiz.korge.util.IntArray2
import com.soywiz.korge.view.*
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.Rectangle

class TileMap(val map: IntArray2, val tileset: TileSet, views: Views) : View(views) {
    val tileWidth = tileset.width.toDouble()
    val tileHeight = tileset.height.toDouble()
    var smoothing = true

    override fun render(ctx: RenderContext, m: Matrix2d) {
        val batch = ctx.batch

        val pos = m.transform(0.0, 0.0)
        val dU = m.transform(tileWidth, 0.0) - pos
        val dV = m.transform(0.0, tileHeight) - pos

        val colorMul = globalColorMul
		val colorAdd = globalColorAdd

        batch.setStateFast(tileset.base, blendFactors = blendMode.factors, smoothing = smoothing)

        map.forEach { v, x, y ->
            val tex = tileset[0]
            val p0 = pos + (dU * x.toDouble()) + (dV * y.toDouble())
            val p1 = p0 + dU
            val p2 = p0 + dU + dV
            val p3 = p0 + dV
            batch.addQuadFast(
                    p0.x.toFloat(), p0.y.toFloat(),
                    p1.x.toFloat(), p1.y.toFloat(),
                    p2.x.toFloat(), p2.y.toFloat(),
                    p3.x.toFloat(), p3.y.toFloat(),
                    tex, colorMul, colorAdd
            )
        }

        ctx.flush()
    }

	override fun getLocalBounds(out: Rectangle) {
		out.setTo(0, 0, tileWidth * map.width, tileHeight * map.height)
	}

	override fun hitTestInternal(x: Double, y: Double): View? {
        return if (checkGlobalBounds(x, y, 0.0, 0.0, tileWidth * map.width, tileHeight * map.height)) this else null
    }
}

fun Views.tileMap(map: IntArray2, tileset: TileSet) = TileMap(map, tileset, this)

fun Container.tileMap(map: IntArray2, tileset: TileSet): TileMap = tileMap(map, tileset) { }

inline fun Container.tileMap(map: IntArray2, tileset: TileSet, callback: TileMap.() -> Unit): TileMap {
    val child = views.tileMap(map, tileset)
    this += child
    callback(child)
    return child
}
