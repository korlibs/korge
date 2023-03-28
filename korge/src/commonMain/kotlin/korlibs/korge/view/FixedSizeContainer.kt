package korlibs.korge.view

import korlibs.korge.internal.*
import korlibs.korge.render.*
import korlibs.korge.view.filter.*
import korlibs.korge.view.property.*
import korlibs.io.util.*
import korlibs.math.geom.*

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

@korlibs.math.annotations.ViewDslMarker
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

    override fun getLocalBoundsInternal() = Rectangle(0.0, 0.0, width, height)

    override fun toString(): String {
        var out = super.toString()
        out += ":size=(${width.niceStr}x${height.niceStr})"
        return out
    }

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
                // If width or height are negative, because scale was negative
                val bounds = getClippingBounds(ctx).transformed(ctx.viewMat2D).normalized()

                //println("ctx.ag.isRenderingToWindow=${ctx.ag.isRenderingToWindow}, FIXED_CLIP: bounds=$bounds, ctx.viewMat2D=${ctx.viewMat2D}")

                //println("FIXED_CLIP: bounds=$bounds")
                val rect = c2d.batch.scissor.toRectOrNull()
                var intersects = true
                if (rect != null) {
                    intersects = bounds.intersects(rect)
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

fun View.getVisibleLocalArea(): Rectangle {
    val global = getVisibleGlobalArea()
    return NewBoundsBuilder(
        globalToLocal(global.topLeft),
        globalToLocal(global.topRight),
        globalToLocal(global.bottomRight),
        globalToLocal(global.bottomLeft),
    ).bounds
}

fun View.getNextClippingView(): View {
    forEachAscendant(includeThis = true) {
        if ((it is FixedSizeContainer && it.clip) || it is Stage) return@getNextClippingView it
    }
    return this
}

fun View.getVisibleGlobalArea(): Rectangle {
    forEachAscendant(includeThis = true) {
        if ((it is FixedSizeContainer && it.clip) || it is Stage) return@getVisibleGlobalArea it.globalBounds
    }
    return Rectangle(0.0, 0.0, 4096.0, 4096.0)
}

fun View.getVisibleWindowArea(): Rectangle {
    forEachAscendant(includeThis = true) {
        if ((it is FixedSizeContainer && it.clip) || it is Stage) return@getVisibleWindowArea it.windowBounds
    }
    return Rectangle(0.0, 0.0, 4096.0, 4096.0)
}
