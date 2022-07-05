package com.soywiz.korge.view

import com.soywiz.korge.view.vector.GpuShapeView
import com.soywiz.korge.view.vector.gpuGraphics
import com.soywiz.korim.vector.EmptyShape
import com.soywiz.korim.vector.Shape
import com.soywiz.korim.vector.ShapeBuilder
import com.soywiz.korim.vector.buildShape

inline fun Container.newGraphics(
    renderer: GraphicsRenderer = GraphicsRenderer.GPU,
    callback: ShapeBuilder.(NewGraphics) -> Unit = {}
): NewGraphics = NewGraphics(EmptyShape, renderer).addTo(this).also { graphics ->
    graphics.updateShape { callback(this, graphics) }
    graphics.redrawIfRequired()
}

inline fun Container.newGraphics(
    build: ShapeBuilder.() -> Unit,
    renderer: GraphicsRenderer = GraphicsRenderer.GPU,
    callback: @ViewDslMarker NewGraphics.() -> Unit = {}
) = NewGraphics(buildShape { build() }, renderer).addTo(this, callback)

inline fun Container.newGraphics(
    shape: Shape,
    renderer: GraphicsRenderer = GraphicsRenderer.GPU,
    callback: @ViewDslMarker NewGraphics.() -> Unit = {}
) = NewGraphics(shape, renderer).addTo(this, callback)

enum class GraphicsRenderer {
    /** Uses software system renderer (fast) */
    SYSTEM,
    /** Uses software kotlin renderer (slow) */
    CPU,
    /** Uses GPU renderer */
    GPU
}

class NewGraphics(shape: Shape = EmptyShape, renderer: GraphicsRenderer = GraphicsRenderer.GPU) : Container(), ViewLeaf {
    private var softGraphics: Graphics? = null
    private var gpuGraphics: GpuShapeView? = null

    var antialiased: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            softGraphics?.antialiased = true
            gpuGraphics?.antialiased = true
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

    inline fun updateShape(block: ShapeBuilder.(NewGraphics) -> Unit) {
        this.shape = buildShape { block(this@NewGraphics) }
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
                }
            }
            else -> {
                if (gpuGraphics != null) {
                    gpuGraphics?.removeFromParent()
                    gpuGraphics = null
                }
                if (softGraphics == null) {
                    softGraphics = graphics()
                    softGraphics?.antialiased = antialiased
                    softGraphics?.autoScaling = autoScaling
                }
                softGraphics?.useNativeRendering = (renderer == GraphicsRenderer.SYSTEM)
            }
        }
    }

    init {
        this.shape = shape
    }
}
