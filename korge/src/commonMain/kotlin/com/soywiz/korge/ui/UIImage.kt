package com.soywiz.korge.ui

import com.soywiz.kmem.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.property.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*

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
    private val cachedGlobalMatrix = MMatrix()
    private var validCoords: Boolean = false

    @ViewProperty
    var bgcolor: RGBA = Colors.TRANSPARENT

    @ViewProperty
    var smoothing: Boolean = true

    @ViewProperty
    var bitmap: BmpSlice = bitmap
        set(value) {
            if (field !== value) {
                field = value; validCoords = false
            }
        }

    @ViewProperty
    @ViewPropertyProvider(ScaleMode.Provider::class)
    var scaleMode: ScaleMode = scaleMode
        set(value) {
            if (field !== value) {
                field = value; validCoords = false
            }
        }

    @ViewProperty
    var contentAnchor: Anchor = contentAnchor
        set(value) {
            if (field != value) {
                field = value; validCoords = false
            }
        }

    override fun onSizeChanged() {
        validCoords = false
    }

    private val vertices = TexturedVertexArray(4, TexturedVertexArray.QUAD_INDICES)

    override fun renderInternal(ctx: RenderContext) {
        if (!validCoords || cachedGlobalMatrix != globalMatrix) {
            validCoords = true
            cachedGlobalMatrix.copyFrom(globalMatrix)

            // @TODO: Can we generalize this to be placed in KorMA?
            val bitmapSize = bitmap.bounds.size.toFloat()
            val finalRect = bitmapSize.applyScaleMode(Rectangle(0.0, 0.0, width, height), scaleMode, contentAnchor)

            val realL = finalRect.left.clamp(0f, width.toFloat())
            val realT = finalRect.top.clamp(0f, height.toFloat())
            val realR = finalRect.right.clamp(0f, width.toFloat())
            val realB = finalRect.bottom.clamp(0f, height.toFloat())

            val ratioL = realL.convertRange(finalRect.left, finalRect.right, 0f, 1f)
            val ratioR = realR.convertRange(finalRect.left, finalRect.right, 0f, 1f)
            val ratioT = realT.convertRange(finalRect.top, finalRect.bottom, 0f, 1f)
            val ratioB = realB.convertRange(finalRect.top, finalRect.bottom, 0f, 1f)

            //println("finalRect=$finalRect, ratioL=$ratioL, ratioR=$ratioR, ratioT=$ratioT, ratioB=$ratioB")

            vertices.quad(
                0,
                realL.toFloat(),
                realT.toFloat(),
                (realR - realL).toFloat(),
                (realB - realT).toFloat(),
                globalMatrix,
                ratioL.convertRange(0f, 1f, bitmap.tlX, bitmap.trX),
                ratioT.convertRange(0f, 1f, bitmap.tlY, bitmap.blY),
                ratioR.convertRange(0f, 1f, bitmap.tlX, bitmap.trX),
                ratioT.convertRange(0f, 1f, bitmap.trY, bitmap.brY),
                ratioL.convertRange(0f, 1f, bitmap.blX, bitmap.brX),
                ratioB.convertRange(0f, 1f, bitmap.tlY, bitmap.blY),
                ratioR.convertRange(0f, 1f, bitmap.blX, bitmap.brX),
                ratioB.convertRange(0f, 1f, bitmap.trY, bitmap.brY),
                renderColorMul,
            )
        }
        ctx.useBatcher { batch ->
            if (bgcolor.a != 0) batch.drawQuad(
                ctx.getTex(Bitmaps.white),
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                globalMatrix,
                colorMul = bgcolor
            )
            batch.drawVertices(
                vertices,
                ctx.getTex(bitmap).base,
                smoothing,
                renderBlendMode,
            )
        }
    }
}
