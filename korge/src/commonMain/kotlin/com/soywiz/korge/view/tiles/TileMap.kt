package com.soywiz.korge.view.tiles

import com.soywiz.kmem.*
import com.soywiz.korge.render.*
import com.soywiz.korge.util.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.*

inline fun Container.tileMap(map: IntArray2, tileset: TileSet, callback: @ViewsDslMarker TileMap.() -> Unit = {}) =
	TileMap(map, tileset).addTo(this).apply(callback)

open class TileMap(val map: IntArray2, val tileset: TileSet) : View() {
	val tileWidth = tileset.width.toDouble()
	val tileHeight = tileset.height.toDouble()
	var smoothing = true

	private val t0 = Point(0, 0)
	private val tt0 = Point(0, 0)
	private val tt1 = Point(0, 0)
	private val tempPointPool = PointArea(16)

	private fun computeVertexIfRequired(ctx: RenderContext) {
		if (!dirtyVertices) return
		dirtyVertices = false
		val m = globalMatrix

		val renderTilesCounter = ctx.stats.counter("renderedTiles")

		val pos = m.transform(0.0, 0.0)
		val dU = m.transform(tileWidth, 0.0) - pos
		val dV = m.transform(0.0, tileHeight) - pos

		val colMulInt = renderColorMulInt
		val colAdd = renderColorAdd


		// @TODO: Bounds in clipped view
		val pp0 = globalToLocal(t0.setTo(currentVirtualRect.left, currentVirtualRect.top), tt0)
		val pp1 = globalToLocal(t0.setTo(currentVirtualRect.right, currentVirtualRect.bottom), tt1)
		val mx0 = ((pp0.x / tileWidth) - 1).toInt().clamp(0, map.width)
		val mx1 = ((pp1.x / tileWidth) + 1).toInt().clamp(0, map.width)
		val my0 = ((pp0.y / tileHeight) - 1).toInt().clamp(0, map.height)
		val my1 = ((pp1.y / tileHeight) + 1).toInt().clamp(0, map.height)

		//views.stats.value("tiledmap.$name.bounds").set("${views.virtualLeft},${views.virtualTop},${views.virtualRight},${views.virtualBottom}")
		//views.stats.value("tiledmap.$name.pp0,pp1").set("$pp0,$pp1")
		//views.stats.value("tiledmap.$name.tileWidth,tileHeight").set("$tileWidth,$tileHeight")
		//views.stats.value("tiledmap.$name.mx0,my0").set("$mx0,$my0")
		//views.stats.value("tiledmap.$name.mx1,my1").set("$mx1,$my1")

		val yheight = my1 - my0
		val xwidth = mx1 - mx0
		val ntiles = xwidth * yheight
		verticesPerTex.clear()

		var count = 0
		for (y in my0 until my1) {
			for (x in mx0 until mx1) {
				val tex = tileset[map.getInt(x, y)] ?: continue

				val info = verticesPerTex.getOrPut(tex.bmp) {
					val indices = TexturedVertexArray.quadIndices(ntiles)
					//println(indices.toList())
					Info(TexturedVertexArray(ntiles * 4, indices))
				}

				tempPointPool {
					val p0 = pos + (dU * x) + (dV * y)
					val p1 = p0 + dU
					val p2 = p0 + dU + dV
					val p3 = p0 + dV

					info.vertices.select(info.vcount++).xy(p0.x, p0.y).uv(tex.tl_x, tex.tl_y).cols(colMulInt, colAdd)
					info.vertices.select(info.vcount++).xy(p1.x, p1.y).uv(tex.tr_x, tex.tr_y).cols(colMulInt, colAdd)
					info.vertices.select(info.vcount++).xy(p2.x, p2.y).uv(tex.br_x, tex.br_y).cols(colMulInt, colAdd)
					info.vertices.select(info.vcount++).xy(p3.x, p3.y).uv(tex.bl_x, tex.bl_y).cols(colMulInt, colAdd)
				}

				info.icount += 6
				count++
			}
		}
		renderTilesCounter?.increment(count)
	}

	// @TOOD: Use a TextureVertexBuffer or something
	class Info(val vertices: TexturedVertexArray) {
		var vcount = 0
		var icount = 0
	}

	private val verticesPerTex = LinkedHashMap<Bitmap, Info>()

	private var lastVirtualRect = Rectangle(-1, -1, -1, -1)
	private var currentVirtualRect = Rectangle(-1, -1, -1, -1)

	override fun renderInternal(ctx: RenderContext) {
		if (!visible) return
		currentVirtualRect.setBounds(ctx.virtualLeft, ctx.virtualTop, ctx.virtualRight, ctx.virtualBottom)
		if (currentVirtualRect != lastVirtualRect) {
			dirtyVertices = true
			lastVirtualRect.copyFrom(currentVirtualRect)
		}
		computeVertexIfRequired(ctx)

		for ((tex, buffer) in verticesPerTex) {
			ctx.batch.drawVertices(
				buffer.vertices, ctx.getTex(tex), smoothing, renderBlendMode.factors, buffer.vcount, buffer.icount
			)
		}
		ctx.flush()
	}

	override fun getLocalBoundsInternal(out: Rectangle) {
		out.setTo(0, 0, tileWidth * map.width, tileHeight * map.height)
	}

	override fun hitTest(x: Double, y: Double): View? {
		return if (checkGlobalBounds(x, y, 0.0, 0.0, tileWidth * map.width, tileHeight * map.height)) this else null
	}
}
