package com.soywiz.korge.view

import com.soywiz.korim.color.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.vector.*

inline fun Container.shapeView(
    shape: VectorPath? = null,
    fill: Paint = Colors.WHITE,
    stroke: Paint = Colors.WHITE,
    strokeThickness: Double = 0.0,
    autoScaling: Boolean = true,
    callback: @ViewDslMarker ShapeView.() -> Unit = {}
): ShapeView = ShapeView(shape, fill, stroke, strokeThickness, autoScaling).addTo(this, callback)

open class ShapeView(
    shape: VectorPath? = null,
    fill: Paint = Colors.WHITE,
    stroke: Paint = Colors.WHITE,
    strokeThickness: Double = 0.0,
    autoScaling: Boolean = true,
) : Graphics(autoScaling = autoScaling) {
    var shape: VectorPath? = shape
        set(value) {
            field = value
            updateGraphics()
        }
    var fill: Paint = fill
        set(value) {
            if (field != value) {
                field = value
                updateGraphics()
            }
        }
    var stroke: Paint = stroke
        set(value) {
            if (field != value) {
                field = value
                updateGraphics()
            }
        }
    var strokeThickness: Double = strokeThickness
        set(value) {
            if (field != value) {
                field = value
                updateGraphics()
            }
        }

    init {
        updateGraphics()
    }

    @PublishedApi
    internal val internalShape = VectorPath()
    inline fun updateShape(block: VectorPath.() -> Unit) {
        block(internalShape)
        shape = internalShape
    }

    private fun updateGraphics() {
        val shape = this.shape
        clear()
        if (shape != null && shape.isNotEmpty()) {
            if (strokeThickness != 0.0) {
                fillStroke(this@ShapeView.fill, this@ShapeView.stroke, StrokeInfo(thickness = strokeThickness)) {
                    this.path(shape)
                }
            } else {
                fill(this@ShapeView.fill) {
                    this.path(shape)
                }
            }
        }
    }
}
