package com.soywiz.korge.view

import com.soywiz.korim.color.Colors
import com.soywiz.korim.paint.Paint
import com.soywiz.korma.geom.vector.StrokeInfo
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.geom.vector.isNotEmpty

inline fun Container.shapeView(
    shape: VectorPath? = null,
    fill: Paint = Colors.WHITE,
    stroke: Paint = Colors.WHITE,
    strokeThickness: Double = 1.0,
    autoScaling: Boolean = true,
    callback: @ViewDslMarker ShapeView.() -> Unit = {}
): ShapeView = ShapeView(shape, fill, stroke, strokeThickness, autoScaling).addTo(this, callback)

open class ShapeView(
    shape: VectorPath? = null,
    fill: Paint = Colors.WHITE,
    stroke: Paint = Colors.WHITE,
    strokeThickness: Double = 1.0,
    autoScaling: Boolean = true,
    //renderer: GraphicsRenderer = GraphicsRenderer.SYSTEM
    renderer: GraphicsRenderer = GraphicsRenderer.GPU
) : Container(), Anchorable {
    internal val shapeView = Graphics(renderer = renderer).addTo(this)
    init {
        shapeView.autoScaling = autoScaling
    }

    override var anchorX: Double by shapeView::anchorX
    override var anchorY: Double by shapeView::anchorY
    var autoScaling: Boolean by shapeView::autoScaling
    var renderer: GraphicsRenderer by shapeView::renderer
    var smoothing: Boolean by shapeView::smoothing
    var boundsIncludeStrokes: Boolean by shapeView::boundsIncludeStrokes

    @PublishedApi
    internal var _path: VectorPath? = shape

    var path: VectorPath?
        get() = _path
        set(value) {
            if (_path == value) return
            _path = value
            _updateShapeGraphics()
        }
    var fill: Paint = fill
        set(value) {
            if (field == value) return
            field = value
            _updateShapeGraphics()
        }
    var stroke: Paint = stroke
        set(value) {
            if (field == value) return
            field = value
            _updateShapeGraphics()
        }
    var strokeThickness: Double = strokeThickness
        set(value) {
            if (field == value) return
            field = value
            _updateShapeGraphics()
        }

    init {
        _updateShapeGraphics()
    }

    @PublishedApi
    internal val internalShape = VectorPath()
    inline fun updatePath(block: VectorPath.() -> Unit) {
        block(internalShape)
        _path = internalShape
        _updateShapeGraphics()
    }

    @PublishedApi
    internal fun _updateShapeGraphics() {
        shapeView.updateShape {
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
