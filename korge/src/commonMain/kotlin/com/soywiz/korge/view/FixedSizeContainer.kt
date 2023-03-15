package com.soywiz.korge.view

import com.soywiz.korge.internal.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korge.view.property.*
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

@com.soywiz.korma.annotations.ViewDslMarker
open class SContainer(
    width: Double = 100.0,
    height: Double = 100.0,
    clip: Boolean = false,
) : FixedSizeContainer(width, height, clip)

open class FixedSizeContainer(
    override var width: Double = 100.0,
    override var height: Double = 100.0,
    @property:ViewProperty
    open var clip: Boolean = false,
) : Container(), View.Reference {

    override fun getLocalBoundsInternal(out: MRectangle) {
        out.setTo(0.0, 0.0, width, height)
    }

    override fun toString(): String {
        var out = super.toString()
        out += ":size=(${width.niceStr}x${height.niceStr})"
        return out
    }

    private val tempBounds = MRectangle()
    private var renderingInternal = false

    private val tempRect = MRectangle()

    @OptIn(KorgeInternal::class)
    override fun renderInternal(ctx: RenderContext) {
        if (renderingInternal) {
            return super.renderInternal(ctx)
        }
        if (clip) {
            val m = globalMatrix
            val hasRotation = m.b != 0f || m.c != 0f
            //val hasNegativeScale = m.a < 0.0 || m.d < 0.0
            //if (hasRotation || hasNegativeScale) {
            if (hasRotation) {
            //if (true) {
                // Use a framebuffer instead
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
                // @TODO: Maybe scissor should be global and do the global to window / texture conversions in the very last moment,
                // @TODO: so we don't propagate that complexity here
                val bounds = getClippingBounds(ctx, tempBounds)
                //val bounds = getWindowBounds(ctx, tempBounds)
                //val bounds = getGlobalBounds(tempBounds)

                //println("BOUNDS: globalToWindowMatrix=${ctx.globalToWindowMatrix} : ${ctx.identityHashCode()}, ${ctx.globalToWindowMatrix.identityHashCode()}")
                //println("BOUNDS: windowBounds=$windowBounds, globalBounds=${getGlobalBounds()}")
                //println("BOUNDS1: $windowBounds, ${ctx.viewMat2D}")
                //println("bounds=$bounds, bp.globalToWindowMatrix=${ctx.globalToWindowMatrix}")

                @Suppress("DEPRECATION")
                bounds.applyTransform(ctx.viewMat2D)
                bounds.normalize() // If width or height are negative, because scale was negative

                //println("ctx.ag.isRenderingToWindow=${ctx.ag.isRenderingToWindow}, FIXED_CLIP: bounds=$bounds, ctx.viewMat2D=${ctx.viewMat2D}")

                //println("FIXED_CLIP: bounds=$bounds")
                val rect = c2d.batch.scissor.toRectOrNull(tempRect)
                var intersects = true
                if (rect != null) {
                    intersects = bounds.setToIntersection(bounds, rect) != null
                }
                //println("BOUNDS2: $windowBounds, ${ctx.viewMat2D}")
                if (intersects) {
                    c2d.scissor(bounds) {
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

fun View.getVisibleLocalArea(out: MRectangle = MRectangle()): MRectangle {
    getVisibleGlobalArea(out)
    val p0 = globalToLocal(out.topLeft)
    val p1 = globalToLocal(out.topRight)
    val p2 = globalToLocal(out.bottomRight)
    val p3 = globalToLocal(out.bottomLeft)
    val xmin = min(p0.x, p1.x, p2.x, p3.x)
    val xmax = max(p0.x, p1.x, p2.x, p3.x)
    val ymin = min(p0.y, p1.y, p2.y, p3.y)
    val ymax = max(p0.y, p1.y, p2.y, p3.y)
    return out.setBounds(xmin, ymin, xmax, ymax)
}

fun View.getNextClippingView(): View {
    forEachAscendant(includeThis = true) {
        if ((it is FixedSizeContainer && it.clip) || it is Stage) return@getNextClippingView it
    }
    return this
}

fun View.getVisibleGlobalArea(out: MRectangle = MRectangle()): MRectangle {
    forEachAscendant(includeThis = true) {
        if ((it is FixedSizeContainer && it.clip) || it is Stage) return@getVisibleGlobalArea it.getGlobalBounds(out)
    }
    return out.setTo(0.0, 0.0, 4096.0, 4096.0)
}

fun View.getVisibleWindowArea(out: MRectangle = MRectangle()): MRectangle {
    forEachAscendant(includeThis = true) {
        if ((it is FixedSizeContainer && it.clip) || it is Stage) return@getVisibleWindowArea it.getWindowBounds(out)
    }
    return out.setTo(0.0, 0.0, 4096.0, 4096.0)
}
