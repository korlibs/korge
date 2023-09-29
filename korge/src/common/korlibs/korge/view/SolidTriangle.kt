package korlibs.korge.view

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.korge.render.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*

/** Creates a new [SolidTriangle] of [size] and color [color] and allows you to configure it via [callback]. Once created, it is added to this receiver [Container]. */
inline fun Container.solidTriangle(size: Size, color: RGBA = Colors.WHITE, callback: @ViewDslMarker SolidTriangle.() -> Unit = {}) = SolidTriangle(size, color).addTo(this, callback)

/** Creates a new [SolidTriangle] of size [width]x[height] and color [color] and allows you to configure it via [callback]. Once created, it is added to this receiver [Container]. */
inline fun Container.solidTriangle(width: Number, height: Number, color: RGBA = Colors.WHITE, callback: @ViewDslMarker SolidTriangle.() -> Unit = {}) = SolidTriangle(Size(width, height), color).addTo(this, callback)

/**
 * A Rect [TriBase] [View] of size [widthD] and [heightD] with the initial color, [color].
 */
class SolidTriangle(size: Size, color: RGBA = Colors.WHITE) : SolidRect(size, color) {
    // Temporary hacky solution
    // TODO add tri() method to TexturedVertexArray ---> then we can use shortArrayOf(0, 1, 2) here
    private val TEXTURED_ARRAY_TRI_INDICES = shortArrayOf(0, 1, 3,  0, 1, 3)
    override val vertices = TexturedVertexArray(4, TEXTURED_ARRAY_TRI_INDICES)
}
