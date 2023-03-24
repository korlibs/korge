package korlibs.korge.view

import korlibs.korge.ui.*
import korlibs.korge.view.property.*
import korlibs.image.color.*
import korlibs.image.paint.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*

/**
 * Creates a [Ellipse] of [radiusX], [radiusY] and [fill].
 * The [autoScaling] determines if the underlying texture will be updated when the hierarchy is scaled.
 * The [callback] allows to configure the [Circle] instance.
 */
inline fun Container.ellipse(
    radiusX: Double = 16.0,
    radiusY: Double = 16.0,
    fill: Paint = Colors.WHITE,
    stroke: Paint = Colors.WHITE,
    strokeThickness: Double = 0.0,
    autoScaling: Boolean = true,
    callback: @ViewDslMarker Ellipse.() -> Unit = {}
): Ellipse = Ellipse(radiusX, radiusY, fill, stroke, strokeThickness, autoScaling).addTo(this, callback)

/**
 * A [CpuGraphics] class that automatically keeps a ellipse shape with [radiusX], [radiusY] and [color].
 * The [autoScaling] property determines if the underlying texture will be updated when the hierarchy is scaled.
 */
open class Ellipse(
    radiusX: Double = 16.0,
    radiusY: Double = 16.0,
    fill: Paint = Colors.WHITE,
    stroke: Paint = Colors.WHITE,
    strokeThickness: Double = 0.0,
    autoScaling: Boolean = true,
) : ShapeView(shape = VectorPath(), fill = fill, stroke = stroke, strokeThickness = strokeThickness, autoScaling = autoScaling) {
    /** Radius of the circle */
    var radiusX: Double by uiObservable(radiusX) { updateGraphics() }
    var radiusY: Double by uiObservable(radiusY) { updateGraphics() }

    @Suppress("unused")
    @ViewProperty(min = 0.0, max = 1000.0, name = "radius")
    private var radiusXY: Pair<Double, Double>
        get() = radiusX to radiusY
        set(value) {
            radiusX = value.first
            radiusY = value.second
        }

    val isCircle get() = radiusX == radiusY
    /** Color of the circle. Internally it uses the [colorMul] property */
    var color: RGBA
        get() = colorMul
        set(value) { colorMul = value }

    override var width: Double
        get() = radiusX * 2
        set(value) {
            radiusX = value / 2
        }
    override var height: Double
        get() = radiusY * 2
        set(value) {
            radiusY = value / 2
        }

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