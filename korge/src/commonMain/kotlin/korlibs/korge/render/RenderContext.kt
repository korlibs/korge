package korlibs.korge.render

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.memory.unit.*
import korlibs.graphics.*
import korlibs.graphics.annotation.*
import korlibs.graphics.log.*
import korlibs.graphics.shader.*
import korlibs.korge.internal.*
import korlibs.korge.stat.*
import korlibs.korge.view.*
import korlibs.render.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.io.async.*
import korlibs.io.lang.*
import korlibs.math.geom.*
import kotlin.coroutines.*

/**
 * A context that allows to render objects.
 *
 * The [RenderContext] contains the [ag] [AG] (Accelerated Graphics),
 * that allow to render triangles and other primitives to the current render buffer.
 *
 * When doing 2D, you should usually use the [batch] to buffer vertices,
 * so they can be rendered at once when flushing.
 *
 * If you plan to do a custom drawing using [ag] directly, you should call [flush],
 * so all the pending vertices are rendered.
 *
 * If you want to perform drawing using a context allowing non-precomputed transformations
 * you can use [ctx2d].
 *
 * If you need to get textures from [Bitmap] that are allocated and deallocated as required
 * preventing leaks, you should use [getTex].
 */
class RenderContext constructor(
    /** The Accelerated Graphics object that allows direct rendering */
	val ag: AG,
	val bp: BoundsProvider = BoundsProvider.Base(),
    val deviceDimensionsProvider: DeviceDimensionsProvider = (bp as? DeviceDimensionsProvider?) ?: DeviceDimensionsProvider.DEFAULT,
    /** Object storing all the rendering [Stats] like number of batches, number of vertices etc. */
	val stats: Stats = Stats(),
	val coroutineContext: CoroutineContext = EmptyCoroutineContext,
    val batchMaxQuads: Int = BatchBuilder2D.DEFAULT_BATCH_QUADS
) :
    Extra by Extra.Mixin(),
    BoundsProvider by bp,
    AGFeatures by ag,
    DeviceDimensionsProvider by deviceDimensionsProvider,
    Closeable
{
    @PublishedApi internal val _buffers = AGProgramWithUniforms.BufferCache()
    private val _programs = FastIdentityMap<Program, AGProgramWithUniforms>()

    //@PublishedApi internal val textureUnitsPool = Pool { AGTextureUnits() }
    val textureUnits = AGTextureUnits()

    var projectionMatrixTransform = Matrix()
    var projectionMatrixTransformInv = Matrix()
    private var projMat: Matrix4 = Matrix4()
    @KorgeInternal
    var viewMat: Matrix4 = Matrix4()
    @KorgeInternal
    var viewMat2D: Matrix = Matrix()

    var flipRenderTexture = true
    //var flipRenderTexture = false

    val tempTexturePool: Pool<AGTexture> = Pool { AGTexture() }

    fun afterFullBatch() {
        //projMatrixBuffer.reset()
    }

    //val projViewMat by lazy { this[DefaultShaders.ub_ProjViewMatBlock] }
    val projViewMatUB by lazy { this[DefaultShaders.ProjViewUB] }

    fun updateStandardUniforms() {
        //println("updateStandardUniforms: ag.currentSize(${ag.currentWidth}, ${ag.currentHeight}) : ${ag.currentFrameBuffer}")
        if (flipRenderTexture && currentFrameBuffer.isTexture) {
            projMat = Matrix4.ortho(Rectangle.fromBounds(0, currentFrameBuffer.height, currentFrameBuffer.width, 0), -1f, 1f)
        } else {
            projMat = Matrix4.ortho(Rectangle.fromBounds(0, 0, currentFrameBuffer.width, currentFrameBuffer.height), -1f, 1f)
            projMat = projMat * projectionMatrixTransform.toMatrix4()
        }

        //println("updateStandardUniforms!!!")

        // @TODO: Detect if there was a change
        //projViewMat.push(deduplicate = true) {
        //    it[DefaultShaders.u_ProjMat].set(projMat)
        //    it[DefaultShaders.u_ViewMat].set(viewMat)
        //}
        projViewMatUB.push(deduplicate = true) {
            it[u_ProjMat] = projMat
            it[u_ViewMat] = viewMat
        }

        //uniforms[DefaultShaders.u_ProjMat] = projMat
        //uniforms[DefaultShaders.u_ViewMat] = viewMat
    }

    /**
     * Executes [callback] while setting temporarily the view matrix to [matrix]
     */
    inline fun setViewMatrixTemp(matrix: Matrix, crossinline callback: () -> Unit) {
        flush()
        val temp4 = this.viewMat
        val temp2d = this.viewMat2D
        this.viewMat2D = matrix
        this.viewMat = matrix.toMatrix4()
        //this[DefaultShaders.ub_ProjViewMatBlock].push {
        //    it[DefaultShaders.u_ViewMat].set(this.viewMat)
        //}
        this[DefaultShaders.ProjViewUB].push {
            it[u_ViewMat] = this@RenderContext.viewMat
        }
        //println("viewMat: $viewMat, matrix: $matrix")
        try {
            callback()
        } finally {
            flush()
            this.viewMat = temp4
            this.viewMat2D = temp2d
            //this[DefaultShaders.ub_ProjViewMatBlock].pop()
            this[DefaultShaders.ProjViewUB].pop()
            //uniforms[DefaultShaders.u_ViewMat] = this.viewMat
        }
    }

    //inline fun <T> keepTextureUnit(sampler: Sampler, flush: Boolean = true, callback: () -> T): T {
    //    val oldTex = textureUnits.textures[sampler.index]
    //    val oldInfo = textureUnits.infos[sampler.index]
    //    try {
    //        return callback()
    //    } finally {
    //        if (flush) flush()
    //        textureUnits.set(sampler, oldTex, oldInfo)
    //    }
    //}
    //inline fun <T> keepTextureUnits(samplers: Array<Sampler>, flush: Boolean = true, callback: () -> T): T {
    //    textureUnitsPool.alloc { old ->
    //        samplers.fastForEach { old.set(it, textureUnits.textures[it.index], textureUnits.infos[it.index]) }
    //        try {
    //            return callback()
    //        } finally {
    //            if (flush) flush()
    //            samplers.fastForEach { textureUnits.set(it, old.textures[it.index], old.infos[it.index]) }
    //        }
    //    }
    //}

    val agAutoFreeManager = AgAutoFreeManager()
	val agBitmapTextureManager = AgBitmapTextureManager(ag)
    val agBufferManager = AgBufferManager(ag)

    /** Allows to register handlers when the [flush] method is called */
    val flushers = Signal<FlushKind>()

    enum class FlushKind { STATE, FULL }

    val views: Views? = bp as? Views?

    var debugAnnotateView: View? = null
        set(value) {
            views?.invalidatedView(field)
            field = value
            views?.invalidatedView(field)
        }
    var debugExtraFontScale : Double = 1.0
    var debugExtraFontColor : RGBA = Colors.WHITE

    val debugOverlayScale: Double get() = kotlin.math.round(computedPixelRatio * debugExtraFontScale).coerceAtLeast(1.0)

    /** Allows to draw quads, sprites and nine patches using a precomputed global matrix or raw vertices */
    @Deprecated("Use useBatcher instead")
    @KorgeInternal
    val batch = BatchBuilder2D(this, batchMaxQuads)

    val dynamicVertexBufferPool = Pool { AGBuffer() }

    @OptIn(KorgeInternal::class)
    inline fun useBatcher(block: (BatchBuilder2D) -> Unit) = batch.use(block)

    /** [RenderContext2D] similar to the one from JS, that keeps a matrix (affine transformation) and allows to draw shapes using the current matrix */
    @KorgeInternal
    @Deprecated("Use useCtx2d instead")
    val ctx2d = RenderContext2D(batch, agBitmapTextureManager)

    @Suppress("DEPRECATION")
    @OptIn(KorgeInternal::class)
    inline fun useCtx2d(block: (RenderContext2D) -> Unit) { useBatcher(batch) { block(ctx2d) } }

    /**
     * Allows to toggle whether stencil-based masks are enabled or not.
     */
	var masksEnabled = true
    var currentBatcher: Any? = null

    /**
     * Flushes all the pending renderings. This is called automatically at the end of the frame.
     * You should call this if you plan to render something else not managed via [batch],
     * so all the pending vertices are drawn.
     */
	fun flush(kind: FlushKind = FlushKind.FULL) {
        flushers(kind)
	}

    var currentFrameBuffer: AGFrameBuffer = ag.mainFrameBuffer
    val currentFrameBufferOrMain: AGFrameBuffer get() = currentFrameBuffer ?: mainFrameBuffer
    val isRenderingToWindow: Boolean get() = currentFrameBufferOrMain === mainFrameBuffer
    val isRenderingToTexture: Boolean get() = !isRenderingToWindow

    // On macOS components, this will be the size of the component
    open val backWidth: Int get() = mainFrameBuffer.width
    open val backHeight: Int get() = mainFrameBuffer.height

    // On macOS components, this will be the full size of the window
    val realBackWidth get() = mainFrameBuffer.fullWidth
    val realBackHeight get() = mainFrameBuffer.fullHeight

    val currentWidth: Int get() = currentFrameBuffer?.width ?: mainFrameBuffer.width
    val currentHeight: Int get() = currentFrameBuffer?.height ?: mainFrameBuffer.height

    val mainFrameBuffer: AGFrameBuffer get() = ag.mainFrameBuffer

    fun clear(
        color: RGBA = Colors.TRANSPARENT,
        depth: Float = 1f,
        stencil: Int = 0,
        clearColor: Boolean = true,
        clearDepth: Boolean = true,
        clearStencil: Boolean = true,
        scissor: AGScissor = AGScissor.NIL,
    ) {
        ag.clear(
            currentFrameBuffer.base,
            currentFrameBuffer.info,
            color,
            depth,
            stencil,
            clearColor,
            clearDepth,
            clearStencil,
            scissor
        )
    }

    inline fun renderToFrameBuffer(
        frameBuffer: AGFrameBuffer,
        clear: Boolean = true,
        render: (AGFrameBuffer) -> Unit,
    ) {
        setFrameBufferTemporally(frameBuffer) {
            useBatcher { batch ->
                batch.scissor(AGScissor(0, 0, frameBuffer.width, frameBuffer.height)) {
                    if (clear) clear(Colors.TRANSPARENT)
                    render(frameBuffer)
                }
            }
        }
    }

    /**
     * Temporarily sets the render buffer to a temporal texture of the size [width] and [height] that can be used later in the [use] method.
     * First the texture is created, then [render] method is called once the render buffer is set to the texture,
     * and later the context is restored and the [use] method is called providing as first argument the rendered [Texture].
     * This method is useful for per-frame filters. If you plan to keep the texture data, consider using the [renderToBitmap] method.
     */
    inline fun renderToTexture(
        width: Int, height: Int,
        render: (AGFrameBuffer) -> Unit = {},
        hasDepth: Boolean = false, hasStencil: Boolean = true, msamples: Int = 1,
        use: (texture: Texture) -> Unit
    ) {
		flush()
        tempAllocateFrameBuffer(width, height, hasDepth = hasDepth, hasStencil = hasStencil, msamples = msamples) { fb ->
            renderToFrameBuffer(fb) { render(it) }
            use(Texture(fb).sliceWithSize(0, 0, width, height))
            flush()
        }
	}

    /**
     * Finishes the drawing and flips the screen. Called by the KorGe engine at the end of the frame.
     */
	fun finish() {
		ag.finish()
        frameBuffers.free(frameFrameBuffers)
        if (frameFrameBuffers.isNotEmpty()) frameFrameBuffers.clear()
    }

    /**
     * Temporarily allocates a [Texture] with its coords from a [BmpSlice].
     * Textures are managed (allocated and de-allocated) automatically by the engine as required.
     * The texture coords matches the region in the [BmpSlice].
     */
    fun getTex(bmp: BmpSlice): Texture = agBitmapTextureManager.getTexture(bmp)
	fun getTex(bmp: BitmapCoords): TextureCoords = agBitmapTextureManager.getTexture(bmp)
    fun getBuffer(buffer: AgCachedBuffer): AGBuffer = agBufferManager.getBuffer(buffer)

    /**
     * Allocates a [Texture.Base] from a [Bitmap]. A Texture.Base doesn't have region information.
     * It is just the whole texture/bitmap.
     */
    fun getTex(bmp: Bitmap): TextureBase = agBitmapTextureManager.getTextureBase(bmp)

    /**
     * References a [closeable] for this frame that will be tracked in next frames.
     * If after a period of time, this closeable has not been referenced in between frames,
     * the [Closeable.close] method will be called so the object can be freed.
     *
     * This can be use for example to automatically manage temporal/cached textures.
     */
    fun refGcCloseable(closeable: Closeable) = agAutoFreeManager.reference(closeable)

    internal fun beforeRender() {
        batch.beforeRender()
    }

    internal fun afterRender() {
        flush()
        finish()
        batch.afterRender()
        agAutoFreeManager.afterRender()
        agBitmapTextureManager.afterRender()
        agBufferManager.afterRender()
    }

    inline fun <T> useBatcher(batcher: T, block: (T) -> Unit) {
        if (currentBatcher !== batcher) {
            //println("currentBatcher=$currentBatcher, batcher=$batcher")
            currentBatcher = batcher
            flush()
        }
        block(batcher)
    }

    override fun close() {
        agBitmapTextureManager.close()
        agAutoFreeManager.close()
    }

    /////////////// FROM AG ///////////////////

    val frameFrameBuffers = LinkedHashSet<AGFrameBuffer>()
    val frameBuffers: Pool<AGFrameBuffer> = Pool { AGFrameBuffer(id = it) }
    val frameBufferStack = FastArrayList<AGFrameBuffer>()

    inline fun doRender(block: () -> Unit) {
        _buffers.reset()
        ag.startFrame()
        try {
            //mainRenderBuffer.init()
            setFrameBufferTemporally(mainFrameBuffer) {
                block()
            }
        } finally {
            ag.endFrame()
        }
    }

    inline fun setFrameBufferTemporally(rb: AGFrameBuffer, callback: (AGFrameBuffer) -> Unit) {
        pushFrameBuffer(rb)
        try {
            callback(rb)
        } finally {
            popFrameBuffer()
        }
    }
    inline fun tempAllocateFrameBuffer(width: Int, height: Int, hasDepth: Boolean = false, hasStencil: Boolean = true, msamples: Int = 1, block: (rb: AGFrameBuffer) -> Unit) {
        val rb = unsafeAllocateFrameBuffer(width, height, hasDepth = hasDepth, hasStencil = hasStencil, msamples = msamples)
        try {
            block(rb)
        } finally {
            unsafeFreeFrameBuffer(rb)
        }
    }

    inline fun tempAllocateFrameBuffers2(width: Int, height: Int, hasDepth: Boolean = false, hasStencil: Boolean = true, msamples: Int = 1, block: (rb0: AGFrameBuffer, rb1: AGFrameBuffer) -> Unit) {
        tempAllocateFrameBuffer(width, height, hasDepth, hasStencil, msamples) { rb0 ->
            tempAllocateFrameBuffer(width, height, hasDepth, hasStencil, msamples) { rb1 ->
                block(rb0, rb1)
            }
        }
    }

    @KoragExperimental
    fun unsafeAllocateFrameBuffer(width: Int, height: Int, hasDepth: Boolean = false, hasStencil: Boolean = true, msamples: Int = 1, onlyThisFrame: Boolean = true): AGFrameBuffer {
        val realWidth = kotlin.math.max(width, 64)
        val realHeight = kotlin.math.max(height, 64)
        val rb = frameBuffers.alloc()
        if (onlyThisFrame) frameFrameBuffers += rb
        rb.setSize(0, 0, realWidth, realHeight, realWidth, realHeight)
        rb.setExtra(hasDepth = hasDepth, hasStencil = hasStencil)
        rb.setSamples(msamples)
        //println("unsafeAllocateFrameRenderBuffer[${rb.id}]($width, $height), real($realWidth, $realHeight), $rb")
        return rb
    }

    @KoragExperimental
    fun unsafeFreeFrameBuffer(rb: AGFrameBuffer) {
        if (frameFrameBuffers.remove(rb)) {
        }
        frameBuffers.free(rb)
    }

    inline fun renderToTextureInternal(
        width: Int, height: Int,
        render: (rb: AGFrameBuffer) -> Unit,
        hasDepth: Boolean = false, hasStencil: Boolean = false, msamples: Int = 1,
        use: (tex: AGTexture, texWidth: Int, texHeight: Int) -> Unit
    ) {
        flush()
        tempAllocateFrameBuffer(width, height, hasDepth, hasStencil, msamples) { rb ->
            setFrameBufferTemporally(rb) {
                clear(Colors.TRANSPARENT) // transparent
                render(rb)
            }
            use(rb.tex, rb.width, rb.height)
        }
    }

    inline fun renderToBitmap(
        width: Int, height: Int,
        hasDepth: Boolean = false, hasStencil: Boolean = false, msamples: Int = 1,
        callback: () -> Unit
    ): Bitmap32 = renderToBitmap(
        Bitmap32(width, height, premultiplied = true),
        hasDepth = hasDepth, hasStencil = hasStencil, msamples = msamples,
        callback = callback
    )

    /**
     * Sets the render buffer temporarily to [bmp] [Bitmap32] and calls the [callback] render method that should perform all the renderings inside.
     */
    inline fun renderToBitmap(
        bmp: Bitmap32,
        hasDepth: Boolean = false, hasStencil: Boolean = false, msamples: Int = 1,
        useTexture: Boolean = true,
        callback: () -> Unit
    ): Bitmap32 {
        if (useTexture) {
            renderToTextureInternal(bmp.width, bmp.height, render = {
                callback()
                //println("renderToBitmap.readColor: $currentRenderBuffer")
                ag.readColor(currentFrameBuffer, bmp)
            }, hasDepth = hasDepth, hasStencil = hasStencil, msamples = msamples, use = { _, _, _ -> })
        } else {
            setFrameBufferTemporally(mainFrameBuffer) {
                updateStandardUniforms()
                clear(Colors.TRANSPARENT) // transparent
                flush()
                callback()
                flush()
                ag.readColor(mainFrameBuffer, bmp)
            }
        }
        return bmp
    }

    fun pushFrameBuffer(frameBuffer: AGFrameBuffer) {
        frameBufferStack.add(currentFrameBuffer)
        currentFrameBuffer = frameBuffer
    }

    fun popFrameBuffer() {
        currentFrameBuffer = frameBufferStack.removeAt(frameBufferStack.size - 1)
    }


    val textureDrawer by lazy { AGTextureDrawer(ag) }
    fun drawTexture(frameBuffer: AGFrameBuffer, tex: AGTexture) {
        textureDrawer.draw(frameBuffer, tex, -1f, +1f, +1f, -1f)
    }

    private val drawTempTexture: AGTexture by lazy { AGTexture() }

    fun drawBitmap(frameBuffer: AGFrameBuffer, bmp: Bitmap) {
        drawTempTexture.upload(bmp, mipmaps = false)
        drawTexture(frameBuffer, drawTempTexture)
        drawTempTexture.upload(Bitmaps.transparent.bmp)
    }

    inline fun backupTexture(frameBuffer: AGFrameBuffer, tex: AGTexture?, callback: () -> Unit) {
        if (tex != null) {
            ag.readToTexture(currentFrameBuffer, tex, 0, 0, currentFrameBuffer.width, currentFrameBuffer.height)
        }
        try {
            callback()
        } finally {
            if (tex != null) drawTexture(frameBuffer, tex)
        }
    }

    operator fun <T : UniformBlock> get(uniformBlock: T): UniformBlockBuffer<T> = _buffers[uniformBlock]
    //operator fun get(uniform: UniformBlock): AGRichUniformBlockData = _buffers[uniform]
    operator fun get(program: Program): AGProgramWithUniforms = _programs.getOrPut(program) { AGProgramWithUniforms(it, _buffers) }

    fun createCurrentUniformsRef(program: Program, autoUpload: Boolean = true): UniformBlocksBuffersRef {
        if (autoUpload) uploadUpdatedUniforms()
        return this[program].createRef()
    }
    fun uploadUpdatedUniforms() {
        _buffers.uploadUpdatedBuffers()
    }
}

inline fun <T : AG> testRenderContext(ag: T, bp: BoundsProvider = BoundsProvider.Base(), block: (RenderContext) -> Unit): T {
    val ctx = RenderContext(ag, bp)
    block(ctx)
    ctx.flush()
    return ag
}

inline fun testRenderContext(bp: BoundsProvider = BoundsProvider.Base(), block: (RenderContext) -> Unit): AGLog = testRenderContext(AGLog(), bp, block)
