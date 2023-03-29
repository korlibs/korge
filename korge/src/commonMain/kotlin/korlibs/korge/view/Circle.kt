package korlibs.korge.view

import korlibs.korge.ui.*
import korlibs.image.color.*
import korlibs.image.paint.*
import korlibs.math.geom.*
import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*

/**
 * Creates a [Circle] of [radius] and [fill].
 * The [autoScaling] determines if the underlying texture will be updated when the hierarchy is scaled.
 * The [callback] allows to configure the [Circle] instance.
 */
inline fun Container.circle(
    radius: Double = 16.0,
    fill: Paint = Colors.WHITE,
    stroke: Paint = Colors.WHITE,
    strokeThickness: Double = 0.0,
    autoScaling: Boolean = true,
    renderer: GraphicsRenderer = GraphicsRenderer.GPU,
    callback: @ViewDslMarker Circle.() -> Unit = {}
): Circle = Circle(radius, fill, stroke, strokeThickness, autoScaling, renderer).addTo(this, callback)

/**
 * A [CpuGraphics] class that automatically keeps a circle shape with [radius] and [color].
 * The [autoScaling] property determines if the underlying texture will be updated when the hierarchy is scaled.
 */
open class Circle(
    radius: Double = 16.0,
    fill: Paint = Colors.WHITE,
    stroke: Paint = Colors.WHITE,
    strokeThickness: Double = 0.0,
    autoScaling: Boolean = true,
    renderer: GraphicsRenderer = GraphicsRenderer.GPU,
) : ShapeView(shape = VectorPath(), fill = fill, stroke = stroke, strokeThickness = strokeThickness, autoScaling = autoScaling, renderer = renderer) {
    /** Radius of the circle */
    var radius: Double by uiObservable(radius) { updateGraphics() }
    /** Color of the circle. Internally it uses the [colorMul] property */
    var color: RGBA by ::colorMul

    init {
        updateGraphics()
    }

    private fun updateGraphics() {
        val halfStroke = this@Circle.strokeThickness / 2
        val radius = this.radius
        hitShape2d = Circle(Point(radius, radius), radius.toFloat())
        //println("radius=$radius, halfStroke=$halfStroke")
        updatePath {
            clear()
            circle(Point(radius, radius), radius.toFloat())
            assumeConvex = true // Optimization to avoid computing convexity
            //circle(radius + halfStroke, radius + halfStroke, radius)
            //println(toSvgString())
        }
    }
}
