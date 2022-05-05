package com.soywiz.korge.view

import com.soywiz.korge.view.vector.*
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
// @TODO: This should probably use
//) : Graphics(autoScaling = autoScaling) {
) : GpuShapeView(autoScaling = autoScaling) {
    var path: VectorPath? = shape
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
    inline fun updatePath(block: VectorPath.() -> Unit) {
        block(internalShape)
        path = internalShape
    }

    private fun updateGraphics() {
        updateShape {
            val shapeView = this@ShapeView
            val shape = shapeView.path
            if (shape != null && shape.isNotEmpty()) {
                if (shapeView.strokeThickness != 0.0) {
                    this.fillStroke(shapeView.fill, shapeView.stroke, StrokeInfo(thickness = shapeView.strokeThickness)) {
                        this.path(shape)
                    }
                } else {
                    this.fill(shapeView.fill) {
                        this.path(shape)
                    }
                }
            }
        }
    }
}
