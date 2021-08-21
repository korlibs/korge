package com.soywiz.korge.view

import com.soywiz.korge.debug.*
import com.soywiz.korge.ui.*
import com.soywiz.korim.color.*
import com.soywiz.korim.paint.Paint
import com.soywiz.korma.geom.vector.*
import com.soywiz.korui.*

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
 * A [Graphics] class that automatically keeps a ellipse shape with [radiusX], [radiusY] and [color].
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
        updateShape {
            clear()
            ellipse(0.0, 0.0, this@Ellipse.width, this@Ellipse.height)
        }
    }

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        val view = this
        container.uiCollapsibleSection("Ellipse") {
            uiEditableValue(Pair(view::radiusX, view::radiusY), min = 0.0, max = 1000.0, clamp = false, name = "radius")
        }
        super.buildDebugComponent(views, container)
    }
}
