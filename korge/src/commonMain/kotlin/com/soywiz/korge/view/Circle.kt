package com.soywiz.korge.view

import com.soywiz.korge.ui.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.vector.*

/**
 * Creates a [Circle] of [radius] and [color].
 * The [autoScaling] determines if the underlying texture will be updated when the hierarchy is scaled.
 * The [callback] allows to configure the [Circle] instance.
 */
inline fun Container.circle(
    radius: Double = 16.0,
    color: RGBA = Colors.WHITE,
    autoScaling: Boolean = true,
    callback: Circle.() -> Unit = {}
): Circle = Circle(radius, color, autoScaling).addTo(this, callback)

/**
 * A [Graphics] class that automatically keeps a circle shape with [radius] and [color].
 * The [autoScaling] property determines if the underlying texture will be updated when the hierarchy is scaled.
 */
open class Circle(
    radius: Double = 16.0,
    color: RGBA = Colors.WHITE,
    autoScaling: Boolean = true
) : Graphics(autoScaling = autoScaling) {

    /** Radius of the circle */
    var radius: Double by uiObservable(radius) { updateGraphics() }
    /** Color of the circle. Internally it uses the [colorMul] property */
    var color: RGBA
        get() = colorMul
        set(value) { colorMul = value }

    override val bwidth get() = radius * 2
    override val bheight get() = radius * 2

    init {
        this.color = color
        updateGraphics()
    }

    private fun updateGraphics() {
        clear()
        fill(Colors.WHITE) {
            circle(radius, radius, radius)
        }
    }
}
