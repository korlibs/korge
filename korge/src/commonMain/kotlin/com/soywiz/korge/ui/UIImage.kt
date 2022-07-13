package com.soywiz.korge.ui

import com.soywiz.kmem.clamp
import com.soywiz.kmem.convertRange
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.TexturedVertexArray
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.ViewDslMarker
import com.soywiz.korge.view.addTo
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.ScaleMode
import com.soywiz.korma.geom.applyScaleMode
import com.soywiz.korma.geom.size

inline fun Container.uiImage(
    width: Number = UI_DEFAULT_WIDTH,
    height: Number = UI_DEFAULT_HEIGHT,
    bitmap: BmpSlice = Bitmaps.transparent,
    scaleMode: ScaleMode = ScaleMode.NO_SCALE,
    contentAnchor: Anchor = Anchor.TOP_LEFT,
    block: @ViewDslMarker UIImage.() -> Unit = {}
): UIImage = UIImage(width.toDouble(), height.toDouble(), bitmap, scaleMode, contentAnchor).addTo(this).apply(block)

class UIImage(
    width: Double, height: Double,
    bitmap: BmpSlice,
    scaleMode: ScaleMode = ScaleMode.NO_SCALE,
    contentAnchor: Anchor = Anchor.TOP_LEFT,
) : UIView(width, height) {
    private val cachedGlobalMatrix = Matrix()
    private var validCoords: Boolean = false

    var bgcolor: RGBA = Colors.TRANSPARENT_BLACK
    var smoothing: Boolean = true
    var bitmap: BmpSlice = bitmap ; set(value) { if (field !== value) { field = value; validCoords = false } }
    var scaleMode: ScaleMode = scaleMode ; set(value) { if (field !== value) { field = value; validCoords = false } }
    var contentAnchor: Anchor = contentAnchor ; set(value) { if (field !== value) { field = value; validCoords = false } }
    override fun onSizeChanged() {
        validCoords = false
    }

    private val vertices = TexturedVertexArray(4, TexturedVertexArray.QUAD_INDICES)

    override fun renderInternal(ctx: RenderContext) {
        if (!validCoords || cachedGlobalMatrix != globalMatrix) {
            validCoords = true
            cachedGlobalMatrix.copyFrom(globalMatrix)

            // @TODO: Can we generalize this to be placed in KorMA?
            val bitmapSize = bitmap.bounds.size.size
            val finalRect = bitmapSize.applyScaleMode(Rectangle(0.0, 0.0, width, height), scaleMode, contentAnchor)

            val realL = finalRect.left.clamp(0.0, width)
            val realT = finalRect.top.clamp(0.0, height)
            val realR = finalRect.right.clamp(0.0, width)
            val realB = finalRect.bottom.clamp(0.0, height)

            val ratioL = realL.convertRange(finalRect.left, finalRect.right, 0.0, 1.0).toFloat()
            val ratioR = realR.convertRange(finalRect.left, finalRect.right, 0.0, 1.0).toFloat()
            val ratioT = realT.convertRange(finalRect.top, finalRect.bottom, 0.0, 1.0).toFloat()
            val ratioB = realB.convertRange(finalRect.top, finalRect.bottom, 0.0, 1.0).toFloat()

            //println("finalRect=$finalRect, ratioL=$ratioL, ratioR=$ratioR, ratioT=$ratioT, ratioB=$ratioB")

            vertices.quad(
                0,
                realL.toFloat(), realT.toFloat(), (realR - realL).toFloat(), (realB - realT).toFloat(),
                globalMatrix,
                ratioL.convertRange(0f, 1f, bitmap.tl_x, bitmap.tr_x), ratioT.convertRange(0f, 1f, bitmap.tl_y, bitmap.bl_y),
                ratioR.convertRange(0f, 1f, bitmap.tl_x, bitmap.tr_x), ratioT.convertRange(0f, 1f, bitmap.tr_y, bitmap.br_y),
                ratioL.convertRange(0f, 1f, bitmap.bl_x, bitmap.br_x), ratioB.convertRange(0f, 1f, bitmap.tl_y, bitmap.bl_y),
                ratioR.convertRange(0f, 1f, bitmap.bl_x, bitmap.br_x), ratioB.convertRange(0f, 1f, bitmap.tr_y, bitmap.br_y),
                renderColorMul, renderColorAdd
            )
        }
        ctx.useBatcher { batch ->
            if (bgcolor.a != 0) batch.drawQuad(ctx.getTex(Bitmaps.white), 0f, 0f, width.toFloat(), height.toFloat(), globalMatrix, colorMul = bgcolor)
            batch.drawVertices(vertices, ctx.getTex(bitmap).base, smoothing, renderBlendMode)
        }
    }
}
