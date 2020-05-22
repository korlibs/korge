package com.soywiz.korge.view.tiles

import com.soywiz.kds.IntArray2
import com.soywiz.kmem.*
import com.soywiz.korge.render.*
import com.soywiz.korge.util.toIntArray2
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.RgbaArray
import com.soywiz.korma.geom.*

inline fun Container.tileMap(map: IntArray2, tileset: TileSet, repeatX: TileMap.Repeat = TileMap.Repeat.NONE, repeatY: TileMap.Repeat = repeatX, callback: @ViewsDslMarker TileMap.() -> Unit = {}) =
	TileMap(map, tileset).addTo(this).repeat(repeatX, repeatY).apply(callback)

inline fun Container.tileMap(map: Bitmap32, tileset: TileSet, repeatX: TileMap.Repeat = TileMap.Repeat.NONE, repeatY: TileMap.Repeat = repeatX, callback: @ViewsDslMarker TileMap.() -> Unit = {}) =
	TileMap(map.toIntArray2(), tileset).addTo(this).repeat(repeatX, repeatY).apply(callback)

open class TileMap(val intMap: IntArray2, val tileset: TileSet) : View() {
    @PublishedApi
    internal val _map = Bitmap32(intMap.width, intMap.height, RgbaArray(intMap.data))
	@Deprecated("kept for compatiblity")
	val map get() = _map
	constructor(map: Bitmap32, tileset: TileSet) : this(map.toIntArray2(), tileset)

	val tileWidth = tileset.width.toDouble()
	val tileHeight = tileset.height.toDouble()
	var smoothing = true

	enum class Repeat(val get: (v: Int, max: Int) -> Int) {
		NONE({ v, max -> v }),
		REPEAT({ v, max -> v umod max }),
		MIRROR({ v, max ->
			val r = v umod max
			if ((v / max) % 2 == 0) r else max - 1 - r
		})
	}

	var repeatX = Repeat.NONE
	var repeatY = Repeat.NONE

	private val t0 = Point(0, 0)
	private val tt0 = Point(0, 0)
	private val tt1 = Point(0, 0)
	private val tempPointPool = PointArea(16)

    // Analogous to Bitmap32.locking
    fun lock() = _map.lock()
    fun unlock() = _map.unlock()
    inline fun lock(block: () -> Unit) = _map.lock(block = block)

    private var cachedContentVersion = 0
	private fun computeVertexIfRequired(ctx: RenderContext) {
		if (!dirtyVertices && cachedContentVersion == _map.contentVersion) return
        cachedContentVersion = _map.contentVersion
		dirtyVertices = false
		val m = globalMatrix

		val renderTilesCounter = ctx.stats.counter("renderedTiles")

		val pos = m.transform(0.0, 0.0)
		val dU = m.transform(tileWidth, 0.0) - pos
		val dV = m.transform(0.0, tileHeight) - pos

		val colMul = renderColorMul
		val colAdd = renderColorAdd


		// @TODO: Bounds in clipped view
		val pp0 = globalToLocal(t0.setTo(currentVirtualRect.left, currentVirtualRect.top), tt0)
		val pp1 = globalToLocal(t0.setTo(currentVirtualRect.right, currentVirtualRect.bottom), tt1)
		val mx0 = ((pp0.x / tileWidth) - 1).toInt()
		val mx1 = ((pp1.x / tileWidth) + 1).toInt()
		val my0 = ((pp0.y / tileHeight) - 1).toInt()
		val my1 = ((pp1.y / tileHeight) + 1).toInt()

		val yheight = my1 - my0
		val xwidth = mx1 - mx0
		val ntiles = xwidth * yheight
		verticesPerTex.clear()

		var count = 0
		for (y in my0 until my1) {
			for (x in mx0 until mx1) {
				val rx = repeatX.get(x, intMap.width)
				val ry = repeatY.get(y, intMap.height)

				if (rx < 0 || rx >= intMap.width) continue
				if (ry < 0 || ry >= intMap.height) continue

				val tex = tileset[intMap[rx, ry]] ?: continue

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

					info.vertices.select(info.vcount++).xy(p0.x, p0.y).uv(tex.tl_x, tex.tl_y).cols(colMul, colAdd)
					info.vertices.select(info.vcount++).xy(p1.x, p1.y).uv(tex.tr_x, tex.tr_y).cols(colMul, colAdd)
					info.vertices.select(info.vcount++).xy(p2.x, p2.y).uv(tex.br_x, tex.br_y).cols(colMul, colAdd)
					info.vertices.select(info.vcount++).xy(p3.x, p3.y).uv(tex.bl_x, tex.bl_y).cols(colMul, colAdd)
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
		out.setTo(0, 0, tileWidth * intMap.width, tileHeight * intMap.height)
	}

	override fun hitTest(x: Double, y: Double): View? {
		return if (checkGlobalBounds(x, y, 0.0, 0.0, tileWidth * intMap.width, tileHeight * intMap.height)) this else null
	}
}

fun <T : TileMap> T.repeat(repeatX: TileMap.Repeat, repeatY: TileMap.Repeat = repeatX): T = this.apply {
	this.repeatX = repeatX
	this.repeatY = repeatY
}

