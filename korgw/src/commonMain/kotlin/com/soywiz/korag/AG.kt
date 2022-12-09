package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.kmem.unit.*
import com.soywiz.korag.annotation.*
import com.soywiz.korag.gl.*
import com.soywiz.korag.shader.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.annotations.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
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

class AGToCommandChannel(val channel: Channel<AGCommand>) : AG() {
    override fun execute(command: AGCommand) {
        channel.trySend(command)
    }
}

abstract class AG : AGFeatures, AGCommandExecutor, Extra by Extra.Mixin() {
    companion object {
        const val defaultPixelsPerInch : Double = 96.0
    }

    var contextVersion: Int = 0
        private set

    open fun contextLost() {
        Console.info("AG.contextLost()", this)
        contextVersion++
        //printStackTrace("AG.contextLost")
    }

    open fun beforeDoRender() {
    }

    fun resized(width: Int, height: Int) {
        resized(0, 0, width, height, width, height)
    }

    open fun resized(x: Int, y: Int, width: Int, height: Int, fullWidth: Int, fullHeight: Int) {
        mainFrameBuffer.setSize(x, y, width, height, fullWidth, fullHeight)
    }

    open fun dispose() {
    }

    // On MacOS components, this will be the size of the component
    open val backWidth: Int get() = mainFrameBuffer.width
    open val backHeight: Int get() = mainFrameBuffer.height

    // On MacOS components, this will be the full size of the window
    val realBackWidth get() = mainFrameBuffer.fullWidth
    val realBackHeight get() = mainFrameBuffer.fullHeight

    val currentWidth: Int get() = currentFrameBuffer?.width ?: mainFrameBuffer.width
    val currentHeight: Int get() = currentFrameBuffer?.height ?: mainFrameBuffer.height

    //protected fun setViewport(v: IntArray) = setViewport(v[0], v[1], v[2], v[3])

    var createdTextureCount = 0
    var deletedTextureCount = 0

    internal val textures = LinkedHashSet<AGTexture>()
    private val texturesCount: Int get() = textures.size
    private val texturesMemory: ByteUnits get() = ByteUnits.fromBytes(textures.sumOf { it.estimatedMemoryUsage.bytesLong })

    fun createTexture(): AGTexture = createTexture(premultiplied = true)
    fun createTexture(bmp: Bitmap, mipmaps: Boolean = false): AGTexture = createTexture(bmp.premultiplied).upload(bmp, mipmaps)
    @Deprecated("This will copy the data")
    fun createTexture(bmp: BitmapSlice<Bitmap>, mipmaps: Boolean = false): AGTexture = createTexture(bmp.premultiplied).upload(bmp, mipmaps)
    fun createTexture(bmp: Bitmap, mipmaps: Boolean = false, premultiplied: Boolean = true): AGTexture = createTexture(premultiplied).upload(bmp, mipmaps)
    open fun createTexture(premultiplied: Boolean, targetKind: AGTextureTargetKind = AGTextureTargetKind.TEXTURE_2D): AGTexture = AGTexture(premultiplied, targetKind)

