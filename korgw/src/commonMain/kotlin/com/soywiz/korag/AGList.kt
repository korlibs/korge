/**
 * Allows to enqueue GPU commands that will be processed by a different thread eventually.
 */
@file:Suppress("OPT_IN_IS_NOT_ENABLED")
@file:OptIn(com.soywiz.korio.annotations.KorIncomplete::class)

package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.kds.lock.*
import com.soywiz.kmem.*
import com.soywiz.korag.annotation.*
import com.soywiz.korag.shader.*
import com.soywiz.korio.annotations.*
import kotlinx.coroutines.*

typealias AGBlendEquation = AG.BlendEquation
typealias AGBlendFactor = AG.BlendFactor
typealias AGStencilOp = AG.StencilOp
typealias AGTriangleFace = AG.TriangleFace
typealias AGCompareMode = AG.CompareMode
typealias AGFrontFace = AG.FrontFace
typealias AGCullFace = AG.CullFace
typealias AGDrawType = AG.DrawType
typealias AGIndexType = AG.IndexType

@KorIncomplete
@KorInternal
interface AGQueueProcessor {
    fun finish()
    fun enableDisable(kind: AGEnable, enable: Boolean)
    fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean)
    fun blendEquation(rgb: AGBlendEquation, a: AGBlendEquation)
    fun blendFunction(srcRgb: AGBlendFactor, dstRgb: AGBlendFactor, srcA: AGBlendFactor = srcRgb, dstA: AGBlendFactor = dstRgb)
    fun cullFace(face: AGCullFace)
    fun frontFace(face: AGFrontFace)
    fun depthFunction(depthTest: AGCompareMode)
    fun programCreate(programId: Int, program: Program)
    fun programDelete(programId: Int)
    fun programUse(programId: Int)
    fun draw(type: AGDrawType, vertexCount: Int, offset: Int = 0, instances: Int = 1, indexType: AGIndexType? = null)
    fun uniformsSet(layout: UniformLayout, data: FBuffer)
    fun depthMask(depth: Boolean)
    fun depthRange(near: Float, far: Float)
    fun stencilFunction(compareMode: AG.CompareMode, referenceValue: Int, readMask: Int)
    fun stencilOperation(actionOnDepthFail: AG.StencilOp, actionOnDepthPassStencilFail: AG.StencilOp, actionOnBothPass: AG.StencilOp)
    fun stencilMask(writeMask: Int)
    fun scissor(x: Int, y: Int, width: Int, height: Int)
    fun clear(color: Boolean, depth: Boolean, stencil: Boolean)
    fun clearColor(red: Float, green: Float, blue: Float, alpha: Float)
    fun clearDepth(depth: Float)
    fun clearStencil(stencil: Int)
}

@KorInternal
inline fun AGQueueProcessor.processBlocking(list: AGList, maxCount: Int = 1) {
    with(list) {
        processBlocking(maxCount)
    }
}

@KorInternal
inline fun AGQueueProcessor.processBlockingAll(list: AGList) = processBlocking(list, -1)

enum class AGEnable {
    BLEND, CULL_FACE, DEPTH, SCISSOR, STENCIL;
    companion object {
        val VALUES = values()
    }
}

@KorIncomplete
class AGGlobalState {
    internal val programIndices = ConcurrentPool { it }
    internal val textureIndices = ConcurrentPool { it }
    //var programIndex = KorAtomicInt(0)
    private val lock = Lock()
    private val lists = Deque<AGList>()

    // For example if we want to wait for pixels to be read
    suspend fun waitProcessed(list: AGList) {
        list.completed.await()
    }

    fun enqueue(list: AGList) {
        lock { lists.add(list) }
    }

    fun createList(): AGList = AGList(this)
}

