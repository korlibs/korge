package korlibs.korge.view

import korlibs.korge.render.RenderContext
import korlibs.korge.render.useLineBatcher
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.math.geom.vector.VectorPath

inline fun Container.outline(vectorPath: VectorPath, x: Double = 0.0, y: Double = 0.0, color: RGBA = Colors.WHITE, callback: @ViewDslMarker Outline.() -> Unit = {})
    = Outline(vectorPath, x, y, color).addTo(this, callback)

class Outline(
    vectorPath: VectorPath,
    x: Double = 0.0,
    y: Double = 0.0,
    color: RGBA = Colors.WHITE,
) : View() {
    var vectorPath: VectorPath = vectorPath
        set(value) {
            field = value
            invalidateVectorPath()
        }

    init {
        this.x = x
        this.y = y
        colorMul = color
    }

    override fun renderInternal(ctx: RenderContext) {
        ctx.useLineBatcher { lines ->
            lines.drawWithGlobalMatrix(globalMatrix.mutableOrNull) {
                lines.color(renderColorMul) {
                    lines.drawVector(vectorPath)
                }
            }
        }
    }

    private fun invalidateVectorPath() {
    }
}
