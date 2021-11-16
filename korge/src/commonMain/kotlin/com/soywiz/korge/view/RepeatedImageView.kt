package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kmem.*
import com.soywiz.korge.internal.KorgeInternal
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.TexturedVertexArray
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.Rectangle
import kotlin.math.max
import kotlin.math.min

inline fun Container.repeatedImageView(
    bitmap: BmpSlice,
    repeatX: Boolean = false,
    repeatY: Boolean = false,
    smoothing: Boolean = true,
    block: @ViewDslMarker RepeatedImageView.() -> Unit = {}
) = RepeatedImageView(bitmap, smoothing, repeatX, repeatY).addTo(this, block)

open class RepeatedImageView(
    bitmap: BmpSlice,
    var repeatX: Boolean = false,
    var repeatY: Boolean = false,
    override var smoothing: Boolean = true
) : View(), SmoothedBmpSlice {

    override var width = 0.0
    override var height = 0.0

    override var bitmap: BmpSlice = bitmap
        set(value) {
            if (field !== value) {
                field = value
                this.width = value.width.toDouble()
                this.height = value.height.toDouble()
            }
        }

    private val t0 = Point(0, 0)
    private val tt0 = Point(0, 0)
    private val tt1 = Point(0, 0)
    private val tt2 = Point(0, 0)
    private val tt3 = Point(0, 0)

    private fun computeVertexIfRequired(ctx: RenderContext) {
        if (!dirtyVertices) return
        dirtyVertices = false
        val m = globalMatrix

        val renderTilesCounter = ctx.stats.counter("renderedTiles")

        val posX = m.transformX(0.0, 0.0)
        val posY = m.transformY(0.0, 0.0)
        val dUX = m.transformX(width, 0.0) - posX
        val dUY = m.transformY(width, 0.0) - posY
        val dVX = m.transformX(0.0, height) - posX
        val dVY = m.transformY(0.0, height) - posY
        val initY = 0.0
        val nextTextureX = width.let { width ->
            min(m.transformX(width, 0.0) - posX, m.transformY(0.0, width) - posY)
        }
        val nextTextureY = height.let { height ->
            min(m.transformX(height, 0.0) - posX, m.transformY(0.0, height) - posY)
        }

        val colMul = renderColorMul
        val colAdd = renderColorAdd

        // @TODO: Bounds in clipped view
        val pp0 = globalToLocal(t0.setTo(currentVirtualRect.left, currentVirtualRect.top), tt0)
        val pp1 = globalToLocal(t0.setTo(currentVirtualRect.right, currentVirtualRect.bottom), tt1)
        val pp2 = globalToLocal(t0.setTo(currentVirtualRect.right, currentVirtualRect.top), tt2)
        val pp3 = globalToLocal(t0.setTo(currentVirtualRect.left, currentVirtualRect.bottom), tt3)
        val mx0 = ((pp0.x / width) - 1).toInt()
        val mx1 = ((pp1.x / width) + 1).toInt()
        val mx2 = ((pp2.x / width) + 1).toInt()
        val mx3 = ((pp3.x / width) + 1).toInt()
        val my0 = ((pp0.y / height) - 1).toInt()
        val my1 = ((pp1.y / height) + 1).toInt()
        val my2 = ((pp2.y / height) + 1).toInt()
        val my3 = ((pp3.y / height) + 1).toInt()

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
                val rx = if (repeatX) x umod 1 else x
                val ry = if (repeatY) y umod 1 else y
                if (rx < 0 || rx >= 1) continue
                if (ry < 0 || ry >= 1) continue

                count++

                val info = verticesPerTex.getOrPut(bitmap.bmpBase) {
                    infosPool.alloc().also { info ->
                        info.tex = bitmap.bmpBase
                        if (info.vertices.initialVcount < allocTiles * 4) {
                            info.vertices =
                                TexturedVertexArray(allocTiles * 4, TexturedVertexArray.quadIndices(allocTiles))
                            //println("ALLOC TexturedVertexArray")
                        }
                        info.vcount = 0
                        info.icount = 0
                        infos += info
                    }
                }

                run {
                    val p0X = posX + (nextTextureX * x) + (dVX * y)
                    val p0Y = posY + (dUY * x) + (nextTextureY * y) + initY
                    val p1X = p0X + dUX
                    val p1Y = p0Y + dUY
                    val p2X = p0X + dUX + dVX
                    val p2Y = p0Y + dUY + dVY
                    val p3X = p0X + dVX
                    val p3Y = p0Y + dVY

                    tempX[0] = bitmap.tl_x
                    tempX[1] = bitmap.tr_x
                    tempX[2] = bitmap.br_x
                    tempX[3] = bitmap.bl_x
                    tempY[0] = bitmap.tl_y
                    tempY[1] = bitmap.tr_y
                    tempY[2] = bitmap.br_y
                    tempY[3] = bitmap.bl_y

                    computeIndices(indices = indices)

                    info.vertices.quadV(info.vcount++, p0X, p0Y, tempX[indices[0]], tempY[indices[0]], colMul, colAdd)
                    info.vertices.quadV(info.vcount++, p1X, p1Y, tempX[indices[1]], tempY[indices[1]], colMul, colAdd)
                    info.vertices.quadV(info.vcount++, p2X, p2Y, tempX[indices[2]], tempY[indices[2]], colMul, colAdd)
                    info.vertices.quadV(info.vcount++, p3X, p3Y, tempX[indices[3]], tempY[indices[3]], colMul, colAdd)
                }
                info.icount += 6
            }
        }
        renderTilesCounter.increment(count)
    }

    private val indices = IntArray(4)
    private val tempX = FloatArray(4)
    private val tempY = FloatArray(4)

    // @TODO: Use a TextureVertexBuffer or something
    @KorgeInternal
    private class Info(var tex: Bitmap, var vertices: TexturedVertexArray) {
        var vcount = 0
        var icount = 0
    }

    private val verticesPerTex = FastIdentityMap<Bitmap, Info>()
    private val infos = arrayListOf<Info>()

    companion object {
        private val dummyTexturedVertexArray = TexturedVertexArray.EMPTY

        fun computeIndices(indices: IntArray = IntArray(4)): IntArray {
            // @TODO: const val optimization issue in Kotlin/Native: https://youtrack.jetbrains.com/issue/KT-46425
            indices[0] = 0 // 0/*TL*/
            indices[1] = 1 // 1/*TR*/
            indices[2] = 2 // 2/*BR*/
            indices[3] = 3 // 3/*BL*/
            return indices
        }

    }

    private val infosPool = Pool { Info(Bitmaps.transparent.bmpBase, dummyTexturedVertexArray) }
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

        ctx.useBatcher { batch ->
            infos.fastForEach { buffer ->
                batch.drawVertices(buffer.vertices, ctx.getTex(buffer.tex), smoothing, renderBlendMode.factors, buffer.vcount, buffer.icount)
            }
        }
    }
}