@KorIncomplete
class AGList(val globalState: AGGlobalState) {
    internal val completed = CompletableDeferred<Unit>()
    private val _lock = Lock() // @TODO: This is slow!
    private val _data = IntDeque(128)
    private val _ints = IntDeque(16)
    private val _float = FloatDeque(16)
    private val _extra = Deque<Any?>(16)

    private fun addExtra(v0: Any?) { _lock { _extra.add(v0) } }
    private fun addExtra(v0: Any?, v1: Any?) { _lock { _extra.add(v0); _extra.add(v1) } }

    private fun addFloat(v0: Float) { _lock { _float.add(v0) } }
    private fun addFloat(v0: Float, v1: Float) { _lock { _float.add(v0); _float.add(v1) } }
    private fun addFloat(v0: Float, v1: Float, v2: Float) { _lock { _float.add(v0); _float.add(v1); _float.add(v2) } }
    private fun addFloat(v0: Float, v1: Float, v2: Float, v3: Float) { _lock { _float.add(v0); _float.add(v1); _float.add(v2); _float.add(v3) } }

    private fun addInt(v0: Int) { _lock { _ints.add(v0) } }
    private fun addInt(v0: Int, v1: Int) { _lock { _ints.add(v0); _ints.add(v1) } }
    private fun addInt(v0: Int, v1: Int, v2: Int) { _lock { _ints.add(v0); _ints.add(v1); _ints.add(v2) } }
    private fun addInt(v0: Int, v1: Int, v2: Int, v3: Int) { _lock { _ints.add(v0); _ints.add(v1); _ints.add(v2); _ints.add(v3) } }

    private fun add(v0: Int) { _lock { _data.add(v0) } }
    private fun <T> readExtra(): T = _lock { _extra.removeFirst() }.fastCastTo()
    private fun readFloat(): Float = _lock { _float.removeFirst() }
    private fun readInt(): Int = _lock { _ints.removeFirst() }
    private fun read(): Int = _lock { _data.removeFirst() }

    @KorInternal
    fun AGQueueProcessor.processBlocking(maxCount: Int = 1): Boolean {
        var pending = maxCount
        val processor = this@processBlocking
        while (true) {
            if (pending-- == 0) break
            // @TODO: Wait for more data
            if (_data.size < 1) break
            val data = read()
            val cmd = data.extract8(24)
            when (cmd) {
                CMD_FINISH -> {
                    processor.finish()
                    completed.complete(Unit)
                    return true
                }
                CMD_DEPTH_FUNCTION -> processor.depthFunction(AGCompareMode.VALUES[data.extract4(0)])
                CMD_ENABLE -> processor.enableDisable(AGEnable.VALUES[data.extract4(0)], enable = true)
                CMD_DISABLE -> processor.enableDisable(AGEnable.VALUES[data.extract4(0)], enable = false)
                CMD_COLOR_MASK -> processor.colorMask(data.extract(0), data.extract(1), data.extract(2), data.extract(3))
                CMD_BLEND_EQ -> processor.blendEquation(
                    AGBlendEquation.VALUES[data.extract4(0)], AGBlendEquation.VALUES[data.extract4(4)]
                )
                CMD_BLEND_FUNC -> processor.blendFunction(
                    AGBlendFactor.VALUES[data.extract4(0)],
                    AGBlendFactor.VALUES[data.extract4(4)],
                    AGBlendFactor.VALUES[data.extract4(8)],
                    AGBlendFactor.VALUES[data.extract4(12)],
                )
                CMD_CULL_FACE -> processor.cullFace(AGCullFace.VALUES[data.extract4(0)])
                CMD_FRONT_FACE -> processor.frontFace(AGFrontFace.VALUES[data.extract4(0)])
                // Programs
                CMD_PROGRAM_CREATE -> processor.programCreate(data.extract16(0), _extra.removeFirst().fastCastTo())
                CMD_PROGRAM_DELETE -> processor.programDelete(data.extract16(0))
                CMD_PROGRAM_USE -> processor.programUse(data.extract16(0))
                // Draw
                CMD_DRAW -> processor.draw(
                    AGDrawType.VALUES[data.extract4(0)],
                    readInt(), readInt(), readInt(),
                    AGIndexType.VALUES.getOrNull(data.extract4(4)),
                )
                // Uniforms
                CMD_UNIFORMS_SET -> processor.uniformsSet(readExtra(), readExtra())
                CMD_DEPTH_MASK -> processor.depthMask(data.extract(0))
                CMD_DEPTH_RANGE -> processor.depthRange(readFloat(), readFloat())
                CMD_SYNC -> readExtra<CompletableDeferred<Unit>>().complete(Unit)
                CMD_STENCIL_FUNC -> processor.stencilFunction(
                    AGCompareMode.VALUES[data.extract4(0)],
                    data.extract8(8),
                    data.extract8(16),
                )
                CMD_STENCIL_OP -> processor.stencilOperation(
                    AG.StencilOp.VALUES[data.extract4(0)],
                    AG.StencilOp.VALUES[data.extract4(4)],
                    AG.StencilOp.VALUES[data.extract4(8)],
                )
                //CMD_STENCIL_MASK -> processor.stencilMask(data.extract8(0))
                CMD_STENCIL_MASK -> processor.stencilMask(readInt())
                CMD_SCISSOR -> processor.scissor(readInt(), readInt(), readInt(), readInt())
                CMD_CLEAR -> processor.clear(data.extract(0), data.extract(1), data.extract(2))
                CMD_CLEAR_COLOR -> processor.clearColor(readFloat(), readFloat(), readFloat(), readFloat())
                CMD_CLEAR_DEPTH -> processor.clearDepth(readFloat())
                CMD_CLEAR_STENCIL -> processor.clearStencil(readInt())
                else -> TODO("Unknown AG command $cmd")
            }
        }
        return false
    }

