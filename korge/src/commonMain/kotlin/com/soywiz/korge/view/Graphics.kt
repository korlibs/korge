package com.soywiz.korge.view

import com.soywiz.korge.internal.*
import com.soywiz.korge.view.property.*
import com.soywiz.korge.view.vector.*
import com.soywiz.korim.vector.*

inline fun Container.graphics(
    renderer: GraphicsRenderer = GraphicsRenderer.SYSTEM,
    callback: ShapeBuilder.(Graphics) -> Unit = {}
): Graphics = Graphics(EmptyShape, renderer).addTo(this).also { graphics ->
    graphics.updateShape { callback(this, graphics) }
    graphics.redrawIfRequired()
}

inline fun Container.graphics(
    build: ShapeBuilder.() -> Unit,
    renderer: GraphicsRenderer = GraphicsRenderer.SYSTEM,
    callback: @ViewDslMarker Graphics.() -> Unit = {}
) = Graphics(buildShape { build() }, renderer).addTo(this, callback)

inline fun Container.graphics(
    shape: Shape,
    renderer: GraphicsRenderer = GraphicsRenderer.SYSTEM,
    callback: @ViewDslMarker Graphics.() -> Unit = {}
) = Graphics(shape, renderer).addTo(this, callback)

enum class GraphicsRenderer {
    /** Uses software system renderer (fast) */
    SYSTEM,
    /** Uses software kotlin renderer (slow) */
    CPU,
    /** Uses GPU renderer */
    GPU
}

class Graphics(
    shape: Shape = EmptyShape,
    renderer: GraphicsRenderer = GraphicsRenderer.GPU
) : Container(), ViewLeaf, Anchorable {
    private var softGraphics: CpuGraphics? = null
    private var gpuGraphics: GpuShapeView? = null

    private val anchorable: Anchorable get() = (softGraphics ?: gpuGraphics)!!
    val rendererView: View get() = (softGraphics ?: gpuGraphics)!!

    @ViewProperty
    var boundsIncludeStrokes: Boolean
        get() = softGraphics?.boundsIncludeStrokes ?: gpuGraphics?.boundsIncludeStrokes ?: false
        set(value) {
            softGraphics?.boundsIncludeStrokes = value
            gpuGraphics?.boundsIncludeStrokes = value
            invalidateRender()
        }

    override var anchorX: Double
        get() = anchorable.anchorX
        set(value) {
            anchorable.anchorX = value
            invalidateRender()
        }
    override var anchorY: Double
        get() = anchorable.anchorY
        set(value) {
            anchorable.anchorY = value
            invalidateRender()
        }
    @KorgeInternal override val anchorDispX: Double get() = rendererView.anchorDispX
    @KorgeInternal override val anchorDispY: Double get() = rendererView.anchorDispY

    @ViewProperty
    var antialiased: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            softGraphics?.antialiased = true
            gpuGraphics?.antialiased = true
            invalidateRender()
        }

    @ViewProperty
    var debugDrawOnlyAntialiasedBorder: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            //softGraphics?.debugDrawOnlyAntialiasedBorder = true
            gpuGraphics?.debugDrawOnlyAntialiasedBorder = true
            invalidateRender()
        }

    @ViewProperty
    var smoothing: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            softGraphics?.smoothing = true
            gpuGraphics?.smoothing = true
            invalidateRender()
        }
    @ViewProperty
    var autoScaling: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            softGraphics?.autoScaling = true
            gpuGraphics?.autoScaling = true
            invalidateRender()
        }

    var shape: Shape = EmptyShape
        set(value) {
            if (field === value) return
            field = value
            ensure()
            softGraphics?.shape = value
            gpuGraphics?.shape = value
            invalidateRender()
        }

    @ViewProperty
    var renderer: GraphicsRenderer = renderer
        set(value) {
            if (field === value) return
            field = value
            redrawIfRequired()
        }

    inline fun <T> updateShape(block: ShapeBuilder.(Graphics) -> T): T {
        var result: T
        this.shape = buildShape { result = block(this@Graphics) }
        return result
    }

    fun redrawIfRequired() {
        ensure()
        softGraphics?.dirty()
        softGraphics?.redrawIfRequired()
    }

    private fun ensure() {
        when (renderer) {
            GraphicsRenderer.GPU -> {
                if (softGraphics != null) {
                    softGraphics?.removeFromParent()
                    softGraphics = null
                }
                if (gpuGraphics == null) {
                    gpuGraphics = gpuGraphics()
                    gpuGraphics?.antialiased = antialiased
                    gpuGraphics?.autoScaling = autoScaling
                    gpuGraphics?.smoothing = smoothing
                    gpuGraphics?.shape = shape
                }
            }
            else -> {
                if (gpuGraphics != null) {
                    gpuGraphics?.removeFromParent()
                    gpuGraphics = null
                }
                if (softGraphics == null) {
                    softGraphics = cpuGraphics()
                    softGraphics?.antialiased = antialiased
                    softGraphics?.autoScaling = autoScaling
                    softGraphics?.smoothing = smoothing
                    softGraphics?.shape = shape
                }
                softGraphics?.useNativeRendering = (renderer == GraphicsRenderer.SYSTEM)
            }
        }
    }

    init {
        this.shape = shape
    }
}
