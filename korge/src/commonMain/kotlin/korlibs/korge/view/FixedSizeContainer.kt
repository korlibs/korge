package korlibs.korge.view

import korlibs.datastructure.*
import korlibs.io.util.*
import korlibs.korge.internal.*
import korlibs.korge.render.*
import korlibs.korge.view.filter.*
import korlibs.korge.view.property.*
import korlibs.math.geom.*

inline fun Container.fixedSizeContainer(
    width: Float,
    height: Float,
    clip: Boolean = false,
    callback: @ViewDslMarker FixedSizeContainer.() -> Unit = {}
) = FixedSizeContainer(width, height, clip).addTo(this, callback)

inline fun Container.fixedSizeContainer(
    width: Double,
    height: Double,
    clip: Boolean = false,
    callback: @ViewDslMarker FixedSizeContainer.() -> Unit = {}
) = FixedSizeContainer(width.toFloat(), height.toFloat(), clip).addTo(this, callback)

inline fun Container.fixedSizeContainer(
    width: Int,
    height: Int,
    clip: Boolean = false,
    callback: @ViewDslMarker FixedSizeContainer.() -> Unit = {}
) = FixedSizeContainer(width.toFloat(), height.toFloat(), clip).addTo(this, callback)

@korlibs.math.annotations.ViewDslMarker
open class SContainer(
    width: Double = 100.0,
    height: Double = 100.0,
    clip: Boolean = false,
) : FixedSizeContainer(width.toFloat(), height.toFloat(), clip)

open class FixedSizeContainer(
    override var width: Float = 100f,
    override var height: Float = 100f,
    @property:ViewProperty
    open var clip: Boolean = false,
) : Container(), View.Reference {

    override fun getLocalBoundsInternal() = Rectangle(0f, 0f, width, height)

    override fun toString(): String {
        var out = super.toString()
        out += ":size=(${width.niceStr}x${height.niceStr})"
        return out
    }

    private var renderingInternalRef = Ref(false)

    private val tempRect = MRectangle()

    @OptIn(KorgeInternal::class)
    override fun renderInternal(ctx: RenderContext) {
        renderClipped(this, ctx, clip, renderingInternalRef) { super.renderInternal(ctx) }
    }

    companion object {
        inline fun renderClipped(view: View, ctx: RenderContext, clip: Boolean, renderingInternalRef: Ref<Boolean>, superRenderInternal: (ctx: RenderContext) -> Unit) {
            if (renderingInternalRef.value) {
                return superRenderInternal(ctx)
            }
            if (clip) {
                val m = view.globalMatrix
                val hasRotation = m.b != 0f || m.c != 0f
                //val hasNegativeScale = m.a < 0.0 || m.d < 0.0
                //if (hasRotation || hasNegativeScale) {
                if (hasRotation) {
                    //if (true) {
                    // Use a framebuffer instead
                    val old = renderingInternalRef.value
                    try {
                        renderingInternalRef.value = true
                        view.renderFiltered(ctx, IdentityFilter)
                    } finally {
                        renderingInternalRef.value = old
                    }
                    return
                }
                ctx.useCtx2d { c2d ->
                    // @TODO: Maybe scissor should be global and do the global to window / texture conversions in the very last moment,
                    // @TODO: so we don't propagate that complexity here
                    // If width or height are negative, because scale was negative
                    val bounds = view.getClippingBounds(ctx).transformed(ctx.viewMat2D).normalized()

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
                            superRenderInternal(ctx)
                        }
                    } else {
                        superRenderInternal(ctx)
                    }
                }
            } else {
                superRenderInternal(ctx)
            }
        }
    }
}

fun View.getVisibleLocalArea(): Rectangle {
    val global = getVisibleGlobalArea()
    return BoundsBuilder(
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
