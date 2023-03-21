package com.soywiz.korim.vector

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.font.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.vector.renderer.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.contracts.*

fun VectorPath.toFillShape(paint: Paint): Shape = buildShape { fill(paint) { write(this@toFillShape) } }
fun VectorPath.toStrokeShape(paint: Paint, info: StrokeInfo = StrokeInfo()): Shape = buildShape { stroke(paint, info) { write(this@toStrokeShape) } }
fun VectorPath.toStrokeShape(
    paint: Paint,
    thickness: Double = 1.0,
    pixelHinting: Boolean = false,
    scaleMode: LineScaleMode = LineScaleMode.NORMAL,
    startCap: LineCap = LineCap.BUTT,
    endCap: LineCap = LineCap.BUTT,
    lineJoin: LineJoin = LineJoin.MITER,
    miterLimit: Double = 20.0
): Shape = buildShape { stroke(paint, StrokeInfo(thickness, pixelHinting, scaleMode, startCap, endCap, lineJoin, miterLimit)) { write(this@toStrokeShape) } }

@OptIn(ExperimentalContracts::class)
inline fun buildShape(width: Int? = null, height: Int? = null, builder: ShapeBuilder.() -> Unit): Shape {
    contract { callsInPlace(builder, kotlin.contracts.InvocationKind.EXACTLY_ONCE) }
    return ShapeBuilder(width, height).apply(builder).buildShape()
}

open class ShapeBuilder(width: Int?, height: Int?) : Context2d(DummyRenderer), Drawable {
    override val rendererWidth: Int = width ?: 256
    override val rendererHeight: Int = height ?: 256

    val shapes = arrayListOf<Shape>()

    override fun rendererRender(state: State, fill: Boolean, winding: Winding?) {
        if (state.path.isEmpty()) return

        val path = state.path.clone().also { it.winding = winding ?: it.winding }
        val clip = state.clip?.clone()?.also { it.winding = winding ?: it.winding }
        val transform = state.transform
        if (fill) {
            shapes += FillShape(
                path = path,
                clip = clip,
                paint = state.fillStyle.clone(),
                transform = transform.mutable,
                globalAlpha = state.globalAlpha,
            )
        } else {
            shapes += PolylineShape(
                path = path,
                clip = clip,
                paint = state.strokeStyle.clone(),
                transform = transform.mutable,
                StrokeInfo(
                    thickness = state.lineWidth,
                    pixelHinting = true,
                    scaleMode = state.lineScaleMode,
                    startCap = state.startLineCap,
                    endCap = state.endLineCap,
                    join = state.lineJoin,
                    dash = state.lineDash,
                    dashOffset = state.lineDashOffset,
                    miterLimit = state.miterLimit,
                ),
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
            transform = state.transform.mutable
        )
    }

    override fun rendererDrawImage(image: Bitmap, x: Double, y: Double, width: Double, height: Double, transform: Matrix) {
        rendererRender(
            State(
                transform = transform,
                path = VectorPath().apply { rect(x, y, width, height) },
                fillStyle = BitmapPaint(
                    image,
                    transform = Matrix.IDENTITY
                        .scaled(width / image.width.toDouble(), height / image.height.toDouble())
                        .translated(x, y)
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
    fun buildShape(): Shape {
        return when (shapes.size) {
            1 -> shapes.first()
            else -> CompoundShape(shapes.toList())
        }
    }

    override fun draw(c: Context2d) {
        c.draw(buildShape())
    }
}