    fun enable(kind: AGEnable): Unit = add(CMD(CMD_ENABLE).finsert4(kind.ordinal, 0))
    fun disable(kind: AGEnable): Unit = add(CMD(CMD_DISABLE).finsert4(kind.ordinal, 0))

    fun sync(deferred: CompletableDeferred<Unit>) {
        addExtra(deferred)
        add(CMD(CMD_SYNC))
    }

    @KoragExperimental
    suspend fun sync() {
        // @TODO: Will only work if we are processing stuff in another thread
        val deferred = CompletableDeferred<Unit>()
        sync(deferred)
        deferred.await()
    }

    inline fun enableDisable(kind: AGEnable, enable: Boolean, block: () -> Unit = {}): Unit {
        if (enable) {
            enable(kind)
            block()
        } else {
            disable(kind)
        }
    }

    fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {
        add(CMD(CMD_COLOR_MASK).finsert(red, 0).finsert(green, 1).finsert(blue, 2).finsert(alpha, 3))
    }

    fun depthMask(depth: Boolean) {
        add(CMD(CMD_DEPTH_MASK).finsert(depth, 0))
    }

    fun clearColor(red: Float, green: Float, blue: Float, alpha: Float) {
        addFloat(red, green, blue, alpha)
        add(CMD(CMD_CLEAR_COLOR))
    }

    fun clearDepth(depth: Float) {
        addFloat(depth)
        add(CMD(CMD_CLEAR_DEPTH))
    }

    fun clearStencil(stencil: Int) {
        addInt(stencil)
        add(CMD(CMD_CLEAR_STENCIL))
    }

    fun clear(color: Boolean, depth: Boolean, stencil: Boolean) {
        add(CMD(CMD_CLEAR).finsert(color, 0).finsert(depth, 1).finsert(stencil, 2))
    }

    fun depthRange(near: Float, far: Float) {
        addFloat(near, far)
        add(CMD(CMD_DEPTH_RANGE))
    }

    fun blendEquation(rgb: AGBlendEquation, a: AGBlendEquation = rgb) {
        add(CMD(CMD_BLEND_EQ).finsert4(rgb.ordinal, 0).finsert4(a.ordinal, 4))
    }

