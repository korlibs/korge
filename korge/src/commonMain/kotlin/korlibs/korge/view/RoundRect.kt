package korlibs.korge.view

import korlibs.image.color.*
import korlibs.image.paint.*
import korlibs.korge.ui.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*

/** Creates a new [RoundRect] of size [width]x[height] and [color]
 *  and allows you to configure it via [callback].
 *  Once created, it is added to this receiver [Container].
 **/
inline fun Container.roundRect(
    size: Size,
    radius: RectCorners,
    fill: Paint = Colors.WHITE,
    stroke: Paint = Colors.WHITE,
    strokeThickness: Number = 0.0,
    autoScaling: Boolean = true,
    callback: @ViewDslMarker RoundRect.() -> Unit = {}
) = RoundRect(size, radius, fill, stroke, strokeThickness, autoScaling).addTo(this, callback)

/**
 * A Rect [View] with rounded corners of size [widthD] and [heightD] with the initial [color].
 */
class RoundRect(
    size: Size,
    radius: RectCorners,
    fill: Paint = Colors.WHITE,
    stroke: Paint = Colors.WHITE,
    strokeThickness: Number = 0.0,
    autoScaling: Boolean = true
) : ShapeView(shape = VectorPath(), fill = fill, stroke = stroke, strokeThickness = strokeThickness, autoScaling = autoScaling) {

    override var unscaledSize: Size by uiObservable(size) { updateGraphics() }
    var radius: RectCorners by uiObservable(radius) { updateGraphics() }

    /** The [color] of this [RoundRect]. Alias of [colorMul]. */
    var color: RGBA by ::colorMul

    init {
        updateGraphics()
    }

    private fun updateGraphics() {
        updatePath {
            clear()
            roundRect(RoundRectangle(Rectangle(0.0, 0.0, this@RoundRect.width, this@RoundRect.height), radius))
            assumeConvex = true // Optimization to avoid computing convexity
        }
    }
}
