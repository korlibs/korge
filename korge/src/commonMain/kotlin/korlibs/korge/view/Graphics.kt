package korlibs.korge.view

import korlibs.korge.internal.*
import korlibs.korge.view.property.*
import korlibs.korge.view.vector.*
import korlibs.image.vector.*

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
            softGraphics?.antialiased = value
            gpuGraphics?.antialiased = value
            invalidateRender()
        }

    @ViewProperty
    var debugDrawOnlyAntialiasedBorder: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            gpuGraphics?.debugDrawOnlyAntialiasedBorder = value
            invalidateRender()
        }

    @ViewProperty
    var smoothing: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            softGraphics?.smoothing = value
            gpuGraphics?.smoothing = value
            invalidateRender()
        }
    @ViewProperty
    var autoScaling: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            softGraphics?.autoScaling = value
            gpuGraphics?.autoScaling = value
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
                    gpuGraphics = gpuGraphics().also {
                        it.antialiased = antialiased
                        it.autoScaling = autoScaling
                        it.smoothing = smoothing
                        it.shape = shape
                    }
                }
            }
            else -> {
                if (gpuGraphics != null) {
                    gpuGraphics?.removeFromParent()
                    gpuGraphics = null
                }
                if (softGraphics == null) {
                    softGraphics = cpuGraphics().also {
                        it.antialiased = antialiased
                        it.autoScaling = autoScaling
                        it.smoothing = smoothing
                        it.shape = shape
                    }
                }
                softGraphics?.useNativeRendering = (renderer == GraphicsRenderer.SYSTEM)
            }
        }
    }

    init {
        this.shape = shape
    }
}