    fun blendFunction(srcRgb: AGBlendFactor, dstRgb: AGBlendFactor, srcA: AGBlendFactor = srcRgb, dstA: AGBlendFactor = dstRgb) {
        add(CMD(CMD_BLEND_FUNC)
            .finsert4(srcRgb.ordinal, 0).finsert4(dstRgb.ordinal, 4)
            .finsert4(srcA.ordinal, 8).finsert4(dstA.ordinal, 12)
        )
    }

    fun cullFace(face: AGCullFace) {
        add(CMD(CMD_CULL_FACE).finsert4(face.ordinal, 0))
    }

    fun frontFace(face: AGFrontFace) {
        add(CMD(CMD_FRONT_FACE).finsert4(face.ordinal, 0))
    }

    fun scissor(x: Int, y: Int, width: Int, height: Int) {
        addInt(x, y, width, height)
        add(CMD(CMD_SCISSOR))
    }

    fun finish() {
        add(CMD(CMD_FINISH))
    }

    fun depthFunction(depthTest: AGCompareMode) {
        add(CMD(CMD_DEPTH_FUNCTION).finsert4(depthTest.ordinal, 0))
    }

    ////////////////////////////////////////
    // PROGRAMS
    ////////////////////////////////////////

    fun createProgram(program: Program): Int {
        val programId = globalState.programIndices.alloc()
        addExtra(program)
        add(CMD(CMD_PROGRAM_CREATE).finsert16(programId, 0))
        return programId
    }

    fun deleteProgram(programId: Int) {
        globalState.programIndices.free(programId)
        add(CMD(CMD_PROGRAM_DELETE).finsert16(programId, 0))
    }

    fun useProgram(programId: Int) {
        add(CMD(CMD_PROGRAM_USE).finsert16(programId, 0))
    }

    ////////////////////////////////////////
    // TEXTURES
    ////////////////////////////////////////

    fun createTexture(): Int {
        val textureId = globalState.textureIndices.alloc()
        add(CMD(CMD_TEXTURE_CREATE).finsert16(textureId, 0))
        return textureId
    }

    fun deleteTexture(textureId: Int) {
        globalState.programIndices.free(textureId)
        add(CMD(CMD_TEXTURE_DELETE).finsert16(textureId, 0))
    }

    fun updateTexture(textureId: Int, data: Any?, width: Int, height: Int) {
        addExtra(data)
        addInt(width, height)
        add(CMD(CMD_TEXTURE_UPDATE).finsert16(textureId, 0))
    }

    fun bindTexture(textureId: Int) {
        add(CMD(CMD_TEXTURE_BIND).finsert16(textureId, 0))
    }

    ////////////////////////////////////////
    // UNIFORMS
    ////////////////////////////////////////

    fun uniformsSet(layout: UniformLayout, data: FBuffer) {
        addExtra(layout, data)
        add(CMD(CMD_UNIFORMS_SET))
    }

    ////////////////////////////////////////
    // DRAW
    ////////////////////////////////////////

    fun draw(type: AGDrawType, vertexCount: Int, offset: Int = 0, instances: Int = 1, indexType: AGIndexType? = null) {
        addInt(vertexCount, offset, instances)
        add(CMD(CMD_DRAW).finsert4(type.ordinal, 0).finsert4(indexType?.ordinal ?: 0xF, 4))
    }

    fun stencilFunction(compareMode: AG.CompareMode, referenceValue: Int, readMask: Int) {
        add(CMD(CMD_STENCIL_FUNC).finsert4(compareMode.ordinal, 0).finsert8(referenceValue, 8).finsert8(readMask, 16))
    }

