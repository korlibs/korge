package com.soywiz.korge.view

import com.soywiz.korge.internal.*
import com.soywiz.korge.render.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*

inline fun Container.fixedSizeContainer(
    width: Double,
    height: Double,
    clip: Boolean = false,
    callback: @ViewDslMarker FixedSizeContainer.() -> Unit = {}
) = FixedSizeContainer(width, height, clip).addTo(this, callback)

inline fun Container.fixedSizeContainer(
    width: Int,
    height: Int,
    clip: Boolean = false,
    callback: @ViewDslMarker FixedSizeContainer.() -> Unit = {}
) = FixedSizeContainer(width.toDouble(), height.toDouble(), clip).addTo(this, callback)

open class FixedSizeContainer(
    override var width: Double = 100.0,
    override var height: Double = 100.0,
    var clip: Boolean = false
) : Container(), View.Reference {

    override fun getLocalBoundsInternal(out: Rectangle) {
        out.setTo(0, 0, width, height)
    }

    override fun toString(): String {
        var out = super.toString()
        out += ":size=(${width.niceStr}x${height.niceStr})"
        return out
    }

    private val tempBounds = Rectangle()

    @OptIn(KorgeInternal::class)
    override fun renderInternal(ctx: RenderContext) {
        if (clip) {
            val c2d = ctx.ctx2d
            //val bounds = stage?.views?.getWindowBounds(this, tempBounds) ?: getGlobalBounds(tempBounds)
            val bounds = getGlobalBounds(tempBounds)
            c2d.scissor(bounds) {
                super.renderInternal(ctx)
            }
        } else {
            super.renderInternal(ctx)
        }
    }
}
