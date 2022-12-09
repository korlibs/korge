package com.soywiz.korag

import com.soywiz.kds.Extra
import com.soywiz.kds.FastArrayList
import com.soywiz.kds.FloatArray2
import com.soywiz.kds.Pool
import com.soywiz.kds.fastCastTo
import com.soywiz.klogger.Console
import com.soywiz.kmem.*
import com.soywiz.kmem.unit.ByteUnits
import com.soywiz.korag.annotation.KoragExperimental
import com.soywiz.korag.gl.*
import com.soywiz.korag.shader.Attribute
import com.soywiz.korag.shader.Program
import com.soywiz.korag.shader.VarType
import com.soywiz.korag.shader.VertexLayout
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.Bitmap8
import com.soywiz.korim.bitmap.BitmapSlice
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.RGBAPremultiplied
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.math.nextMultipleOf
import kotlin.math.*


interface AGWindow : AGContainer {
}

interface AGFeatures {
    val parentFeatures: AGFeatures? get() = null
    val graphicExtensions: Set<String> get() = emptySet()
    val isInstancedSupported: Boolean get() = parentFeatures?.isInstancedSupported ?: false
    val isStorageMultisampleSupported: Boolean get() = parentFeatures?.isStorageMultisampleSupported ?: false
    val isFloatTextureSupported: Boolean get() = parentFeatures?.isFloatTextureSupported ?: false
}

abstract class AG(val checked: Boolean = false) : AGFeatures, AGCommandExecutor, Extra by Extra.Mixin() {
    companion object {
        const val defaultPixelsPerInch : Double = 96.0
    }

    abstract val nativeComponent: Any

    open fun contextLost() {
        Console.info("AG.contextLost()", this)
        //printStackTrace("AG.contextLost")
    }

    val tempVertexBufferPool = Pool { createBuffer() }
    val tempIndexBufferPool = Pool { createBuffer() }
    val tempTexturePool = Pool { createTexture() }

    open val maxTextureSize = Size(2048, 2048)

    open val devicePixelRatio: Double = 1.0
    open val pixelsPerLogicalInchRatio: Double = 1.0
    /** Approximate on iOS */
    open val pixelsPerInch: Double = defaultPixelsPerInch
    // Use this in the debug handler, while allowing people to access raw devicePixelRatio without the noise of window scaling
    // I really dont know if "/" or "*" or right but in my mathematical mind "pixelsPerLogicalInchRatio" must increase and not decrease the scale
    // maybe it is pixelsPerLogicalInchRatio / devicePixelRatio ?
    open val computedPixelRatio: Double get() = devicePixelRatio * pixelsPerLogicalInchRatio

    open fun beforeDoRender() {
    }

    inline fun doRender(block: () -> Unit) {
        beforeDoRender()
        startFrame()
        try {
            //mainRenderBuffer.init()
            setRenderBufferTemporally(mainRenderBuffer) {
                block()
            }
        } finally {
            endFrame()
        }
    }

    open fun offscreenRendering(callback: () -> Unit) {
        callback()
    }

    open fun repaint() {
    }

    fun resized(width: Int, height: Int) {
        resized(0, 0, width, height, width, height)
    }

    open fun resized(x: Int, y: Int, width: Int, height: Int, fullWidth: Int, fullHeight: Int) {
        mainRenderBuffer.setSize(x, y, width, height, fullWidth, fullHeight)
    }

    open fun dispose() {
    }

    // On MacOS components, this will be the size of the component
    open val backWidth: Int get() = mainRenderBuffer.width
    open val backHeight: Int get() = mainRenderBuffer.height

    // On MacOS components, this will be the full size of the window
    val realBackWidth get() = mainRenderBuffer.fullWidth
    val realBackHeight get() = mainRenderBuffer.fullHeight

    val currentWidth: Int get() = currentRenderBuffer?.width ?: mainRenderBuffer.width
    val currentHeight: Int get() = currentRenderBuffer?.height ?: mainRenderBuffer.height

    var contextVersion: Int = 0

    //protected fun setViewport(v: IntArray) = setViewport(v[0], v[1], v[2], v[3])

    var createdTextureCount = 0
    var deletedTextureCount = 0

    internal val textures = LinkedHashSet<AGTexture>()
    private val texturesCount: Int get() = textures.size
    private val texturesMemory: ByteUnits get() = ByteUnits.fromBytes(textures.sumOf { it.estimatedMemoryUsage.bytesLong })

    internal val buffers = LinkedHashSet<AGBuffer>()
    private val buffersCount: Int get() = buffers.size
    private val buffersMemory: ByteUnits get() = ByteUnits.fromBytes(buffers.sumOf { it.estimatedMemoryUsage.bytesLong })

    val dummyTexture by lazy { createTexture() }

