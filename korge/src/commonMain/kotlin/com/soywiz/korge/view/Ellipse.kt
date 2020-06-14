package com.soywiz.korge.view

import com.soywiz.korge.ui.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.vector.*

/**
 * Creates a [Ellipse] of [radiusX], [radiusY] and [color].
 * The [autoScaling] determines if the underlying texture will be updated when the hierarchy is scaled.
 * The [callback] allows to configure the [Circle] instance.
 */
inline fun Container.ellipse(
    radiusX: Double = 16.0,
    radiusY: Double = 16.0,
    color: RGBA = Colors.WHITE,
    autoScaling: Boolean = true,
    callback: Ellipse.() -> Unit = {}
): Ellipse = Ellipse(radiusX, radiusY, color, autoScaling).addTo(this, callback)

/**
 * A [Graphics] class that automatically keeps a ellipse shape with [radiusX], [radiusY] and [color].
 * The [autoScaling] property determines if the underlying texture will be updated when the hierarchy is scaled.
 */
open class Ellipse(
    radiusX: Double = 16.0,
    radiusY: Double = 16.0,
    color: RGBA = Colors.WHITE,
    autoScaling: Boolean = true
) : Graphics(autoScaling = autoScaling) {

    /** Radius of the circle */
    var radiusX: Double by uiObservable(radiusX) { updateGraphics() }
    var radiusY: Double by uiObservable(radiusY) { updateGraphics() }
    /** Color of the circle. Internally it uses the [colorMul] property */
    var color: RGBA
        get() = colorMul
        set(value) { colorMul = value }

    override val bwidth get() = radiusX * 2
    override val bheight get() = radiusY * 2

    init {
        this.color = color
        updateGraphics()
    }

    private fun updateGraphics() {
        clear()
        fill(Colors.WHITE) {
            //ellipse(radiusX, radiusY, radiusX, radiusY)
            ellipse(0.0, 0.0, radiusX, radiusY)
        }
    }
}
