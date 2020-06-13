package com.soywiz.korge.view.tiles

import com.soywiz.kds.*
import com.soywiz.kds.IntArray2
import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.render.*
import com.soywiz.korge.util.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.paint.*
import com.soywiz.korma.geom.*
import kotlin.math.*

inline fun Container.tileMap(map: IntArray2, tileset: TileSet, repeatX: TileMap.Repeat = TileMap.Repeat.NONE, repeatY: TileMap.Repeat = repeatX, callback: @ViewsDslMarker TileMap.() -> Unit = {}) =
	TileMap(map, tileset).addTo(this).repeat(repeatX, repeatY).apply(callback)

inline fun Container.tileMap(map: Bitmap32, tileset: TileSet, repeatX: TileMap.Repeat = TileMap.Repeat.NONE, repeatY: TileMap.Repeat = repeatX, callback: @ViewsDslMarker TileMap.() -> Unit = {}) =
	TileMap(map.toIntArray2(), tileset).addTo(this).repeat(repeatX, repeatY).apply(callback)

@OptIn(KorgeInternal::class)
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
    private val tt2 = Point(0, 0)
    private val tt3 = Point(0, 0)

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

        val posX = m.fastTransformX(0.0, 0.0)
        val posY = m.fastTransformY(0.0, 0.0)
        val dUX = m.fastTransformX(tileWidth, 0.0) - posX
        val dUY = m.fastTransformY(tileWidth, 0.0) - posY
        val dVX = m.fastTransformX(0.0, tileHeight) - posX
        val dVY = m.fastTransformY(0.0, tileHeight) - posY

        val colMul = renderColorMul
        val colAdd = renderColorAdd

        // @TODO: Bounds in clipped view
        val pp0 = globalToLocal(t0.setTo(currentVirtualRect.left, currentVirtualRect.top), tt0)
        val pp1 = globalToLocal(t0.setTo(currentVirtualRect.right, currentVirtualRect.bottom), tt1)
        val pp2 = globalToLocal(t0.setTo(currentVirtualRect.right, currentVirtualRect.top), tt2)
        val pp3 = globalToLocal(t0.setTo(currentVirtualRect.left, currentVirtualRect.bottom), tt3)
        val mx0 = ((pp0.x / tileWidth) - 1).toInt()
        val mx1 = ((pp1.x / tileWidth) + 1).toInt()
        val mx2 = ((pp2.x / tileWidth) + 1).toInt()
        val mx3 = ((pp3.x / tileWidth) + 1).toInt()
        val my0 = ((pp0.y / tileHeight) - 1).toInt()
        val my1 = ((pp1.y / tileHeight) + 1).toInt()
        val my2 = ((pp2.y / tileHeight) + 1).toInt()
        val my3 = ((pp3.y / tileHeight) + 1).toInt()

        val ymin = min(min(min(my0, my1), my2), my3)
        val ymax = max(max(max(my0, my1), my2), my3)
        val xmin = min(min(min(mx0, mx1), mx2), mx3)
        val xmax = max(max(max(mx0, mx1), mx2), mx3)

        val yheight = ymax - ymin
        val xwidth = xmax - xmin
        val ntiles = xwidth * yheight
        val allocTiles = ntiles.nextPowerOfTwo
        //println("(mx0=$mx0, my0=$my0)-(mx1=$mx1, my1=$my1)-(mx2=$mx2, my2=$my2)-(mx3=$mx3, my3=$my3) ($xwidth, $yheight)")
        infos.fastForEach { infosPool.free(it) }
        verticesPerTex.clear()
        infos.clear()

        var count = 0
        for (y in ymin until ymax) {
            for (x in xmin until xmax) {
                val rx = repeatX.get(x, intMap.width)
                val ry = repeatY.get(y, intMap.height)

                if (rx < 0 || rx >= intMap.width) continue
                if (ry < 0 || ry >= intMap.height) continue
                val tex = tileset[intMap[rx, ry]] ?: continue

                val info = verticesPerTex.getOrPut(tex.bmp) {
                    infosPool.alloc().also { info ->
                        info.tex = tex.bmp
                        if (info.vertices.initialVcount < allocTiles * 4) {
                            info.vertices = TexturedVertexArray(allocTiles * 4, TexturedVertexArray.quadIndices(allocTiles))
                            //println("ALLOC TexturedVertexArray")
                        }
                        info.vcount = 0
                        info.icount = 0
                        infos += info
                    }
                }

                run {
                    val p0X = posX + (dUX * x) + (dVX * y)
                    val p0Y = posY + (dUY * x) + (dVY * y)

                    val p1X = p0X + dUX
                    val p1Y = p0Y + dUY

                    val p2X = p0X + dUX + dVX
                    val p2Y = p0Y + dUY + dVY

                    val p3X = p0X + dVX
                    val p3Y = p0Y + dVY

                    info.vertices.quadV(info.vcount++, p0X, p0Y, tex.tl_x, tex.tl_y, colMul, colAdd)
                    info.vertices.quadV(info.vcount++, p1X, p1Y, tex.tr_x, tex.tr_y, colMul, colAdd)
                    info.vertices.quadV(info.vcount++, p2X, p2Y, tex.br_x, tex.br_y, colMul, colAdd)
                    info.vertices.quadV(info.vcount++, p3X, p3Y, tex.bl_x, tex.bl_y, colMul, colAdd)
                }

                info.icount += 6
                count++
            }
        }
        renderTilesCounter?.increment(count)
	}

	// @TOOD: Use a TextureVertexBuffer or something
    @KorgeInternal
	private class Info(var tex: Bitmap, var vertices: TexturedVertexArray) {
		var vcount = 0
		var icount = 0
	}

	private val verticesPerTex = FastIdentityMap<Bitmap, Info>()
    private val infos = arrayListOf<Info>()
    companion object {
        private val dummyTexturedVertexArray = TexturedVertexArray(0, IntArray(0))
    }
    private val infosPool = Pool { Info(Bitmaps.transparent.bmp, dummyTexturedVertexArray) }

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

        infos.fastForEach { buffer ->
            ctx.batch.drawVertices(
                buffer.vertices, ctx.getTex(buffer.tex), smoothing, renderBlendMode.factors, buffer.vcount, buffer.icount
            )
        }
		ctx.flush()
	}

	override fun getLocalBoundsInternal(out: Rectangle) {
		out.setTo(0, 0, tileWidth * intMap.width, tileHeight * intMap.height)
	}

	//override fun hitTest(x: Double, y: Double): View? {
	//	return if (checkGlobalBounds(x, y, 0.0, 0.0, tileWidth * intMap.width, tileHeight * intMap.height)) this else null
	//}
}

fun <T : TileMap> T.repeat(repeatX: TileMap.Repeat, repeatY: TileMap.Repeat = repeatX): T {
	this.repeatX = repeatX
	this.repeatY = repeatY
    return this
}