    fun createTexture(): AGTexture = createTexture(premultiplied = true)
    fun createTexture(bmp: Bitmap, mipmaps: Boolean = false): AGTexture = createTexture(bmp.premultiplied).upload(bmp, mipmaps)
    @Deprecated("This will copy the data")
    fun createTexture(bmp: BitmapSlice<Bitmap>, mipmaps: Boolean = false): AGTexture = createTexture(bmp.premultiplied).upload(bmp, mipmaps)
    fun createTexture(bmp: Bitmap, mipmaps: Boolean = false, premultiplied: Boolean = true): AGTexture = createTexture(premultiplied).upload(bmp, mipmaps)
    open fun createTexture(premultiplied: Boolean, targetKind: AGTextureTargetKind = AGTextureTargetKind.TEXTURE_2D): AGTexture = AGTexture(this, premultiplied, targetKind)
    open fun createBuffer(): AGBuffer = AGBuffer(this)

    fun createVertexData(vararg attributes: Attribute, layoutSize: Int? = null) = AGVertexData(createBuffer(), VertexLayout(*attributes, layoutSize = layoutSize))
    fun createBuffer(data: Buffer) = createBuffer().apply { upload(data) }
    fun createBuffer(data: ShortArray, offset: Int = 0, length: Int = data.size - offset) = createBuffer().apply { upload(data, offset, length) }
    fun createBuffer(data: FloatArray, offset: Int = 0, length: Int = data.size - offset) = createBuffer().apply { upload(data, offset, length) }

    fun drawV2(
        vertexData: AGVertexArrayObject,
        program: Program,
        type: AGDrawType,
        vertexCount: Int,
        indices: AGBuffer? = null,
        indexType: AGIndexType = AGIndexType.USHORT,
        offset: Int = 0,
        blending: AGBlending = AGBlending.NORMAL,
        uniforms: AGUniformValues = AGUniformValues.EMPTY,
        stencilRef: AGStencilReference = AGStencilReference.DEFAULT,
        stencilOpFunc: AGStencilOpFunc = AGStencilOpFunc.DEFAULT,
        colorMask: AGColorMask = AGColorMask.ALL_ENABLED,
        renderState: AGDepthAndFrontFace = AGDepthAndFrontFace.DEFAULT,
        scissor: AGScissor = AGScissor.NIL,
        instances: Int = 1
    ) = draw(batch.also { batch ->
        batch.vertexData = vertexData
        batch.program = program
        batch.drawType = type
        batch.vertexCount = vertexCount
        batch.indices = indices
        batch.indexType = indexType
        batch.drawOffset = offset
        batch.blending = blending
        batch.uniforms = uniforms
        batch.stencilRef = stencilRef
        batch.stencilOpFunc = stencilOpFunc
        batch.colorMask = colorMask
        batch.depthAndFrontFace = renderState
        batch.scissor = scissor
        batch.instances = instances
    })

    private val batch = AGBatch()

    override fun execute(command: AGCommand) {
        when (command) {
            is AGClear -> clear(command)
            is AGBatch -> draw(batch)
            is AGBlitPixels -> blit(command)
            is AGReadPixelsToTexture -> TODO()
        }
    }

    open fun blit(command: AGBlitPixels) {

    }

    open fun clear(clear: AGClear) = Unit
    abstract fun draw(batch: AGBatch)

    fun AGUniformValues.useExternalSampler(): Boolean {
        var useExternalSampler = false
        this.fastForEach { value ->
            val uniform = value.uniform
            val uniformType = uniform.type
            when (uniformType) {
                VarType.Sampler2D -> {
                    val unit = value.nativeValue?.fastCastTo<AGTextureUnit>()
                    val tex = (unit?.texture?.fastCastTo<AGTexture?>())
                    if (tex != null) {
                        if (tex.implForcedTexTarget == AGTextureTargetKind.EXTERNAL_TEXTURE) {
                            useExternalSampler = true
                        }
                    }
                }
                else -> Unit
            }
        }
        //println("useExternalSampler=$useExternalSampler")
        return useExternalSampler
    }

    open fun disposeTemporalPerFrameStuff() = Unit

    val frameRenderBuffers = LinkedHashSet<AGFrameBuffer>()
    val renderBuffers = Pool<AGFrameBuffer>() { createRenderBuffer() }

    object RenderBufferConsts {
        const val DEFAULT_INITIAL_WIDTH = 128
        const val DEFAULT_INITIAL_HEIGHT = 128
    }

    internal val allRenderBuffers = LinkedHashSet<AGFrameBuffer>()
    private val renderBufferCount: Int get() = allRenderBuffers.size
    private val renderBuffersMemory: ByteUnits get() = ByteUnits.fromBytes(allRenderBuffers.sumOf { it.estimatedMemoryUsage.bytesLong })


