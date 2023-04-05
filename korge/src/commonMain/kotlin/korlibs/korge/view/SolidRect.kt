package korlibs.korge.view

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.math.geom.*

/** Creates a new [SolidRect] of [size] and color [color] and allows you to configure it via [callback]. Once created, it is added to this receiver [Container]. */
inline fun Container.solidRect(size: Size, color: RGBA = Colors.WHITE, callback: @ViewDslMarker SolidRect.() -> Unit = {})
    = SolidRect(size, color).addTo(this, callback)

inline fun Container.solidRect(width: Double, height: Double, color: RGBA = Colors.WHITE, callback: @ViewDslMarker SolidRect.() -> Unit = {})
    = SolidRect(Size(width, height), color).addTo(this, callback)

/** Creates a new [SolidRect] of size [width]x[height] and color [color] and allows you to configure it via [callback]. Once created, it is added to this receiver [Container]. */
inline fun Container.solidRect(width: Int, height: Int, color: RGBA = Colors.WHITE, callback: @ViewDslMarker SolidRect.() -> Unit = {})
    = SolidRect(Size(width, height), color).addTo(this, callback)

/**
 * A Rect [RectBase] [View] of size [widthD] and [heightD] with the initial color, [color].
 */
class SolidRect(size: Size, color: RGBA = Colors.WHITE) : RectBase() {
	companion object {
        operator fun invoke(width: Int, height: Int, color: RGBA = Colors.WHITE) = SolidRect(Size(width, height), color)
        operator fun invoke(width: Float, height: Float, color: RGBA = Colors.WHITE) = SolidRect(Size(width, height), color)
        operator fun invoke(width: Double, height: Double, color: RGBA = Colors.WHITE) = SolidRect(Size(width, height), color)
	}

    override var unscaledSize: Size = size
        set(value) {
            if (field == value) return
            field = value
            dirtyVertices = true
            invalidateRender()
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

	override fun createInstance(): View = SolidRect(Size(width, height), colorMul)
}
