package com.soywiz.korge.view

import com.soywiz.korim.color.*
import com.soywiz.korma.geom.vector.*

inline fun Container.circle(radius: Double = 16.0, color: RGBA = Colors.WHITE, autoScaling: Boolean = true, callback: Circle.() -> Unit = {}): Circle = Circle(radius, color, autoScaling).addTo(this).apply(callback)

open class Circle(radius: Double = 16.0, color: RGBA = Colors.WHITE, autoScaling: Boolean = true) : Graphics(autoScaling = autoScaling) {
    var radius: Double = radius
        set(value) {
            field = value; updateGraphics()
        }

    var color: RGBA = color
        set(value) {
            field = value; updateGraphics()
        }

    init {
        updateGraphics()
    }

    private fun updateGraphics() {
        clear()
        fill(color) {
            circle(0.0, 0.0, radius)
        }
    }
}
