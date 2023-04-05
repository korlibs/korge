package korlibs.korge.view

import korlibs.image.vector.*
import korlibs.korge.ui.*
import korlibs.math.geom.*

inline fun Container.ninePatchShapeView(
    shape: NinePatchShape,
    renderer: GraphicsRenderer = GraphicsRenderer.SYSTEM,
    callback: @ViewDslMarker NinePatchShapeView.() -> Unit = {}
): NinePatchShapeView = NinePatchShapeView(shape, renderer).addTo(this, callback)

class NinePatchShapeView(
    shape: NinePatchShape,
    renderer: GraphicsRenderer,
) : UIView(shape.size.width, shape.size.height), Anchorable {
    private val graphics = graphics(shape.shape, renderer = renderer)
    var boundsIncludeStrokes: Boolean by graphics::boundsIncludeStrokes
    var antialiased: Boolean by graphics::antialiased
    var smoothing: Boolean by graphics::smoothing
    var autoScaling: Boolean by graphics::autoScaling
    override var anchor: Anchor by graphics::anchor
    var renderer: GraphicsRenderer by graphics::renderer

    var shape: NinePatchShape = shape
        set(value) {
            if (field == value) return
            field = value
            onSizeChanged()
        }

    override fun onSizeChanged() {
        super.onSizeChanged()
        graphics.shape = shape.transform(Size(widthD, heightD))
    }
}
