package com.soywiz.korge.view

import com.soywiz.kds.identityHashCode
import com.soywiz.korge.internal.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.math.*

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
    private var renderingInternal = false

    @OptIn(KorgeInternal::class)
    override fun renderInternal(ctx: RenderContext) {
        if (renderingInternal) {
            return super.renderInternal(ctx)
        }
        if (clip) {
            val m = globalMatrix
            val hasRotation = m.b != 0.0 || m.c != 0.0
            //val hasNegativeScale = m.a < 0.0 || m.d < 0.0
            //if (hasRotation || hasNegativeScale) {
            if (hasRotation) {
                // Use a renderbuffer instead
                val old = renderingInternal
                try {
                    renderingInternal = true
                    renderFiltered(ctx, IdentityFilter)
                } finally {
                    renderingInternal = old
                }
                return
            }
            ctx.useCtx2d { c2d ->
                val windowBounds = getWindowBounds(ctx, tempBounds)

                //println("BOUNDS: globalToWindowMatrix=${ctx.globalToWindowMatrix} : ${ctx.identityHashCode()}, ${ctx.globalToWindowMatrix.identityHashCode()}")
                //println("BOUNDS: windowBounds=$windowBounds, globalBounds=${getGlobalBounds()}")

                @Suppress("DEPRECATION")
                windowBounds.applyTransform(ctx.batch.viewMat2D) // @TODO: Should viewMat2D be in the context instead?
                windowBounds.normalize() // If width or height are negative, because scale was negative

                //println("FIXED_CLIP: bounds=$bounds")
                val rect = c2d.batch.scissor?.rect
                var intersects = true
                if (rect != null) {
                    intersects = windowBounds.setToIntersection(windowBounds, rect) != null
                }
                if (intersects) {
                    c2d.scissor(windowBounds) {
                        super.renderInternal(ctx)
                    }
                } else {
                    super.renderInternal(ctx)
                }
            }
        } else {
            super.renderInternal(ctx)
        }
    }
}

fun View.getVisibleLocalArea(out: Rectangle = Rectangle()): Rectangle {
    getVisibleGlobalArea(out)
    val x0 = globalToLocalX(out.left, out.top)
    val x1 = globalToLocalX(out.right, out.top)
    val x2 = globalToLocalX(out.right, out.bottom)
    val x3 = globalToLocalX(out.left, out.bottom)
    val y0 = globalToLocalY(out.left, out.top)
    val y1 = globalToLocalY(out.right, out.top)
    val y2 = globalToLocalY(out.right, out.bottom)
    val y3 = globalToLocalY(out.left, out.bottom)
    val xmin = min(x0, x1, x2, x3)
    val xmax = max(x0, x1, x2, x3)
    val ymin = min(y0, y1, y2, y3)
    val ymax = max(y0, y1, y2, y3)
    return out.setBounds(xmin, ymin, xmax, ymax)
}

fun View.getNextClippingView(): View {
    forEachAscendant(includeThis = true) {
        if ((it is FixedSizeContainer && it.clip) || it is Stage) return@getNextClippingView it
    }
    return this
}

fun View.getVisibleGlobalArea(out: Rectangle = Rectangle()): Rectangle {
    forEachAscendant(includeThis = true) {
        if ((it is FixedSizeContainer && it.clip) || it is Stage) return@getVisibleGlobalArea it.getGlobalBounds(out)
    }
    return out.setTo(0.0, 0.0, 4096.0, 4096.0)
}

fun View.getVisibleWindowArea(out: Rectangle = Rectangle()): Rectangle {
    forEachAscendant(includeThis = true) {
        if ((it is FixedSizeContainer && it.clip) || it is Stage) return@getVisibleWindowArea it.getWindowBounds(out)
    }
    return out.setTo(0.0, 0.0, 4096.0, 4096.0)
}
