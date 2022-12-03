package com.soywiz.korge.render

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.log.*
import com.soywiz.korag.shader.Uniform
import com.soywiz.korge.internal.*
import com.soywiz.korge.stat.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import kotlin.coroutines.*

/**
 * A context that allows to render objects.
 *
 * The [RenderContext] contains the [nag] [NAG] (Accelerated Graphics),
 * that allow to render triangles and other primitives to the current render buffer.
 *
 * When doing 2D, you should usually use the [batch] to buffer vertices,
 * so they can be rendered at once when flushing.
 *
 * If you plan to do a custom drawing using [nag] directly, you should call [flush],
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
    val nag: NAG,
	val bp: BoundsProvider = BoundsProvider.Base(),
    /** Object storing all the rendering [Stats] like number of batches, number of vertices etc. */
	val stats: Stats = Stats(),
	val coroutineContext: CoroutineContext = EmptyCoroutineContext,
    val batchMaxQuads: Int = BatchBuilder2D.DEFAULT_BATCH_QUADS
) : Extra by Extra.Mixin(), BoundsProvider by bp, Closeable {
    val projectionMatrixTransform = Matrix()
    val projectionMatrixTransformInv = Matrix()
    private val projMat: Matrix3D = Matrix3D()
    @KorgeInternal
    val viewMat: Matrix3D = Matrix3D()
    @KorgeInternal
    val viewMat2D: Matrix = Matrix()

    @KorgeInternal
    val uniforms: AGUniformValues = AGUniformValues().also {
        it[DefaultShaders.u_ProjMat] = projMat
        it[DefaultShaders.u_ViewMat] = viewMat
    }

    val texturePool = Pool { NAGTexture() }

    var flipRenderTexture = true
    //var flipRenderTexture = false
    private val tempRect = Rectangle()
    private val tempMat3d = Matrix3D()

    var currentFrameBuffer: NAGFrameBuffer? = null

    val graphicExtensions: List<String> get() = nag.graphicExtensions
    val isFloatTextureSupported: Boolean get() = nag.isFloatTextureSupported
    val isInstancedSupported: Boolean get() = nag.isInstancedSupported
    val currentFrameBufferWidth: Int get() = currentFrameBuffer?.width ?: nag.finalFrameBufferWidth
    val currentFrameBufferHeight: Int get() = currentFrameBuffer?.height ?: nag.finalFrameBufferHeight

    val isRenderingToTexture: Boolean get() = currentFrameBuffer != null
    val isRenderingToWindow: Boolean get() = !isRenderingToTexture
    val currentWidth: Int get() = nag.finalFrameBufferWidth
    val currentHeight: Int get() = nag.finalFrameBufferHeight
    val devicePixelRatio: Double get() = nag.devicePixelRatio
    val pixelsPerInch: Double get() = nag.pixelsPerInch
    val computedPixelRatio: Double get() = nag.devicePixelRatio
    val nativeWidth: Int get() = nag.finalFrameBufferWidth
    val nativeHeight: Int get() = nag.finalFrameBufferHeight
    var debugExtraFontScale : Double = 1.0
    var debugExtraFontColor : RGBA = Colors.WHITE
    val debugOverlayScale: Double get() = kotlin.math.round(computedPixelRatio * debugExtraFontScale).coerceAtLeast(1.0)

    @OptIn(KorgeInternal::class)
    fun updateStandardUniforms() {
        //println("updateStandardUniforms: ag.currentSize(${ag.currentWidth}, ${ag.currentHeight}) : ${ag.currentRenderBuffer}")
        if (flipRenderTexture && isRenderingToTexture) {
            projMat.setToOrtho(tempRect.setBounds(0, currentHeight, currentWidth, 0), -1f, 1f)
        } else {
            projMat.setToOrtho(tempRect.setBounds(0, 0, currentWidth, currentHeight), -1f, 1f)
            projMat.multiply(projMat, projectionMatrixTransform.toMatrix3D(tempMat3d))
        }
        uniforms[DefaultShaders.u_ProjMat] = projMat
        uniforms[DefaultShaders.u_ViewMat] = viewMat

    }

    /**
     * Executes [callback] while setting temporarily the view matrix to [matrix]
     */
    inline fun setViewMatrixTemp(matrix: Matrix, crossinline callback: () -> Unit) {
        matrix3DPool.alloc { temp ->
            matrixPool.alloc { temp2d ->
                flush()
                temp.copyFrom(this.viewMat)
                temp2d.copyFrom(this.viewMat2D)
                this.viewMat2D.copyFrom(matrix)
                this.viewMat.copyFrom(matrix)
                //println("viewMat: $viewMat, matrix: $matrix")
                try {
                    callback()
                } finally {
                    flush()
                    this.viewMat.copyFrom(temp)
                    this.viewMat2D.copyFrom(temp2d)
                }
            }
        }
    }


    @PublishedApi internal val tempUniforms = Pool(reset = { it.clear() }) { AGUniformValues() }

    open fun startFrame() {
        nag.startFrame()
    }

    open fun clear(
        color: RGBA = Colors.TRANSPARENT_BLACK,
        depth: Float = 1f,
        stencil: Int = 0,
        clearColor: Boolean = true,
        clearDepth: Boolean = true,
        clearStencil: Boolean = true,
        scissor: AGRect = AGRect.NIL,
    ) {
        nag.clear(currentFrameBuffer, if (clearColor) color else null, if (clearDepth) depth else null, if (clearStencil) stencil else null)
    }

    /**
     * Support restoring uniforms later.
     */
    inline fun keepUniforms(callback: (AGUniformValues) -> Unit) {
        tempUniforms.alloc { temp ->
            temp.setTo(this.uniforms)
            try {
                callback(this.uniforms)
            } finally {
                this.uniforms.setTo(temp)
            }
        }
    }

    /**
     * Executes [callback] while setting temporarily an [uniform] to a [value]
     */
    inline fun keepUniform(uniform: Uniform, flush: Boolean = true, callback: (AGUniformValues) -> Unit) {
        tempUniforms.alloc { temp ->
            if (flush) flush()
            temp[uniform] = this.uniforms[uniform]
            try {
                callback(this.uniforms)
            } finally {
                if (flush) flush()
                this.uniforms[uniform] = temp[uniform]
            }
        }
    }

    @PublishedApi
    internal val tempOldUniformsList: Pool<AGUniformValues> = Pool { AGUniformValues() }

    val agAutoFreeManager = AgAutoFreeManager()
	val agBitmapTextureManager = AgBitmapTextureManager()
    val agBufferManager = AgBufferManager()

    enum class FlushKind { STATE, FULL }

    /** Allows to register handlers when the [flush] method is called */
    val flushers = Signal<FlushKind>()

    val views: Views? = bp as? Views?

    var debugAnnotateView: View? = null
        set(value) {
            views?.invalidatedView(field)
            field = value
            views?.invalidatedView(field)
        }

    var stencilIndex: Int = 0

    /** Allows to draw quads, sprites and nine patches using a precomputed global matrix or raw vertices */
    @Deprecated("Use useBatcher instead")
    @KorgeInternal
    val batch = BatchBuilder2D(this, batchMaxQuads)

    val dynamicVertexBufferPool = Pool { NAGBuffer() }

    @OptIn(KorgeInternal::class)
    inline fun useBatcher(block: (BatchBuilder2D) -> Unit) = batch.use(block)

    /** [RenderContext2D] similar to the one from JS, that keeps a matrix (affine transformation) and allows to draw shapes using the current matrix */
    @KorgeInternal
    @Deprecated("Use useCtx2d instead")
    val ctx2d = RenderContext2D(batch, agBitmapTextureManager)

    @Suppress("DEPRECATION")
    @OptIn(KorgeInternal::class)
    inline fun useCtx2d(block: (RenderContext2D) -> Unit) { useBatcher(batch) { block(ctx2d) } }

    /** Pool of [Matrix] objects that could be used temporarily by renders */
    val matrixPool = Pool(reset = { it.identity() }, preallocate = 8) { Matrix() }
    /** Pool of [Matrix3D] objects that could be used temporarily by renders */
    val matrix3DPool = Pool(reset = { it.identity() }, preallocate = 8) { Matrix3D() }
    /** Pool of [Point] objects that could be used temporarily by renders */
    val pointPool = Pool(reset = { it.setTo(0, 0) }, preallocate = 8) { Point() }
    /** Pool of [Rectangle] objects that could be used temporarily by renders */
    val rectPool = Pool(reset = { it.setTo(0, 0, 0, 0) }, preallocate = 8) { Rectangle() }

    val tempMargin: MutableMarginInt = MutableMarginInt()

    val identityMatrix = Matrix()

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
        //currentBatcher = null
        flushers(kind)
	}

    fun unsafeAllocateFrameRenderBuffer(
        width: Int, height: Int, hasDepth: Boolean = false, hasStencil: Boolean = true, msamples: Int = 1, onlyThisFrame: Boolean = true
    ): NAGFrameBuffer = tempNagFrameBufferPool.alloc()

    fun unsafeFreeFrameRenderBuffer(rb: NAGFrameBuffer) {
        tempNagFrameBufferPool.free(rb)
    }

    inline fun renderToFrameBuffer(
        frameBuffer: NAGFrameBuffer,
        clear: Boolean = true,
        render: (NAGFrameBuffer) -> Unit,
    ) {
        flush()
        val oldFrameBuffer = currentFrameBuffer
        try {
            currentFrameBuffer = frameBuffer
            useBatcher { batch ->
                val oldScissors = batch.scissor
                batch.scissor = AGRect(0, 0, frameBuffer.width, frameBuffer.height)
                //batch.scissor = null
                try {
                    if (clear) clear(Colors.TRANSPARENT_BLACK)
                    render(frameBuffer)
                    flush()
                } finally {
                    batch.scissor = oldScissors
                }
            }
        } finally {
            currentFrameBuffer = oldFrameBuffer
        }
    }

    @PublishedApi internal val tempNagFrameBufferPool = Pool(reset = {}) { NAGFrameBuffer() }

    inline fun tempAllocateFrameBuffer(width: Int, height: Int, hasDepth: Boolean = false, hasStencil: Boolean = true, msamples: Int = 1, block: (fb: NAGFrameBuffer) -> Unit) {
        tempNagFrameBufferPool.alloc {
            block(it.set(width, height, hasStencil, hasDepth))
        }
    }

    inline fun tempAllocateFrameBuffers2(width: Int, height: Int, hasDepth: Boolean = false, hasStencil: Boolean = true, msamples: Int = 1, block: (fb1: NAGFrameBuffer, fb2: NAGFrameBuffer) -> Unit) {
        tempAllocateFrameBuffer(width, height, hasDepth, hasStencil, msamples) { fb1 ->
            tempAllocateFrameBuffer(width, height, hasDepth, hasStencil, msamples) { fb2 ->
                block(fb1, fb2)
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
        render: (NAGFrameBuffer) -> Unit = {},
        hasDepth: Boolean = false, hasStencil: Boolean = true, msamples: Int = 1,
        use: (texture: Texture) -> Unit
    ) {
		flush()
        tempAllocateFrameBuffer(width, height, hasDepth = hasDepth, hasStencil = hasStencil, msamples = msamples) { fb ->
            renderToFrameBuffer(fb) { render(it) }
            use(Texture(fb).slice(0, 0, width, height))
            flush()
        }
	}

    /**
     * Sets the render buffer temporarily to [bmp] [Bitmap32] and calls the [callback] render method that should perform all the renderings inside.
     */
	inline fun renderToBitmap(
        bmp: Bitmap32,
        hasDepth: Boolean = false, hasStencil: Boolean = false, msamples: Int = 1,
        callback: () -> Unit
    ): Bitmap32 {
		flush()
        TODO()
		//ag.renderToBitmap(bmp, hasDepth, hasStencil, msamples) {
		//	callback()
		//	flush()
		//}
		return bmp
	}

    inline fun renderToBitmap(
        width: Int, height: Int,
        hasDepth: Boolean = false, hasStencil: Boolean = false, msamples: Int = 1,
        callback: () -> Unit
    ): Bitmap32 =
        renderToBitmap(
            Bitmap32(width, height),
            hasDepth = hasDepth, hasStencil = hasStencil, msamples = msamples,
            callback = callback
        )

    /**
     * Finishes the drawing and flips the screen. Called by the KorGe engine at the end of the frame.
     */
	fun finish() {
		nag.finish()
	}

    /**
     * Temporarily allocates a [Texture] with its coords from a [BmpSlice].
     * Textures are managed (allocated and de-allocated) automatically by the engine as required.
     * The texture coords matches the region in the [BmpSlice].
     */
    fun getTex(bmp: BmpSlice): Texture = agBitmapTextureManager.getTexture(bmp)
	fun getTex(bmp: BitmapCoords): TextureCoords = agBitmapTextureManager.getTexture(bmp)
    fun getBuffer(buffer: Buffer): NAGBuffer = agBufferManager.getBuffer(buffer)

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

    internal fun afterRender() {
        flush(FlushKind.FULL)
        finish()
        agAutoFreeManager.afterRender()
        agBitmapTextureManager.afterRender()
        agBufferManager.afterRender()
    }

    inline fun <T> useBatcher(batcher: T, block: (T) -> Unit) {
        if (currentBatcher !== batcher) {
            currentBatcher = batcher
            flush(FlushKind.FULL)
        }
        block(batcher)
    }

    override fun close() {
        agBitmapTextureManager.close()
        agAutoFreeManager.close()
    }
}

inline fun <T: NAG> testRenderContext(nag: T, bp: BoundsProvider = BoundsProvider.Base(), block: (RenderContext) -> Unit): T {
    val ctx = RenderContext(nag, bp)
    block(ctx)
    ctx.flush()
    return nag
}

inline fun testRenderContext(bp: BoundsProvider = BoundsProvider.Base(), block: (RenderContext) -> Unit): NAGLog {
    return testRenderContext(NAGLog(), bp, block)
}