    fun stencilOperation(actionOnDepthFail: AG.StencilOp, actionOnDepthPassStencilFail: AG.StencilOp, actionOnBothPass: AG.StencilOp) {
        add(CMD(CMD_STENCIL_OP).finsert4(actionOnDepthFail.ordinal, 0).finsert4(actionOnDepthPassStencilFail.ordinal, 4).finsert8(actionOnBothPass.ordinal, 8))
    }

    fun stencilMask(writeMask: Int) {
        addInt(writeMask)
        add(CMD(CMD_STENCIL_MASK))
        //add(CMD(CMD_STENCIL_MASK).finsert8(writeMask, 0))
    }

    companion object {
        private fun CMD(cmd: Int): Int = 0.finsert8(cmd, 24)

        // Special
        private const val CMD_DRAW = 0xFD
        private const val CMD_SYNC = 0xFE
        private const val CMD_FINISH = 0xFF
        // General
        private const val CMD_NOOP = 0x00
        private const val CMD_ENABLE = 0x01
        private const val CMD_DISABLE = 0x02
        private const val CMD_COLOR_MASK = 0x03
        private const val CMD_BLEND_EQ = 0x04
        private const val CMD_BLEND_FUNC = 0x05
        private const val CMD_CULL_FACE = 0x06
        private const val CMD_FRONT_FACE = 0x07
        private const val CMD_DEPTH_FUNCTION = 0x08
        private const val CMD_DEPTH_MASK = 0x09
        private const val CMD_DEPTH_RANGE = 0x0A
        private const val CMD_SCISSOR = 0x0B
        // Clear
        private const val CMD_CLEAR = 0x10
        private const val CMD_CLEAR_COLOR = 0x11
        private const val CMD_CLEAR_DEPTH = 0x12
        private const val CMD_CLEAR_STENCIL = 0x13

        // Programs
        private const val CMD_PROGRAM_CREATE = 0x30
        private const val CMD_PROGRAM_DELETE = 0x31
        private const val CMD_PROGRAM_USE = 0x32
        // Textures
        private const val CMD_TEXTURE_CREATE = 0x40
        private const val CMD_TEXTURE_DELETE = 0x41
        private const val CMD_TEXTURE_UPDATE = 0x42
        private const val CMD_TEXTURE_BIND = 0x43
        // Uniform
        private const val CMD_UNIFORMS_SET = 0x50
        // Attributes
        private const val CMD_ATTRIBUTE_SET = 0x60
        // Render Buffer
        private const val CMD_RENDERBUFFER_CREATE = 0x70
        private const val CMD_RENDERBUFFER_FREE = 0x71
        private const val CMD_RENDERBUFFER_SET = 0x72
        private const val CMD_RENDERBUFFER_USE = 0x73
        private const val CMD_RENDERBUFFER_READ_PIXELS = 0x74
        // Stencil
        private const val CMD_STENCIL_FUNC = 0x80
        private const val CMD_STENCIL_OP = 0x81
        private const val CMD_STENCIL_MASK = 0x82
    }
}

@KorIncomplete fun AGList.enableBlend(): Unit = enable(AGEnable.BLEND)
@KorIncomplete fun AGList.enableCullFace(): Unit = enable(AGEnable.CULL_FACE)
@KorIncomplete fun AGList.enableDepth(): Unit = enable(AGEnable.DEPTH)
@KorIncomplete fun AGList.enableScissor(): Unit = enable(AGEnable.SCISSOR)
@KorIncomplete fun AGList.enableStencil(): Unit = enable(AGEnable.STENCIL)
@KorIncomplete fun AGList.disableBlend(): Unit = disable(AGEnable.BLEND)
@KorIncomplete fun AGList.disableCullFace(): Unit = disable(AGEnable.CULL_FACE)
@KorIncomplete fun AGList.disableDepth(): Unit = disable(AGEnable.DEPTH)
@KorIncomplete fun AGList.disableScissor(): Unit = disable(AGEnable.SCISSOR)
@KorIncomplete fun AGList.disableStencil(): Unit = disable(AGEnable.STENCIL)

