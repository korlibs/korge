package korlibs.korge.view

import korlibs.korge.ui.*
import korlibs.image.color.*
import korlibs.image.paint.Paint
import korlibs.math.geom.vector.*

/** Creates a new [RoundRect] of size [width]x[height] and [color]
 *  and allows you to configure it via [callback].
 *  Once created, it is added to this receiver [Container].
 **/
inline fun Container.roundRect(
    width: Int,
    height: Int,
    rx: Int,
    ry: Int = rx,
    fill: Paint = Colors.WHITE,
    stroke: Paint = Colors.WHITE,
    strokeThickness: Double = 0.0,
    autoScaling: Boolean = true,
    callback: @ViewDslMarker RoundRect.() -> Unit = {}
) = roundRect(width.toDouble(), height.toDouble(), rx.toDouble(), ry.toDouble(), fill, stroke, strokeThickness, autoScaling, callback)

inline fun Container.roundRect(
    width: Double,
    height: Double,
    rx: Double,
    ry: Double = rx,
    fill: Paint = Colors.WHITE,
    stroke: Paint = Colors.WHITE,
    strokeThickness: Double = 0.0,
    autoScaling: Boolean = true,
    callback: @ViewDslMarker RoundRect.() -> Unit = {}
) = RoundRect(width, height, rx, ry, fill, stroke, strokeThickness, autoScaling).addTo(this, callback)

/**
 * A Rect [View] with rounded corners of size [width] and [height] with the initial [color].
 */
class RoundRect(
    width: Double,
    height: Double,
    rx: Double,
    ry: Double = rx,
    fill: Paint = Colors.WHITE,
    stroke: Paint = Colors.WHITE,
    strokeThickness: Double = 0.0,
    autoScaling: Boolean = true
) : ShapeView(shape = VectorPath(), fill = fill, stroke = stroke, strokeThickness = strokeThickness, autoScaling = autoScaling) {

    override var width: Double by uiObservable(width) { updateGraphics() }
    override var height: Double by uiObservable(height) { updateGraphics() }

    var rx: Double by uiObservable(rx) { updateGraphics() }
    var ry: Double by uiObservable(ry) { updateGraphics() }

    /** The [color] of this [RoundRect]. Alias of [colorMul]. */
    var color: RGBA by ::colorMul

    init {
        updateGraphics()
    }

    private fun updateGraphics() {
        updatePath {
            clear()
            roundRect(0.0, 0.0, this@RoundRect.width, this@RoundRect.height, this@RoundRect.rx, this@RoundRect.ry)
            assumeConvex = true // Optimization to avoid computing convexity
        }
    }
}