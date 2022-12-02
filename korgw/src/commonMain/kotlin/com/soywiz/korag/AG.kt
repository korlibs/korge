package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.klogger.Console
import com.soywiz.kmem.*
import com.soywiz.kmem.unit.ByteUnits
import com.soywiz.korag.annotation.KoragExperimental
import com.soywiz.korag.shader.Attribute
import com.soywiz.korag.shader.Program
import com.soywiz.korag.shader.ProgramConfig
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
import com.soywiz.korio.annotations.KorIncomplete
import com.soywiz.korio.async.runBlockingNoJs
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.math.nextMultipleOf
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmOverloads
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

@OptIn(KorIncomplete::class)
abstract class AG(val checked: Boolean = false) : AGFeatures, Extra by Extra.Mixin() {
    companion object {
        const val defaultPixelsPerInch : Double = 96.0
    }

    abstract val nativeComponent: Any

    open fun contextLost() {
        Console.info("AG.contextLost()", this)
        //printStackTrace("AG.contextLost")
        commandsSync { it.contextLost() }
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
        mainRenderBuffer.init()
        setRenderBufferTemporally(mainRenderBuffer) {
            block()
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
    fun createTexture(bmp: BitmapSlice<Bitmap>, mipmaps: Boolean = false): AGTexture = createTexture(bmp.premultiplied).upload(bmp, mipmaps)
    fun createTexture(bmp: Bitmap, mipmaps: Boolean = false, premultiplied: Boolean = true): AGTexture = createTexture(premultiplied).upload(bmp, mipmaps)
    open fun createTexture(premultiplied: Boolean, targetKind: AGTextureTargetKind = AGTextureTargetKind.TEXTURE_2D): AGTexture = AGTexture(this, premultiplied, targetKind)
    open fun createBuffer(): AGBuffer = commandsNoWaitNoExecute { AGBuffer(this, it) }
    @Deprecated("")
    fun createIndexBuffer(): AGBuffer = createBuffer()
    @Deprecated("")
    fun createVertexBuffer(): AGBuffer = createBuffer()

    fun createVertexData(vararg attributes: Attribute, layoutSize: Int? = null) = AGVertexData(createVertexBuffer(), VertexLayout(*attributes, layoutSize = layoutSize))

    fun createIndexBuffer(data: ShortArray, offset: Int = 0, length: Int = data.size - offset) =
        createIndexBuffer().apply {
            upload(data, offset, length)
        }

    fun createIndexBuffer(data: com.soywiz.kmem.Buffer, offset: Int = 0, length: Int = data.size - offset) =
        createIndexBuffer().apply {
            upload(data, offset, length)
        }

    fun createVertexBuffer(data: FloatArray, offset: Int = 0, length: Int = data.size - offset) =
        createVertexBuffer().apply {
            upload(data, offset, length)
        }

    fun createVertexBuffer(data: com.soywiz.kmem.Buffer, offset: Int = 0, length: Int = data.size - offset) =
        createVertexBuffer().apply {
            upload(data, offset, length)
        }

    @Deprecated("Use draw(Batch) or drawV2() instead")
    fun draw(
        vertices: AGBuffer,
        program: Program,
        type: AGDrawType,
        vertexLayout: VertexLayout,
        vertexCount: Int,
        indices: AGBuffer? = null,
        indexType: AGIndexType = AGIndexType.USHORT,
        offset: Int = 0,
        blending: AGBlending = AGBlending.NORMAL,
        uniforms: AGUniformValues = AGUniformValues.EMPTY,
        stencilRef: AGStencilReferenceState = AGStencilReferenceState.DEFAULT,
        stencilOpFunc: AGStencilOpFuncState = AGStencilOpFuncState.DEFAULT,
        colorMask: AGColorMaskState = AGColorMaskState.ALL_ENABLED,
        renderState: AGRenderState = AGRenderState.DEFAULT,
        scissor: AGRect = AGRect.NIL,
        instances: Int = 1
    ) = draw(batch.also { batch ->
        batch.vertices = vertices
        batch.program = program
        batch.type = type
        batch.vertexLayout = vertexLayout
        batch.vertexCount = vertexCount
        batch.indices = indices
        batch.indexType = indexType
        batch.offset = offset
        batch.blending = blending
        batch.uniforms = uniforms
        batch.stencilRef = stencilRef
        batch.stencilOpFunc = stencilOpFunc
        batch.colorMask = colorMask
        batch.renderState = renderState
        batch.scissor = scissor
        batch.instances = instances
    })

    fun drawV2(
        vertexData: FastArrayList<AGVertexData>,
        program: Program,
        type: AGDrawType,
        vertexCount: Int,
        indices: AGBuffer? = null,
        indexType: AGIndexType = AGIndexType.USHORT,
        offset: Int = 0,
        blending: AGBlending = AGBlending.NORMAL,
        uniforms: AGUniformValues = AGUniformValues.EMPTY,
        stencilRef: AGStencilReferenceState = AGStencilReferenceState.DEFAULT,
        stencilOpFunc: AGStencilOpFuncState = AGStencilOpFuncState.DEFAULT,
        colorMask: AGColorMaskState = AGColorMaskState.ALL_ENABLED,
        renderState: AGRenderState = AGRenderState.DEFAULT,
        scissor: AGRect = AGRect.NIL,
        instances: Int = 1
    ) = draw(batch.also { batch ->
        batch.vertexData = vertexData
        batch.program = program
        batch.type = type
        batch.vertexCount = vertexCount
        batch.indices = indices
        batch.indexType = indexType
        batch.offset = offset
        batch.blending = blending
        batch.uniforms = uniforms
        batch.stencilRef = stencilRef
        batch.stencilOpFunc = stencilOpFunc
        batch.colorMask = colorMask
        batch.renderState = renderState
        batch.scissor = scissor
        batch.instances = instances
    })

    private val batch = AGBatch()

    open fun draw(batch: AGBatch) {
        val instances = batch.instances
        val program = batch.program
        val type = batch.type
        val vertexCount = batch.vertexCount
        val indices = batch.indices
        val indexType = batch.indexType
        val offset = batch.offset
        val blending = batch.blending
        val uniforms = batch.uniforms
        val stencilRef = batch.stencilRef
        val stencilOpFunc = batch.stencilOpFunc
        val colorMask = batch.colorMask
        val renderState = batch.renderState
        val scissor = batch.scissor

        //println("SCISSOR: $scissor")

        //finalScissor.setTo(0, 0, backWidth, backHeight)

        commandsNoWait { list ->
            list.setScissorState(this, scissor)

            getProgram(program, config = when {
                uniforms.useExternalSampler() -> ProgramConfig.EXTERNAL_TEXTURE_SAMPLER
                else -> ProgramConfig.DEFAULT
            }).use(list)

            list.vertexArrayObjectSet(AGVertexArrayObject(batch.vertexData)) {
                list.uniformsSet(uniforms) {
                    list.setState(blending, stencilOpFunc, stencilRef, colorMask, renderState)

                    //val viewport = Buffer(4 * 4)
                    //gl.getIntegerv(KmlGl.VIEWPORT, viewport)
                    //println("viewport=${viewport.getAlignedInt32(0)},${viewport.getAlignedInt32(1)},${viewport.getAlignedInt32(2)},${viewport.getAlignedInt32(3)}")

                    list.draw(type, vertexCount, offset, instances, if (indices != null) indexType else AGIndexType.NONE, indices)
                }
            }
        }
    }

    open fun disposeTemporalPerFrameStuff() = Unit

    val frameRenderBuffers = LinkedHashSet<AGRenderBuffer>()
    val renderBuffers = Pool<AGRenderBuffer>() { createRenderBuffer() }

    object RenderBufferConsts {
        const val DEFAULT_INITIAL_WIDTH = 128
        const val DEFAULT_INITIAL_HEIGHT = 128
    }

    internal val allRenderBuffers = LinkedHashSet<AGBaseRenderBuffer>()
    private val renderBufferCount: Int get() = allRenderBuffers.size
    private val renderBuffersMemory: ByteUnits get() = ByteUnits.fromBytes(allRenderBuffers.sumOf { it.estimatedMemoryUsage.bytesLong })


    @KoragExperimental
    var agTarget = AGTarget.DISPLAY

    val mainRenderBuffer: AGBaseRenderBuffer by lazy {
        when (agTarget) {
            AGTarget.DISPLAY -> createMainRenderBuffer()
            AGTarget.OFFSCREEN -> createRenderBuffer()
        }
    }

    open fun createMainRenderBuffer(): AGBaseRenderBuffer = AGBaseRenderBufferImpl(this)

    internal fun setViewport(buffer: AGBaseRenderBuffer) {
        commandsNoWait { it.viewport(buffer.x, buffer.y, buffer.width, buffer.height) }
        //println("setViewport: ${buffer.x}, ${buffer.y}, ${buffer.width}, ${buffer.height}")
    }

    var lastRenderContextId = 0

    open fun createRenderBuffer(): AGRenderBuffer = AGFinalRenderBuffer(this)

    //open fun createRenderBuffer() = RenderBuffer()

    fun flip() {
        disposeTemporalPerFrameStuff()
        renderBuffers.free(frameRenderBuffers)
        if (frameRenderBuffers.isNotEmpty()) frameRenderBuffers.clear()
        flipInternal()
        commandsSync { it.finish() }
    }

    open fun flipInternal() = Unit

    open fun startFrame() {
        stats.startFrame()
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
        commandsNoWait { list ->
            //println("CLEAR: $color, $depth")
            list.setScissorState(this, scissor)
            //gl.disable(KmlGl.SCISSOR_TEST)
            if (clearColor) {
                list.colorMask(true, true, true, true)
                list.clearColor(color.rf, color.gf, color.bf, color.af)
            }
            if (clearDepth) {
                list.depthMask(true)
                list.clearDepth(depth)
            }
            if (clearStencil) {
                list.stencilMask(-1)
                list.clearStencil(stencil)
            }
            list.clear(clearColor, clearDepth, clearStencil)
        }
    }


    private val finalScissorBL = Rectangle()
    private val tempRect = Rectangle()

    fun clearStencil(stencil: Int = 0, scissor: AGRect = AGRect.NIL) = clear(clearColor = false, clearDepth = false, clearStencil = true, stencil = stencil, scissor = scissor)
    fun clearDepth(depth: Float = 1f, scissor: AGRect = AGRect.NIL) = clear(clearColor = false, clearDepth = true, clearStencil = false, depth = depth, scissor = scissor)
    fun clearColor(color: RGBA = Colors.TRANSPARENT_BLACK, scissor: AGRect = AGRect.NIL) = clear(clearColor = true, clearDepth = false, clearStencil = false, color = color, scissor = scissor)

    val renderBufferStack = FastArrayList<AGBaseRenderBuffer?>()

    //@PublishedApi
    @KoragExperimental
    var currentRenderBuffer: AGBaseRenderBuffer? = null
        private set

    val currentRenderBufferOrMain: AGBaseRenderBuffer get() = currentRenderBuffer ?: mainRenderBuffer

    val isRenderingToWindow: Boolean get() = currentRenderBufferOrMain === mainRenderBuffer
    val isRenderingToTexture: Boolean get() = !isRenderingToWindow

    @Deprecated("", ReplaceWith("isRenderingToTexture"))
    val renderingToTexture: Boolean get() = isRenderingToTexture

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

    inline fun setRenderBufferTemporally(rb: AGBaseRenderBuffer, callback: (AGBaseRenderBuffer) -> Unit) {
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

    inline fun tempAllocateFrameBuffer(width: Int, height: Int, hasDepth: Boolean = false, hasStencil: Boolean = true, msamples: Int = 1, block: (rb: AGRenderBuffer) -> Unit) {
        val rb = unsafeAllocateFrameRenderBuffer(width, height, hasDepth = hasDepth, hasStencil = hasStencil, msamples = msamples)
        try {
            block(rb)
        } finally {
            unsafeFreeFrameRenderBuffer(rb)
        }
    }

    inline fun tempAllocateFrameBuffers2(width: Int, height: Int, hasDepth: Boolean = false, hasStencil: Boolean = true, msamples: Int = 1, block: (rb0: AGRenderBuffer, rb1: AGRenderBuffer) -> Unit) {
        tempAllocateFrameBuffer(width, height, hasDepth, hasStencil, msamples) { rb0 ->
            tempAllocateFrameBuffer(width, height, hasDepth, hasStencil, msamples) { rb1 ->
                block(rb0, rb1)
            }
        }
    }

    @KoragExperimental
    fun unsafeAllocateFrameRenderBuffer(width: Int, height: Int, hasDepth: Boolean = false, hasStencil: Boolean = true, msamples: Int = 1, onlyThisFrame: Boolean = true): AGRenderBuffer {
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
    fun unsafeFreeFrameRenderBuffer(rb: AGRenderBuffer) {
        if (frameRenderBuffers.remove(rb)) {
        }
        renderBuffers.free(rb)
    }

    @OptIn(KoragExperimental::class)
    inline fun renderToTexture(
        width: Int, height: Int,
        render: (rb: AGRenderBuffer) -> Unit,
        hasDepth: Boolean = false, hasStencil: Boolean = false, msamples: Int = 1,
        use: (tex: AGTexture, texWidth: Int, texHeight: Int) -> Unit
    ) {
        commandsNoWait { list ->
            list.flush()
        }
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

    fun setRenderBuffer(renderBuffer: AGBaseRenderBuffer?): AGBaseRenderBuffer? {
        val old = currentRenderBuffer
        currentRenderBuffer?.unset()
        currentRenderBuffer = renderBuffer
        renderBuffer?.set()
        return old
    }

    fun getRenderBufferAtStackPoint(offset: Int): AGBaseRenderBuffer {
        if (offset == 0) return currentRenderBufferOrMain
        return renderBufferStack.getOrNull(renderBufferStack.size + offset) ?: mainRenderBuffer
    }

    fun pushRenderBuffer(renderBuffer: AGBaseRenderBuffer) {
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
        commandsSync { it.readPixels(x, y, bitmap.width, bitmap.height, bitmap.ints, AGReadKind.COLOR) }
    }
    open fun readDepth(width: Int, height: Int, out: FloatArray) {
        commandsSync { it.readPixels(0, 0, width, height, out, AGReadKind.DEPTH) }
    }
    open fun readStencil(bitmap: Bitmap8) {
        commandsSync { it.readPixels(0, 0, bitmap.width, bitmap.height, bitmap.data, AGReadKind.STENCIL) }
    }
    fun readDepth(out: FloatArray2): Unit = readDepth(out.width, out.height, out.data)
    open fun readColorTexture(texture: AGTexture, x: Int = 0, y: Int = 0, width: Int = backWidth, height: Int = backHeight): Unit = TODO()
    fun readColor(): Bitmap32 = Bitmap32(backWidth, backHeight, premultiplied = isRenderingToTexture).apply { readColor(this) }
    fun readDepth(): FloatArray2 = FloatArray2(backWidth, backHeight) { 0f }.apply { readDepth(this) }

    //////////////

    internal var programCount = 0
    // @TODO: Simplify this. Why do we need external? Maybe we could copy external textures into normal ones to avoid issues
    //private val programs = FastIdentityMap<Program, FastIdentityMap<ProgramConfig, AgProgram>>()
    //private val programs = HashMap<Program, FastIdentityMap<ProgramConfig, AgProgram>>()
    //private val normalPrograms = FastIdentityMap<Program, AgProgram>()
    //private val externalPrograms = FastIdentityMap<Program, AgProgram>()
    private val normalPrograms = HashMap<Program, AGProgram>()
    private val externalPrograms = HashMap<Program, AGProgram>()

    @JvmOverloads
    fun getProgram(program: Program, config: ProgramConfig = ProgramConfig.DEFAULT): AGProgram {
        val map = if (config.externalTextureSampler) externalPrograms else normalPrograms
        return map.getOrPut(program) { AGProgram(this, program, config) }
    }

    //////////////

    val textureDrawer by lazy { AGTextureDrawer(this) }
    val flipRenderTexture = true

    fun drawTexture(tex: AGTexture) {
        textureDrawer.draw(tex, -1f, +1f, +1f, -1f)
    }

    private val drawTempTexture: AGTexture by lazy { createTexture() }

    protected val _globalState = AGGlobalState(checked)
    var contextVersion: Int by _globalState::contextVersion
    @PublishedApi internal val _list = _globalState.createList()

    val multithreadedRendering: Boolean get() = false

    @OptIn(ExperimentalContracts::class)
    @Deprecated("Use commandsNoWait instead")
    inline fun <T> commands(block: (AGList) -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        return commandsNoWait(block)
    }

    /**
     * Queues commands, and wait for them to be executed synchronously
     */
    @OptIn(ExperimentalContracts::class)
    inline fun <T> commandsSync(block: (AGList) -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        val result = block(_list)
        if (multithreadedRendering) {
            runBlockingNoJs { _list.sync() }
        } else {
            _executeList(_list)
        }
        return result
    }

    /**
     * Queues commands without waiting
     */
    @OptIn(ExperimentalContracts::class)
    inline fun <T> commandsNoWait(block: (AGList) -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        val result = block(_list)
        if (!multithreadedRendering) _executeList(_list)
        return result
    }

    /**
     * Queues commands without waiting. In non-multithreaded mode, not even executing
     */
    @OptIn(ExperimentalContracts::class)
    inline fun <T> commandsNoWaitNoExecute(block: (AGList) -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        return block(_list)
    }

    /**
     * Queues commands and suspend until they are executed
     */
    @OptIn(ExperimentalContracts::class)
    suspend inline fun <T> commandsSuspend(block: (AGList) -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        val result = block(_list)
        if (!multithreadedRendering) {
            _executeList(_list)
        } else {
            _list.sync()
        }
        return result
    }

    @PublishedApi
    internal fun _executeList(list: AGList) = executeList(list)

    protected open fun executeList(list: AGList) {
    }

    fun drawBitmap(bmp: Bitmap) {
        drawTempTexture.upload(bmp, mipmaps = false)
        drawTexture(drawTempTexture)
        drawTempTexture.upload(Bitmaps.transparent)
    }

    protected val stats = AGStats()

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

fun AGUniformValues.useExternalSampler(): Boolean {
    var useExternalSampler = false
    this.fastForEach { uniform ->
        val uniformType = uniform.type
        when (uniformType) {
            VarType.Sampler2D -> {
                val unit = uniform.nativeValue?.fastCastTo<AGTextureUnit>()
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