    @KoragExperimental
    var agTarget = AGTarget.DISPLAY

    val mainRenderBuffer: AGFrameBuffer by lazy {
        when (agTarget) {
            AGTarget.DISPLAY -> createMainRenderBuffer()
            AGTarget.OFFSCREEN -> createRenderBuffer()
        }
    }

    var lastRenderContextId = 0

    open fun createMainRenderBuffer(): AGFrameBuffer = AGFrameBuffer(this, isMain = true)
    open fun createRenderBuffer(): AGFrameBuffer = AGFrameBuffer(this, isMain = false)

    //open fun createRenderBuffer() = RenderBuffer()

    open fun flip() {
    }

    open fun flipInternal() = Unit

    open fun startFrame() {
    }
    open fun endFrame() {
    }

    open fun clear(
        color: RGBA = Colors.TRANSPARENT_BLACK,
        depth: Float = 1f,
        stencil: Int = 0,
        clearColor: Boolean = true,
        clearDepth: Boolean = true,
        clearStencil: Boolean = true,
        scissor: AGScissor = AGScissor.NIL,
    ) {
    }


    private val finalScissorBL = Rectangle()
    @PublishedApi internal val tempRect = Rectangle()

    fun clearStencil(stencil: Int = 0, scissor: AGScissor = AGScissor.NIL) = clear(clearColor = false, clearDepth = false, clearStencil = true, stencil = stencil, scissor = scissor)
    fun clearDepth(depth: Float = 1f, scissor: AGScissor = AGScissor.NIL) = clear(clearColor = false, clearDepth = true, clearStencil = false, depth = depth, scissor = scissor)
    fun clearColor(color: RGBA = Colors.TRANSPARENT_BLACK, scissor: AGScissor = AGScissor.NIL) = clear(clearColor = true, clearDepth = false, clearStencil = false, color = color, scissor = scissor)

    val renderBufferStack = FastArrayList<AGFrameBuffer?>()

    //@PublishedApi
    @KoragExperimental
    var currentRenderBuffer: AGFrameBuffer? = null
        protected set

    val currentRenderBufferOrMain: AGFrameBuffer get() = currentRenderBuffer ?: mainRenderBuffer

    val isRenderingToWindow: Boolean get() = currentRenderBufferOrMain === mainRenderBuffer
    val isRenderingToTexture: Boolean get() = !isRenderingToWindow

    inline fun backupTexture(tex: AGTexture?, callback: () -> Unit) {
        if (tex != null) {
            readColorTexture(tex, 0, 0, backWidth, backHeight)
        }
        try {
            callback()
        } finally {
            if (tex != null) drawTexture(tex)
        }
    }

    inline fun setRenderBufferTemporally(rb: AGFrameBuffer, callback: (AGFrameBuffer) -> Unit) {
        pushRenderBuffer(rb)
        try {
            callback(rb)
        } finally {
            popRenderBuffer()
        }
    }

    var adjustFrameRenderBufferSize = false
    //var adjustFrameRenderBufferSize = true

    //open fun fixWidthForRenderToTexture(width: Int): Int = kotlin.math.max(64, width).nextPowerOfTwo
    //open fun fixHeightForRenderToTexture(height: Int): Int = kotlin.math.max(64, height).nextPowerOfTwo

    open fun fixWidthForRenderToTexture(width: Int): Int = if (adjustFrameRenderBufferSize) width.nextMultipleOf(64) else width
    open fun fixHeightForRenderToTexture(height: Int): Int = if (adjustFrameRenderBufferSize) height.nextMultipleOf(64) else height

    //open fun fixWidthForRenderToTexture(width: Int): Int = width
    //open fun fixHeightForRenderToTexture(height: Int): Int = height