    fun drawV2(
        frameBuffer: AGFrameBuffer,
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
        batch.frameBuffer = frameBuffer.base
        batch.frameBufferInfo = frameBuffer.info
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

    private val batch: AGBatch by lazy { AGBatch(mainFrameBuffer.base, mainFrameBuffer.info) }

    open fun blit(command: AGBlitPixels) {

    }

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

    internal val allFrameBuffers = LinkedHashSet<AGFrameBufferBase>()
    private val frameBufferCount: Int get() = allFrameBuffers.size
    private val frameBuffersMemory: ByteUnits get() = ByteUnits.fromBytes(allFrameBuffers.sumOf { it.estimatedMemoryUsage.bytesLong })

    val mainFrameBuffer: AGFrameBuffer by lazy {
        createMainFrameBuffer()
    }

    var lastRenderContextId = 0

    open fun createMainFrameBuffer(): AGFrameBuffer = AGFrameBuffer(AGFrameBufferBase(this, isMain = true))
    open fun createFrameBuffer(): AGFrameBuffer = AGFrameBuffer(AGFrameBufferBase(this, isMain = false))

    //open fun createRenderBuffer() = RenderBuffer()

    open fun flip() {
    }

    open fun startFrame() {
    }
    open fun endFrame() {
    }

    fun clear(
        frameBuffer: AGFrameBuffer,
        color: RGBA = Colors.TRANSPARENT_BLACK,
        depth: Float = 1f,
        stencil: Int = 0,
        clearColor: Boolean = true,
        clearDepth: Boolean = true,
        clearStencil: Boolean = true,
        scissor: AGScissor = AGScissor.NIL,
    ) {
        clear(frameBuffer.base, frameBuffer.info, color, depth, stencil, clearColor, clearDepth, clearStencil, scissor)
    }

    open fun clear(
        frameBuffer: AGFrameBufferBase,
        frameBufferInfo: AGFrameBufferInfo,
        color: RGBA = Colors.TRANSPARENT_BLACK,
        depth: Float = 1f,
        stencil: Int = 0,
        clearColor: Boolean = true,
        clearDepth: Boolean = true,
        clearStencil: Boolean = true,
        scissor: AGScissor = AGScissor.NIL,
    ) {
        execute(AGClear(frameBuffer, frameBufferInfo, color, depth, stencil, clearColor, clearDepth, clearStencil))
    }


    private val finalScissorBL = Rectangle()
    @PublishedApi internal val tempRect = Rectangle()

    open fun flush() {
    }

    //@PublishedApi
    var currentFrameBuffer: AGFrameBuffer? = null

    val currentFrameBufferOrMain: AGFrameBuffer get() = currentFrameBuffer ?: mainFrameBuffer

    val isRenderingToWindow: Boolean get() = currentFrameBufferOrMain === mainFrameBuffer
    val isRenderingToTexture: Boolean get() = !isRenderingToWindow

    inline fun backupTexture(frameBuffer: AGFrameBuffer, tex: AGTexture?, callback: () -> Unit) {
        if (tex != null) {
            readColorTexture(tex, 0, 0, backWidth, backHeight)
        }
        try {
            callback()
        } finally {
            if (tex != null) drawTexture(frameBuffer, tex)
        }
    }

    // @TODO: Rename to Sync and add Suspend versions
    fun readPixel(x: Int, y: Int): RGBA {
        val rawColor = Bitmap32(1, 1, premultiplied = isRenderingToTexture).also { readColor(it, x, y) }.ints[0]
        return if (isRenderingToTexture) RGBAPremultiplied(rawColor).depremultiplied else RGBA(rawColor)
    }

    open fun readColor(bitmap: Bitmap32, x: Int = 0, y: Int = 0) = Unit
    open fun readDepth(width: Int, height: Int, out: FloatArray) = Unit
    open fun readStencil(bitmap: Bitmap8) = Unit
    fun readDepth(out: FloatArray2): Unit = readDepth(out.width, out.height, out.data)
    open fun readColorTexture(texture: AGTexture, x: Int = 0, y: Int = 0, width: Int = backWidth, height: Int = backHeight): Unit = TODO()
    fun readColor(): Bitmap32 = Bitmap32(backWidth, backHeight, premultiplied = isRenderingToTexture).apply { readColor(this) }
    fun readDepth(): FloatArray2 = FloatArray2(backWidth, backHeight) { 0f }.apply { readDepth(this) }

    //////////////

    val textureDrawer by lazy { AGTextureDrawer(this) }
    fun drawTexture(frameBuffer: AGFrameBuffer, tex: AGTexture) {
        textureDrawer.draw(frameBuffer, tex, -1f, +1f, +1f, -1f)
    }

    private val drawTempTexture: AGTexture by lazy { createTexture() }

    fun drawBitmap(frameBuffer: AGFrameBuffer, bmp: Bitmap) {
        drawTempTexture.upload(bmp, mipmaps = false)
        drawTexture(frameBuffer, drawTempTexture)
        drawTempTexture.upload(Bitmaps.transparent.bmp)
    }

    private val stats = AGStats()

    internal var programCount = 0

    fun getStats(out: AGStats = stats): AGStats {
        readStats(out)
        return out
    }

    protected open fun readStats(out: AGStats) {
    }
}
