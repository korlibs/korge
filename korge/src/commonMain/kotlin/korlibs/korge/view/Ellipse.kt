package korlibs.korge.view

import korlibs.image.color.*
import korlibs.image.paint.*
import korlibs.korge.ui.*
import korlibs.korge.view.property.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*

/**
 * Creates a [Ellipse] of [radiusX], [radiusY] and [fill].
 * The [autoScaling] determines if the underlying texture will be updated when the hierarchy is scaled.
 * The [callback] allows to configure the [Circle] instance.
 */
inline fun Container.ellipse(
    radius: Size = Size(16, 16),
    fill: Paint = Colors.WHITE,
    stroke: Paint = Colors.WHITE,
    strokeThickness: Float = 0f,
    autoScaling: Boolean = true,
    callback: @ViewDslMarker Ellipse.() -> Unit = {}
): Ellipse = Ellipse(radius, fill, stroke, strokeThickness, autoScaling).addTo(this, callback)

/**
 * A [CpuGraphics] class that automatically keeps a ellipse shape with [radiusX], [radiusY] and [color].
 * The [autoScaling] property determines if the underlying texture will be updated when the hierarchy is scaled.
 */
open class Ellipse(
    radius: Size = Size(16, 16),
    fill: Paint = Colors.WHITE,
    stroke: Paint = Colors.WHITE,
    strokeThickness: Float = 0f,
    autoScaling: Boolean = true,
) : ShapeView(shape = VectorPath(), fill = fill, stroke = stroke, strokeThickness = strokeThickness, autoScaling = autoScaling) {
    /** Radius of the circle */
    @ViewProperty(min = 0.0, max = 1000.0, name = "radius")
    var radius: Size by uiObservable(radius) { updateGraphics() }

    val isCircle get() = radius.width == radius.height
    /** Color of the circle. Internally it uses the [colorMul] property */
    var color: RGBA
        get() = colorMul
        set(value) { colorMul = value }

    override var unscaledSize: Size
        get() = radius * 2
        set(value) { radius = value / 2 }

    init {
        updateGraphics()
    }

    private fun updateGraphics() {
        updatePath {
            clear()
            ellipse(Point(0, 0), Size(this@Ellipse.width, this@Ellipse.height))
        }
    }
}