    inline fun tempAllocateFrameBuffer(width: Int, height: Int, hasDepth: Boolean = false, hasStencil: Boolean = true, msamples: Int = 1, block: (rb: AGFrameBuffer) -> Unit) {
        val rb = unsafeAllocateFrameRenderBuffer(width, height, hasDepth = hasDepth, hasStencil = hasStencil, msamples = msamples)
        try {
            block(rb)
        } finally {
            unsafeFreeFrameRenderBuffer(rb)
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
    fun unsafeAllocateFrameRenderBuffer(width: Int, height: Int, hasDepth: Boolean = false, hasStencil: Boolean = true, msamples: Int = 1, onlyThisFrame: Boolean = true): AGFrameBuffer {
        val realWidth = fixWidthForRenderToTexture(kotlin.math.max(width, 64))
        val realHeight = fixHeightForRenderToTexture(kotlin.math.max(height, 64))
        val rb = renderBuffers.alloc()
        if (onlyThisFrame) frameRenderBuffers += rb
        rb.setSize(0, 0, realWidth, realHeight, realWidth, realHeight)
        rb.setExtra(hasDepth = hasDepth, hasStencil = hasStencil)
        rb.setSamples(msamples)
        //println("unsafeAllocateFrameRenderBuffer($width, $height), real($realWidth, $realHeight), $rb")
        return rb
    }

    @KoragExperimental
    fun unsafeFreeFrameRenderBuffer(rb: AGFrameBuffer) {
        if (frameRenderBuffers.remove(rb)) {
        }
        renderBuffers.free(rb)
    }

    open fun flush() {
    }

    @OptIn(KoragExperimental::class)
    inline fun renderToTexture(
        width: Int, height: Int,
        render: (rb: AGFrameBuffer) -> Unit,
        hasDepth: Boolean = false, hasStencil: Boolean = false, msamples: Int = 1,
        use: (tex: AGTexture, texWidth: Int, texHeight: Int) -> Unit
    ) {
        flush()
        tempAllocateFrameBuffer(width, height, hasDepth, hasStencil, msamples) { rb ->
            setRenderBufferTemporally(rb) {
                clear(Colors.TRANSPARENT_BLACK) // transparent
                render(rb)
            }
            use(rb.tex, rb.width, rb.height)
        }
    }

    inline fun renderToBitmap(
        bmp: Bitmap32,
        hasDepth: Boolean = false, hasStencil: Boolean = false, msamples: Int = 1,
        render: () -> Unit
    ) {
        renderToTexture(bmp.width, bmp.height, render = {
            render()
            //println("renderToBitmap.readColor: $currentRenderBuffer")
            readColor(bmp)
        }, hasDepth = hasDepth, hasStencil = hasStencil, msamples = msamples, use = { _, _, _ -> })
    }

    open fun setRenderBuffer(renderBuffer: AGFrameBuffer?): AGFrameBuffer? {
        return null
    }

    fun getRenderBufferAtStackPoint(offset: Int): AGFrameBuffer {
        if (offset == 0) return currentRenderBufferOrMain
        return renderBufferStack.getOrNull(renderBufferStack.size + offset) ?: mainRenderBuffer
    }

    fun pushRenderBuffer(renderBuffer: AGFrameBuffer) {
        renderBufferStack.add(currentRenderBuffer)
        setRenderBuffer(renderBuffer)
    }

    fun popRenderBuffer() {
        setRenderBuffer(renderBufferStack.last())
        renderBufferStack.removeAt(renderBufferStack.size - 1)
    }

    // @TODO: Rename to Sync and add Suspend versions
    fun readPixel(x: Int, y: Int): RGBA {
        val rawColor = Bitmap32(1, 1, premultiplied = isRenderingToTexture).also { readColor(it, x, y) }.ints[0]
        return if (isRenderingToTexture) RGBAPremultiplied(rawColor).depremultiplied else RGBA(rawColor)
    }

    open fun readColor(bitmap: Bitmap32, x: Int = 0, y: Int = 0) {
    }
    open fun readDepth(width: Int, height: Int, out: FloatArray) {
    }
    open fun readStencil(bitmap: Bitmap8) {
    }
    fun readDepth(out: FloatArray2): Unit = readDepth(out.width, out.height, out.data)
    open fun readColorTexture(texture: AGTexture, x: Int = 0, y: Int = 0, width: Int = backWidth, height: Int = backHeight): Unit = TODO()
    fun readColor(): Bitmap32 = Bitmap32(backWidth, backHeight, premultiplied = isRenderingToTexture).apply { readColor(this) }
    fun readDepth(): FloatArray2 = FloatArray2(backWidth, backHeight) { 0f }.apply { readDepth(this) }

    //////////////

    val flipRenderTexture = true

    val textureDrawer by lazy { AGTextureDrawer(this) }
    fun drawTexture(tex: AGTexture) {
        textureDrawer.draw(tex, -1f, +1f, +1f, -1f)
    }

    private val drawTempTexture: AGTexture by lazy { createTexture() }

    fun drawBitmap(bmp: Bitmap) {
        drawTempTexture.upload(bmp, mipmaps = false)
        drawTexture(drawTempTexture)
        drawTempTexture.upload(Bitmaps.transparent.bmp)
    }

    private val stats = AGStats()

    internal var programCount = 0

    fun getStats(out: AGStats = stats): AGStats {
        out.texturesMemory = this.texturesMemory
        out.texturesCount = this.texturesCount
        out.buffersMemory = this.buffersMemory
        out.buffersCount = this.buffersCount
        out.renderBuffersMemory = this.renderBuffersMemory
        out.renderBuffersCount = this.renderBufferCount
        out.texturesCreated = this.createdTextureCount
        out.texturesDeleted = this.deletedTextureCount
        out.programCount = this.programCount
        return out
    }
}
