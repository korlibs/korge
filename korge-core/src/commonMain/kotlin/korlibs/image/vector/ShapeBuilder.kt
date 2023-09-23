package korlibs.image.vector

import korlibs.image.bitmap.*
import korlibs.image.font.*
import korlibs.image.paint.*
import korlibs.image.vector.renderer.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*
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
                transform = transform,
                globalAlpha = state.globalAlpha,
            )
        } else {
            shapes += PolylineShape(
                path = path,
                clip = clip,
                paint = state.strokeStyle.clone(),
                transform = transform,
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

    override fun rendererRenderSystemText(state: State, font: Font?, fontSize: Double, text: String, pos: Point, fill: Boolean) {
        shapes += TextShape(
            text = text,
            pos = pos,
            font = font,
            fontSize = fontSize,
            clip = state.clip?.clone(),
            fill = if (fill) state.fillStyle else null,
            stroke = if (fill) null else state.strokeStyle,
            align = state.alignment,
            //transform = Matrix()
            transform = state.transform
        )
    }

    override fun rendererDrawImage(image: Bitmap, pos: Point, size: Size, transform: Matrix) {
        rendererRender(
            State(
                transform = transform,
                path = VectorPath().apply { rect(pos, size) },
                fillStyle = BitmapPaint(
                    image,
                    transform = Matrix.IDENTITY
                        .scaled(size / image.size.toFloat())
                        .translated(pos)
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
