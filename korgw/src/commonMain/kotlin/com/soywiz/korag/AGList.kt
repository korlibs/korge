/**
 * Allows to enqueue GPU commands that will be processed by a different thread eventually.
 */
@file:Suppress("OPT_IN_IS_NOT_ENABLED")
@file:OptIn(com.soywiz.korio.annotations.KorIncomplete::class)

package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.kds.lock.*
import com.soywiz.kmem.*
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

@KorIncomplete
@KorInternal
interface AGQueueProcessor {
    fun enableDisable(kind: AGEnable, enable: Boolean)
    fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean)
    fun blendEquation(rgb: AGBlendEquation, a: AGBlendEquation)
    fun blendFunction(srcRgb: AGBlendFactor, dstRgb: AGBlendFactor, srcA: AGBlendFactor = srcRgb, dstA: AGBlendFactor = dstRgb)
    fun cullFace(face: AGCullFace)
    fun depthFunction(depthTest: AGCompareMode)
}

@KorInternal
inline fun AGQueueProcessor.processBlocking(list: AGList, maxCount: Int = 1) {
    with(list) {
        processBlocking(maxCount)
    }
}

@KorIncomplete
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
    private val _lock = Lock()
    private val _data = IntDeque()
    private val _extra = Deque<Any?>()
    private var dataX: Int = 0
    private var dataY: Int = 0

    private fun addExtra(value: Any?) { _lock { _extra.add(value) } }
    private fun add(value: Int) { _lock { _data.add(value) } }
    private fun add(v0: Int, v1: Int) { _lock { _data.add(v0); _data.add(v1) } }
    private fun add(v0: Int, v1: Int, v2: Int) { _lock { _data.add(v0); _data.add(v1); _data.add(v2) } }
    private fun read(): Int = _lock { _data.removeFirst() }

    @KorInternal
    fun AGQueueProcessor.processBlocking(maxCount: Int = 1): Boolean {
        var pending = maxCount
        while (true) {
            if (pending-- == 0) break
            // @TODO: Wait for more data
            if (_data.size < 1) break
            val data = read()
            val cmd = data.extract8(24)
            when (cmd) {
                CMD_FINISH -> {
                    completed.complete(Unit)
                    return true
                }
                CMD_DEPTH_FUNCTION -> {
                    depthFunction(AGCompareMode.VALUES[data.extract4(0)])
                }
                CMD_ENABLE -> enableDisable(AGEnable.VALUES[data.extract4(0)], enable = true)
                CMD_DISABLE -> enableDisable(AGEnable.VALUES[data.extract4(0)], enable = false)
                CMD_COLOR_MASK -> colorMask(data.extract(0), data.extract(1), data.extract(2), data.extract(3))
                CMD_BLEND_EQ -> blendEquation(
                    AGBlendEquation.VALUES[data.extract4(0)],
                    AGBlendEquation.VALUES[data.extract4(4)]
                )
                CMD_BLEND_FUNC -> blendFunction(
                    AGBlendFactor.VALUES[data.extract4(0)],
                    AGBlendFactor.VALUES[data.extract4(4)]
                )
                CMD_CULL_FACE -> cullFace(
                    AGFrontFace.VALUES[data.extract4(0)]
                )
                CMD_DATA_X -> dataX = data and 0xFFFFFF
                CMD_DATA_Y -> dataY = data and 0xFFFFFF
                else -> TODO("Unknown AG command $cmd")
            }
        }
        return false
    }

    fun enable(kind: AGEnable): Unit = add(CMD(CMD_ENABLE).finsert4(kind.ordinal, 0))
    fun disable(kind: AGEnable): Unit = add(CMD(CMD_DISABLE).finsert4(kind.ordinal, 0))

    fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {
        add(CMD(CMD_COLOR_MASK).finsert(red, 0).finsert(green, 1).finsert(blue, 2).finsert(alpha, 3))
    }

    fun blendEquation(rgb: AGBlendEquation, a: AGBlendEquation = rgb) {
        add(CMD(CMD_BLEND_EQ).finsert4(rgb.ordinal, 0).finsert4(a.ordinal, 4))
    }

    fun blendFunction(rgb: AGBlendFactor, a: AGBlendFactor = rgb) {
        add(CMD(CMD_BLEND_FUNC).finsert4(rgb.ordinal, 0).finsert4(a.ordinal, 4))
    }

    fun cullFace(frontFace: AGFrontFace) {
        add(CMD(CMD_CULL_FACE).finsert4(frontFace.ordinal, 0))
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
        add(
            CMD(CMD_DATA_X).finsert24(width, 0),
            CMD(CMD_DATA_Y).finsert24(height, 0),
            CMD(CMD_TEXTURE_UPDATE).finsert16(textureId, 0)
        )
    }

    fun bindTexture(textureId: Int) {
        add(CMD(CMD_TEXTURE_BIND).finsert16(textureId, 0))
    }

    ////////////////////////////////////////
    // UNIFORMS
    ////////////////////////////////////////

    fun updateUniform(uniform: Uniform, value: Any?) {
        addExtra(value)
        TODO()
    }

    companion object {
        private fun CMD(cmd: Int): Int = 0.finsert8(cmd, 24)

        // Special
        private const val CMD_FINISH = 0xFF
        // General
        private const val CMD_NOOP = 0x00
        private const val CMD_ENABLE = 0x01
        private const val CMD_DISABLE = 0x02
        private const val CMD_COLOR_MASK = 0x03
        private const val CMD_BLEND_EQ = 0x04
        private const val CMD_BLEND_FUNC = 0x05
        private const val CMD_CULL_FACE = 0x06
        private const val CMD_DEPTH_FUNCTION = 0x07
        private const val CMD_DATA_X = 0x08
        private const val CMD_DATA_Y = 0x09
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
        private const val CMD_UNIFORM_SET = 0x50
        // Attributes
        private const val CMD_ATTRIBUTE_SET = 0x60
        // Render Buffer
        private const val CMD_RENDERBUFFER_CREATE = 0x70
        private const val CMD_RENDERBUFFER_FREE = 0x71
        private const val CMD_RENDERBUFFER_SET = 0x72
        private const val CMD_RENDERBUFFER_USE = 0x73
        private const val CMD_RENDERBUFFER_READ_PIXELS = 0x74
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

