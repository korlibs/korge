package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.klogger.*
import com.soywiz.kmem.unit.*
import com.soywiz.korag.shader.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.channels.*

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

    val mainFrameBuffer: AGFrameBuffer = AGFrameBuffer(isMain = true)

    var lastRenderContextId = 0

    open fun flip() {
    }

    open fun startFrame() {
    }
    open fun endFrame() {
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

    open fun readToTexture(frameBuffer: AGFrameBufferBase, frameBufferInfo: AGFrameBufferInfo, texture: AGTexture, x: Int, y: Int, width: Int, height: Int) {
    }

    open fun readToMemory(frameBuffer: AGFrameBufferBase, frameBufferInfo: AGFrameBufferInfo, x: Int, y: Int, width: Int, height: Int, data: Any, kind: AGReadKind) {
    }

    private val stats = AGStats()
    fun getStats(out: AGStats = stats): AGStats {
        readStats(out)
        return out
    }
    protected open fun readStats(out: AGStats) {
    }
}

fun AG.clear(
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

fun AG.readPixel(frameBuffer: AGFrameBuffer, x: Int, y: Int): RGBA {
    val rawColor = Bitmap32(1, 1, premultiplied = frameBuffer.isTexture).also { readColor(frameBuffer, it, x, y) }.ints[0]
    return if (frameBuffer.isTexture) RGBAPremultiplied(rawColor).depremultiplied else RGBA(rawColor)
}

fun AG.readColor(frameBuffer: AGFrameBuffer, bitmap: Bitmap32, x: Int = 0, y: Int = 0) {
    readToMemory(frameBuffer.base, frameBuffer.info, x, y, bitmap.width, bitmap.height, bitmap.ints, AGReadKind.COLOR)
}
fun AG.readDepth(frameBuffer: AGFrameBuffer, width: Int, height: Int, out: FloatArray) {
    readToMemory(frameBuffer.base, frameBuffer.info, 0, 0, width, height, out, AGReadKind.DEPTH)
}
fun AG.readStencil(frameBuffer: AGFrameBuffer, bitmap: Bitmap8) {
    readToMemory(frameBuffer.base, frameBuffer.info, 0, 0, bitmap.width, bitmap.height, bitmap.data, AGReadKind.STENCIL)
}
fun AG.readDepth(frameBuffer: AGFrameBuffer, out: FloatArray2): Unit = readDepth(frameBuffer, out.width, out.height, out.data)
fun AG.readToTexture(frameBuffer: AGFrameBuffer, texture: AGTexture, x: Int = 0, y: Int = 0, width: Int = frameBuffer.width, height: Int = frameBuffer.height): Unit = TODO()
fun AG.readColor(frameBuffer: AGFrameBuffer): Bitmap32 = Bitmap32(frameBuffer.width, frameBuffer.height, premultiplied = frameBuffer.isTexture).apply { readColor(frameBuffer, this) }
fun AG.readDepth(frameBuffer: AGFrameBuffer): FloatArray2 = FloatArray2(frameBuffer.width, frameBuffer.height) { 0f }.apply { readDepth(frameBuffer, this) }
