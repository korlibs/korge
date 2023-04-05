package korlibs.korge.view

import korlibs.korge.render.RenderContext
import korlibs.korge.render.useLineBatcher
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.math.geom.*

inline fun Container.line(a: MPoint, b: MPoint, color: RGBA = Colors.WHITE, callback: @ViewDslMarker Line.() -> Unit = {})
    = Line(a.x, a.y, b.x, b.y, color).addTo(this, callback)
inline fun Container.line(a: Point, b: Point, color: RGBA = Colors.WHITE, callback: @ViewDslMarker Line.() -> Unit = {})
    = Line(a.x.toDouble(), a.y.toDouble(), b.x.toDouble(), b.y.toDouble(), color).addTo(this, callback)

inline fun Container.line(x0: Double, y0: Double, x1: Double, y1: Double, color: RGBA = Colors.WHITE, callback: @ViewDslMarker Line.() -> Unit = {})
    = Line(x0, y0, x1, y1, color).addTo(this, callback)

class Line(
    x1: Double,
    y1: Double,
    var x2: Double,
    var y2: Double,
    color: RGBA = Colors.WHITE,
) : View() {
    var x1: Double get() = xD ; set(value) { xD = value }
    var y1: Double get() = yD ; set(value) { yD = value }

    init {
        xD = x1
        yD = y1
        colorMul = color
    }

    fun setPoints(a: Point, b: Point) = setPoints(a.xD, a.yD, b.xD, b.yD)

    fun setPoints(x1: Double, y1: Double, x2: Double, y2: Double) {
        this.x1 = x1
        this.y1 = y1
        this.x2 = x2
        this.y2 = y2
    }

    override fun renderInternal(ctx: RenderContext) {
        ctx.useLineBatcher { lines ->
            lines.drawWithGlobalMatrix(globalMatrix) {
                val col = renderColorMul
                lines.line(0.0, 0.0, x2 - x1, y2 - y1, col, col)
            }
        }
    }
}
