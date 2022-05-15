package com.soywiz.korim.vector

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.font.Font
import com.soywiz.korim.paint.BitmapPaint
import com.soywiz.korim.vector.renderer.DummyRenderer
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.vector.isEmpty
import com.soywiz.korma.geom.vector.rect

inline fun buildShape(width: Int? = null, height: Int? = null, builder: ShapeBuilder.() -> Unit): Shape =
    ShapeBuilder(width, height).apply(builder).buildShape()

open class ShapeBuilder(width: Int?, height: Int?) : Context2d(DummyRenderer), Drawable {
    override val rendererWidth: Int = width ?: 256
    override val rendererHeight: Int = height ?: 256

    val shapes = arrayListOf<Shape>()

    override fun rendererRender(state: State, fill: Boolean) {
        if (state.path.isEmpty()) return

        if (fill) {
            shapes += FillShape(
                path = state.path.clone(),
                clip = state.clip?.clone(),
                paint = state.fillStyle.clone(),
                transform = state.transform.clone(),
                globalAlpha = state.globalAlpha,
            )
        } else {
            shapes += PolylineShape(
                path = state.path.clone(),
                clip = state.clip?.clone(),
                paint = state.strokeStyle.clone(),
                transform = state.transform.clone(),
                thickness = state.lineWidth,
                pixelHinting = true,
                scaleMode = state.lineScaleMode,
                startCaps = state.startLineCap,
                endCaps = state.endLineCap,
                lineJoin = state.lineJoin,
                miterLimit = state.miterLimit,
                globalAlpha = state.globalAlpha,
            )
        }
    }

    override fun rendererRenderSystemText(state: State, font: Font?, fontSize: Double, text: String, x: Double, y: Double, fill: Boolean) {
        shapes += TextShape(
            text = text,
            x = x, y = y,
            font = font,
            fontSize = fontSize,
            clip = state.clip?.clone(),
            fill = if (fill) state.fillStyle else null,
            stroke = if (fill) null else state.strokeStyle,
            halign = state.horizontalAlign,
            valign = state.verticalAlign,
            //transform = Matrix()
            transform = state.transform.clone()
        )
    }

    override fun rendererDrawImage(image: Bitmap, x: Double, y: Double, width: Double, height: Double, transform: Matrix) {
        rendererRender(State(
            transform = transform,
            path = GraphicsPath().apply { rect(x, y, width.toDouble(), height.toDouble()) },
            fillStyle = BitmapPaint(image,
                transform = Matrix()
                    .scale(width.toDouble() / image.width.toDouble(), height.toDouble() / image.height.toDouble())
                    .translate(x, y)
            )
        ), fill = true)
    }

    override fun rendererDispose() {
    }

    override fun rendererBufferingStart(): Int {
        return 0
    }

    override fun rendererBufferingEnd() {
    }

    fun clear() {
        state.clone()
        shapes.clear()
    }
    fun buildShape(): Shape = if (shapes.size == 1) shapes.first() else CompoundShape(shapes.toList())

    override fun draw(c: Context2d) {
        c.draw(buildShape())
    }
}
