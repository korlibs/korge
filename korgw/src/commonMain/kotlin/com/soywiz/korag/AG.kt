package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.klogger.*
import com.soywiz.korag.shader.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
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

abstract class AG : AGFeatures, Extra by Extra.Mixin() {
    companion object {
        const val defaultPixelsPerInch : Double = 96.0
    }

    val mainFrameBuffer: AGFrameBuffer = AGFrameBuffer(isMain = true)
    var contextVersion: Int = 0
        private set

    open fun contextLost() {
        Console.info("AG.contextLost()", this)
        contextVersion++
        //printStackTrace("AG.contextLost")
    }

    // @TODO: Unify beforeDoRender, startFrame  ---  dispose, finish & endFrame
    open fun beforeDoRender() = Unit
    open fun dispose() = Unit
    open fun finish() = execute(AGFinish)
    open fun startFrame() = Unit
    open fun endFrame() = Unit

    protected open fun execute(command: AGCommand) = Unit

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
    ) = execute(AGClear(frameBuffer, frameBufferInfo, color, depth, stencil, clearColor, clearDepth, clearStencil))

    open fun draw(
        frameBuffer: AGFrameBufferBase,
        frameBufferInfo: AGFrameBufferInfo,
        vertexData: AGVertexArrayObject,
        program: Program,
        drawType: AGDrawType,
        vertexCount: Int,
        indices: AGBuffer? = null,
        indexType: AGIndexType = AGIndexType.USHORT,
        drawOffset: Int = 0,
        blending: AGBlending = AGBlending.NORMAL,
        uniforms: AGUniformValues = AGUniformValues.EMPTY,
        stencilRef: AGStencilReference = AGStencilReference.DEFAULT,
        stencilOpFunc: AGStencilOpFunc = AGStencilOpFunc.DEFAULT,
        colorMask: AGColorMask = AGColorMask.ALL_ENABLED,
        depthAndFrontFace: AGDepthAndFrontFace = AGDepthAndFrontFace.DEFAULT,
        scissor: AGScissor = AGScissor.NIL,
        cullFace: AGCullFace = AGCullFace.NONE,
        instances: Int = 1
    ) = execute(AGBatch(frameBuffer, frameBufferInfo, vertexData, indices, indexType, program, uniforms, blending, stencilOpFunc, stencilRef, colorMask, depthAndFrontFace, scissor, cullFace, drawType, drawOffset, vertexCount, instances))

    open fun readToTexture(frameBuffer: AGFrameBufferBase, frameBufferInfo: AGFrameBufferInfo, texture: AGTexture, x: Int, y: Int, width: Int, height: Int): Unit = Unit
    open fun readToMemory(frameBuffer: AGFrameBufferBase, frameBufferInfo: AGFrameBufferInfo, x: Int, y: Int, width: Int, height: Int, data: Any, kind: AGReadKind): Unit = Unit
    protected open fun readStats(out: AGStats) = Unit

    private val stats = AGStats()
    fun getStats(out: AGStats = stats): AGStats = out.also { readStats(it) }
}

// @TODO: Reuse objects
class AGToCommandChannel(val channel: SendChannel<AGCommand>) : AG() {
    override fun execute(command: AGCommand) {
        channel.trySend(command)
    }
}

suspend fun AG.executeUntilFinish(flow: ReceiveChannel<AGCommand>) {
    while (true) {
        val command = flow.receive()
        command.execute(this)
        if (command is AGFinish) break
    }
}

fun AG.execute(command: AGCommand) = command.execute(this)
fun AG.draw(batch: AGBatch) = batch.execute(this)
fun AG.draw(batch: AGMultiBatch) = batch.execute(this)

fun AG.draw(
    frameBuffer: AGFrameBuffer,
    vertexData: AGVertexArrayObject,
    program: Program,
    drawType: AGDrawType,
    vertexCount: Int,
    indices: AGBuffer? = null,
    indexType: AGIndexType = AGIndexType.USHORT,
    drawOffset: Int = 0,
    blending: AGBlending = AGBlending.NORMAL,
    uniforms: AGUniformValues = AGUniformValues.EMPTY,
    stencilRef: AGStencilReference = AGStencilReference.DEFAULT,
    stencilOpFunc: AGStencilOpFunc = AGStencilOpFunc.DEFAULT,
    colorMask: AGColorMask = AGColorMask.ALL_ENABLED,
    depthAndFrontFace: AGDepthAndFrontFace = AGDepthAndFrontFace.DEFAULT,
    scissor: AGScissor = AGScissor.NIL,
    cullFace: AGCullFace = AGCullFace.NONE,
    instances: Int = 1
): Unit = this.draw(
    frameBuffer.base, frameBuffer.info, vertexData, program, drawType, vertexCount, indices, indexType, drawOffset, blending, uniforms, stencilRef, stencilOpFunc, colorMask, depthAndFrontFace, scissor, cullFace, instances
)

fun AG.clear(
    frameBuffer: AGFrameBuffer,
    color: RGBA = Colors.TRANSPARENT_BLACK,
    depth: Float = 1f,
    stencil: Int = 0,
    clearColor: Boolean = true,
    clearDepth: Boolean = true,
    clearStencil: Boolean = true,
    scissor: AGScissor = AGScissor.NIL,
) = clear(frameBuffer.base, frameBuffer.info, color, depth, stencil, clearColor, clearDepth, clearStencil, scissor)

fun AG.readPixel(frameBuffer: AGFrameBuffer, x: Int, y: Int): RGBA {
    val rawColor = Bitmap32(1, 1, premultiplied = frameBuffer.isTexture).also { readColor(frameBuffer, it, x, y) }.ints[0]
    return if (frameBuffer.isTexture) RGBAPremultiplied(rawColor).depremultiplied else RGBA(rawColor)
}

fun AG.readColor(frameBuffer: AGFrameBuffer, bitmap: Bitmap32, x: Int = 0, y: Int = 0) = readToMemory(frameBuffer.base, frameBuffer.info, x, y, bitmap.width, bitmap.height, bitmap.ints, AGReadKind.COLOR)
fun AG.readDepth(frameBuffer: AGFrameBuffer, width: Int, height: Int, out: FloatArray) = readToMemory(frameBuffer.base, frameBuffer.info, 0, 0, width, height, out, AGReadKind.DEPTH)
fun AG.readStencil(frameBuffer: AGFrameBuffer, bitmap: Bitmap8) = readToMemory(frameBuffer.base, frameBuffer.info, 0, 0, bitmap.width, bitmap.height, bitmap.data, AGReadKind.STENCIL)
fun AG.readDepth(frameBuffer: AGFrameBuffer, out: FloatArray2): Unit = readDepth(frameBuffer, out.width, out.height, out.data)
fun AG.readToTexture(frameBuffer: AGFrameBuffer, texture: AGTexture, x: Int = 0, y: Int = 0, width: Int = frameBuffer.width, height: Int = frameBuffer.height): Unit = readToTexture(frameBuffer.base, frameBuffer.info, texture, x, y, width, height)
fun AG.readColor(frameBuffer: AGFrameBuffer): Bitmap32 = Bitmap32(frameBuffer.width, frameBuffer.height, premultiplied = frameBuffer.isTexture).apply { readColor(frameBuffer, this) }
fun AG.readDepth(frameBuffer: AGFrameBuffer): FloatArray2 = FloatArray2(frameBuffer.width, frameBuffer.height) { 0f }.apply { readDepth(frameBuffer, this) }
