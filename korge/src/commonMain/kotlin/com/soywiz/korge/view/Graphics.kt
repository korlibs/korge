package com.soywiz.korge.view

import com.soywiz.korge.view.vector.GpuShapeView
import com.soywiz.korge.view.vector.gpuGraphics
import com.soywiz.korim.vector.EmptyShape
import com.soywiz.korim.vector.Shape
import com.soywiz.korim.vector.ShapeBuilder
import com.soywiz.korim.vector.buildShape

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

class Graphics(shape: Shape = EmptyShape, renderer: GraphicsRenderer = GraphicsRenderer.GPU) : Container(), ViewLeaf {
    private var softGraphics: CpuGraphics? = null
    private var gpuGraphics: GpuShapeView? = null

    var antialiased: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            softGraphics?.antialiased = true
            gpuGraphics?.antialiased = true
        }
    var smoothing: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            softGraphics?.smoothing = true
            gpuGraphics?.smoothing = true
        }
    var autoScaling: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            softGraphics?.autoScaling = true
            gpuGraphics?.autoScaling = true
        }

    var shape: Shape = EmptyShape
        set(value) {
            if (field === value) return
            field = value
            ensure()
            softGraphics?.shape = value
            gpuGraphics?.shape = value
        }
    var renderer: GraphicsRenderer = renderer
        set(value) {
            if (field === value) return
            field = value
            ensure()
        }

    inline fun <T> updateShape(block: ShapeBuilder.(Graphics) -> T): T {
        var result: T
        this.shape = buildShape { result = block(this@Graphics) }
        return result
    }

    fun redrawIfRequired() {
        ensure()
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
                }
                softGraphics?.useNativeRendering = (renderer == GraphicsRenderer.SYSTEM)
            }
        }
    }

    init {
        this.shape = shape
    }
}
