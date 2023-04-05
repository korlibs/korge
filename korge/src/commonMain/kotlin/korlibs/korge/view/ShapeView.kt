package korlibs.korge.view

import korlibs.korge.internal.*
import korlibs.korge.view.property.*
import korlibs.image.color.*
import korlibs.image.paint.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*

inline fun Container.shapeView(
    shape: VectorPath? = null,
    fill: Paint = Colors.WHITE,
    stroke: Paint = Colors.WHITE,
    strokeThickness: Double = 1.0,
    autoScaling: Boolean = true,
    renderer: GraphicsRenderer = GraphicsRenderer.GPU,
    callback: @ViewDslMarker ShapeView.() -> Unit = {}
): ShapeView = ShapeView(shape, fill, stroke, strokeThickness, autoScaling, renderer = renderer).addTo(this, callback)

open class ShapeView(
    shape: VectorPath? = null,
    fill: Paint = Colors.WHITE,
    stroke: Paint = Colors.WHITE,
    strokeThickness: Double = 1.0,
    autoScaling: Boolean = true,
    //renderer: GraphicsRenderer = GraphicsRenderer.SYSTEM
    renderer: GraphicsRenderer = GraphicsRenderer.GPU
) : Container(), Anchorable, ViewLeaf {
    internal val shapeView = Graphics(renderer = renderer).addTo(this)
    init {
        shapeView.autoScaling = autoScaling
    }

    override var anchor: Anchor by shapeView::anchor
    @KorgeInternal override val anchorDispX: Float get() = shapeView.anchorDispX
    @KorgeInternal override val anchorDispY: Float get() = shapeView.anchorDispY

    @ViewProperty
    var antialiased: Boolean by shapeView::antialiased
    @ViewProperty
    var autoScaling: Boolean by shapeView::autoScaling
    @ViewProperty
    var renderer: GraphicsRenderer by shapeView::renderer
    @ViewProperty
    var smoothing: Boolean by shapeView::smoothing
    @ViewProperty
    var boundsIncludeStrokes: Boolean by shapeView::boundsIncludeStrokes

    @PublishedApi
    internal var _path: VectorPath? = shape

    @ViewProperty
    var path: VectorPath?
        get() = _path
        set(value) {
            if (_path == value) return
            _path = value
            _updateShapeGraphics()
        }
    @ViewProperty
    var fill: Paint = fill
        set(value) {
            if (field == value) return
            field = value
            _updateShapeGraphics()
        }
    @ViewProperty
    var stroke: Paint = stroke
        set(value) {
            if (field == value) return
            field = value
            _updateShapeGraphics()
        }
    @ViewProperty
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

    @Deprecated("Use updatePath instead", ReplaceWith("updatePath(block)"))
    inline fun updateShape(block: VectorPath.() -> Unit) = updatePath(block)

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
