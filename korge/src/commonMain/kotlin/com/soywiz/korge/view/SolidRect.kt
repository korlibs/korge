package com.soywiz.korge.view

import com.soywiz.korim.color.*

/** Creates a new [SolidRect] of size [width]x[height] and color [color] and allows you to configure it via [callback]. Once created, it is added to this receiver [Container]. */
inline fun Container.solidRect(width: Double, height: Double, color: RGBA = Colors.WHITE, callback: @ViewDslMarker SolidRect.() -> Unit = {})
    = SolidRect(width, height, color).addTo(this, callback)

/** Creates a new [SolidRect] of size [width]x[height] and color [color] and allows you to configure it via [callback]. Once created, it is added to this receiver [Container]. */
inline fun Container.solidRect(width: Int, height: Int, color: RGBA = Colors.WHITE, callback: @ViewDslMarker SolidRect.() -> Unit = {})
    = SolidRect(width.toDouble(), height.toDouble(), color).addTo(this, callback)

/**
 * A Rect [RectBase] [View] of size [width] and [height] with the initial color, [color].
 */
class SolidRect(width: Double, height: Double, color: RGBA) : RectBase() {
	companion object {
        operator fun invoke(width: Int, height: Int, color: RGBA) = SolidRect(width.toDouble(), height.toDouble(), color)

        @Deprecated("Kotlin/Native boxes inline+Number")
        inline operator fun invoke(width: Number, height: Number, color: RGBA) = SolidRect(width.toDouble(), height.toDouble(), color)
	}

	override var width: Double = width; set(v) { field = v; dirtyVertices = true }
	override var height: Double = height; set(v) { field = v; dirtyVertices = true }

    override val bwidth: Double get() = width
    override val bheight: Double get() = height

    /** The [color] of this [SolidRect]. Alias of [colorMul]. */
    var color: RGBA
        set(value) { colorMul = value }
        get() = colorMul

	init {
		this.colorMul = color
	}

	override fun createInstance(): View = SolidRect(width, height, colorMul)
}
