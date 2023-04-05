package korlibs.korge.view

import korlibs.image.bitmap.*
import korlibs.image.color.*

/** Creates a new [SolidRect] of size [width]x[height] and color [color] and allows you to configure it via [callback]. Once created, it is added to this receiver [Container]. */
inline fun Container.solidRect(width: Float, height: Float, color: RGBA = Colors.WHITE, callback: @ViewDslMarker SolidRect.() -> Unit = {})
    = SolidRect(width, height, color).addTo(this, callback)

inline fun Container.solidRect(width: Double, height: Double, color: RGBA = Colors.WHITE, callback: @ViewDslMarker SolidRect.() -> Unit = {})
    = SolidRect(width.toFloat(), height.toFloat(), color).addTo(this, callback)

/** Creates a new [SolidRect] of size [width]x[height] and color [color] and allows you to configure it via [callback]. Once created, it is added to this receiver [Container]. */
inline fun Container.solidRect(width: Int, height: Int, color: RGBA = Colors.WHITE, callback: @ViewDslMarker SolidRect.() -> Unit = {})
    = SolidRect(width.toFloat(), height.toFloat(), color).addTo(this, callback)

/**
 * A Rect [RectBase] [View] of size [widthD] and [heightD] with the initial color, [color].
 */
class SolidRect(width: Float, height: Float, color: RGBA = Colors.WHITE) : RectBase() {
	companion object {
        operator fun invoke(width: Int, height: Int, color: RGBA = Colors.WHITE) = SolidRect(width.toFloat(), height.toFloat(), color)
        operator fun invoke(width: Double, height: Double, color: RGBA = Colors.WHITE) = SolidRect(width.toFloat(), height.toFloat(), color)
	}

	override var width: Float = width; set(v) {
        if (field != v) {
            field = v
            dirtyVertices = true
            invalidateRender()
        }
    }
	override var height: Float = height; set(v) {
       if (field != v) {
           field = v
           dirtyVertices = true
           invalidateRender()
       }
    }

    override val bwidth: Float get() = width
    override val bheight: Float get() = height

    /**
     * Allows to store a white bitmap in an atlas along for example a bitmap font to render this rect
     * in a single batch reusing the texture.
     */
    var whiteBitmap: BitmapCoords get() = baseBitmap; set(v) { baseBitmap = v }

    /** The [color] of this [SolidRect]. Alias of [colorMul]. */
    var color: RGBA
        set(value) { colorMul = value }
        get() = colorMul

	init {
		this.colorMul = color
	}

	override fun createInstance(): View = SolidRect(width, height, colorMul)
}
