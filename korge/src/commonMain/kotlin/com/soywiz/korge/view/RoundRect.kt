package com.soywiz.korge.view

import com.soywiz.korge.ui.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.vector.*

/** Creates a new [RoundRect] of size [width]x[height] and [color]
 *  and allows you to configure it via [callback].
 *  Once created, it is added to this receiver [Container].
 **/
inline fun Container.roundRect(
    width: Int,
    height: Int,
    rx: Int,
    ry: Int = rx,
    color: RGBA = Colors.WHITE,
    autoScaling: Boolean = true,
    callback: @ViewDslMarker RoundRect.() -> Unit = {}
) = roundRect(width.toDouble(), height.toDouble(), rx.toDouble(), ry.toDouble(), color, autoScaling, callback)

inline fun Container.roundRect(
    width: Double,
    height: Double,
    rx: Double,
    ry: Double = rx,
    color: RGBA = Colors.WHITE,
    autoScaling: Boolean = true,
    callback: @ViewDslMarker RoundRect.() -> Unit = {}
) = RoundRect(width, height, rx, ry, color, autoScaling).addTo(this, callback)

/**
 * A Rect [View] with rounded corners of size [width] and [height] with the initial [color].
 */
class RoundRect(
    width: Double,
    height: Double,
    rx: Double,
    ry: Double = rx,
    color: RGBA = Colors.WHITE,
    autoScaling: Boolean = true
) : Graphics(autoScaling) {

    override var width: Double by uiObservable(width) { updateGraphics() }
    override var height: Double by uiObservable(height) { updateGraphics() }

    override val bwidth: Double get() = width
    override val bheight: Double get() = height

    var rx: Double by uiObservable(rx) { updateGraphics() }
    var ry: Double by uiObservable(ry) { updateGraphics() }

    /** The [color] of this [RoundRect]. Alias of [colorMul]. */
    var color: RGBA
        get() = colorMul
        set(value) { colorMul = value }

    init {
        this.colorMul = color
        updateGraphics()
    }

    private fun updateGraphics() {
        clear()
        fill(Colors.WHITE) {
            roundRect(0.0, 0.0, this@RoundRect.width, this@RoundRect.height, this@RoundRect.rx, this@RoundRect.ry)
        }
    }
}
