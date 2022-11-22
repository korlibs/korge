/**
 * Allows to enqueue GPU commands that will be processed by a different thread eventually.
 */
@file:Suppress("OPT_IN_IS_NOT_ENABLED")
@file:OptIn(com.soywiz.korio.annotations.KorIncomplete::class)

package com.soywiz.korag

import com.soywiz.kds.ConcurrentPool
import com.soywiz.kds.Deque
import com.soywiz.kds.FloatDeque
import com.soywiz.kds.IntDeque
import com.soywiz.kds.IntSet
import com.soywiz.kds.Pool
import com.soywiz.kds.fastCastTo
import com.soywiz.kds.lock.NonRecursiveLock
import com.soywiz.kmem.FBuffer
import com.soywiz.kmem.extract
import com.soywiz.kmem.extract16
import com.soywiz.kmem.extract24
import com.soywiz.kmem.extract4
import com.soywiz.kmem.extract8
import com.soywiz.kmem.extractBool
import com.soywiz.kmem.finsert
import com.soywiz.kmem.finsert16
import com.soywiz.kmem.finsert24
import com.soywiz.kmem.finsert4
import com.soywiz.kmem.finsert8
import com.soywiz.korag.annotation.KoragExperimental
import com.soywiz.korag.shader.Program
import com.soywiz.korag.shader.ProgramConfig
import com.soywiz.korag.shader.UniformLayout
import com.soywiz.korio.annotations.KorIncomplete
import com.soywiz.korio.annotations.KorInternal
import com.soywiz.korma.geom.Rectangle
import com.soywiz.krypto.encoding.hex
import kotlinx.coroutines.CompletableDeferred

@KorInternal
inline fun AGQueueProcessor.processBlocking(list: AGList, maxCount: Int = 1) {
    with(list) {
        processBlocking(maxCount)
    }
}

@KorInternal
inline fun AGQueueProcessor.processBlockingAll(list: AGList): Unit = processBlocking(list, -1)

enum class AGEnable {
    BLEND, CULL_FACE, DEPTH, SCISSOR, STENCIL;

    companion object {
        val VALUES = values()
    }
}

class AGManagedObjectPool(val name: String, val checked: Boolean = true) {
    private val pool = ConcurrentPool { it + 1 }
    private val allocated = IntSet()

    fun alloc(): Int {
        return pool.alloc().also {
            if (checked) allocated.add(it)
        }
    }

    fun free(value: Int) {
        if (checked) {
            if (value !in allocated) error("Not allocated $value in $name")
            allocated.remove(value)
        }
        pool.free(value)
    }
}

//@KorIncomplete
class AGGlobalState(val checked: Boolean = false) {
    internal var contextVersion = 0
    internal var renderThreadId: Long = -1L
    internal var renderThreadName: String? = null
    internal val vaoIndices = AGManagedObjectPool("vao", checked = checked)
    internal val uboIndices = AGManagedObjectPool("ubo", checked = checked)
    internal val bufferIndices = AGManagedObjectPool("buffer", checked = checked)
    internal val programIndices = AGManagedObjectPool("program", checked = checked)
    internal val textureIndices = AGManagedObjectPool("texture", checked = checked)
    internal val frameBufferIndices = AGManagedObjectPool("frame", checked = checked)
    //var programIndex = KorAtomicInt(0)
    private val lock = NonRecursiveLock()
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

class AGListData {
    private val _data = IntDeque(128)
    private val _ints = IntDeque(16)
    private val _float = FloatDeque(16)
    private val _extra = Deque<Any?>(16)

    fun isEmpty(): Boolean = _data.isEmpty()
    fun isNotEmpty(): Boolean = _data.isNotEmpty()

    fun clear() {
        _data.clear()
        _ints.clear()
        _float.clear()
        _extra.clear()
    }

    fun addExtra(v0: Any?) {
        _extra.add(v0)
    }

    fun addExtra(v0: Any?, v1: Any?) {
        _extra.add(v0); _extra.add(v1)
    }

    fun addFloat(v0: Float) {
        _float.add(v0)
    }

    fun addFloat(v0: Float, v1: Float) {
        _float.add(v0); _float.add(v1)
    }

    fun addFloat(v0: Float, v1: Float, v2: Float) {
        _float.add(v0); _float.add(v1); _float.add(v2)
    }

    fun addFloat(v0: Float, v1: Float, v2: Float, v3: Float) {
        _float.add(v0); _float.add(v1); _float.add(v2); _float.add(v3)
    }

    fun addInt(v0: Int) {
        _ints.add(v0)
    }

    fun addInt(v0: Int, v1: Int) {
        _ints.add(v0); _ints.add(v1)
    }

    fun addInt(v0: Int, v1: Int, v2: Int) {
        _ints.add(v0); _ints.add(v1); _ints.add(v2)
    }

    fun addInt(v0: Int, v1: Int, v2: Int, v3: Int) {
        _ints.add(v0); _ints.add(v1); _ints.add(v2); _ints.add(v3)
    }

    fun add(v0: Int) {
        _data.add(v0)
    }

    fun <T> readExtra(): T = _extra.removeFirst().fastCastTo()
    fun readFloat(): Float = _float.removeFirst()
    fun readInt(): Int = _ints.removeFirst()
    fun read(): Int = _data.removeFirst()
}

// @TODO: Support either calling other lists, or copying the contents of other list here
class AGList(val globalState: AGGlobalState) {
    var contextVersion: Int by globalState::contextVersion

    val tempRect = Rectangle()

    internal val completed = CompletableDeferred<Unit>()

    private val _lock = NonRecursiveLock()
    private val listDataPool: Pool<AGListData> = Pool(reset = { it.clear() }) { AGListData() }
    private val currentReadList: Deque<AGListData> = Deque<AGListData>(16)
    private var currentRead: AGListData = listDataPool.alloc()
    private var currentWrite: AGListData = listDataPool.alloc()

    private fun <T> readExtra(): T = currentRead.readExtra()
    private fun readFloat(): Float = currentRead.readFloat()
    private fun readInt(): Int = currentRead.readInt()
    private fun read(): Int = currentRead.read()

    private fun readTakeMore(): Boolean {
        _lock {
            currentRead.clear()
            if (currentReadList.size > 0) {
                listDataPool.free(currentRead)
                currentRead = currentReadList.removeFirst()
                return true
            }
            return false
        }
    }

    private fun writeListFlush() {
        _lock {
            if (currentWrite.isEmpty()) return@_lock

            currentReadList.add(currentWrite)
            currentWrite = listDataPool.alloc()
        }
    }

    val tempUBOs = Pool { uboCreate() }
    private val uniformValuesPool = Pool { AG.UniformValues() }

    @KorInternal
    fun AGQueueProcessor.processBlocking(maxCount: Int = 1): Boolean {
        var pending = maxCount
        val processor = this@processBlocking

        processor.listStart()

        while (true) {
            if (pending-- == 0) break
            if (currentRead.isEmpty() && currentReadList.isNotEmpty()) {
                readTakeMore()
            }
            // @TODO: Wait for more data
            if (currentRead.isEmpty()) return@processBlocking false
            val data: Int = currentRead.read()

            val cmd = data.extract8(24)
            when (cmd) {
                CMD_FLUSH -> {
                    processor.flush()
                }
                CMD_FINISH -> {
                    processor.finish()
                    completed.complete(Unit)
                    return true
                }
                CMD_CONTEXT_LOST -> processor.contextLost()
                CMD_DEPTH_FUNCTION -> processor.depthFunction(AGCompareMode.VALUES[data.extract4(0)])
                CMD_ENABLE -> processor.enableDisable(AGEnable.VALUES[data.extract4(0)], enable = true)
                CMD_DISABLE -> processor.enableDisable(AGEnable.VALUES[data.extract4(0)], enable = false)
                CMD_COLOR_MASK -> processor.colorMask(
                    data.extract(0),
                    data.extract(1),
                    data.extract(2),
                    data.extract(3)
                )

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
                CMD_PROGRAM_CREATE -> processor.programCreate(data.extract16(0), readExtra(), readExtra())
                CMD_PROGRAM_DELETE -> {
                    val programId = data.extract16(0)
                    processor.programDelete(programId)
                    globalState.programIndices.free(programId)
                }

                CMD_PROGRAM_USE -> processor.programUse(data.extract16(0))
                // Draw
                CMD_DRAW -> processor.draw(
                    AGDrawType.VALUES[data.extract4(0)],
                    readInt(), readInt(), readInt(),
                    AGIndexType.VALUES.getOrNull(data.extract4(4)),
                    readExtra()
                )

                CMD_READ_PIXELS -> processor.readPixels(
                    readInt(), readInt(), readInt(), readInt(),
                    readExtra(),
                    AG.ReadKind.VALUES[data.extract4(0)]
                )

                CMD_READ_PIXELS_TO_TEXTURE -> processor.readPixelsToTexture(
                    readInt(), readInt(), readInt(), readInt(), readInt(),
                    AG.ReadKind.VALUES[data.extract4(0)]
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
                CMD_VIEWPORT -> processor.viewport(readInt(), readInt(), readInt(), readInt())
                CMD_CLEAR -> processor.clear(data.extract(0), data.extract(1), data.extract(2))
                CMD_CLEAR_COLOR -> processor.clearColor(readFloat(), readFloat(), readFloat(), readFloat())
                CMD_CLEAR_DEPTH -> processor.clearDepth(readFloat())
                CMD_CLEAR_STENCIL -> processor.clearStencil(data.extract24(0))
                // VAO
                CMD_VAO_CREATE -> processor.vaoCreate(data.extract16(0))
                CMD_VAO_DELETE -> {
                    val id = data.extract16(0)
                    processor.vaoDelete(id)
                    globalState.vaoIndices.free(id)
                }

                CMD_VAO_SET -> processor.vaoSet(data.extract16(0), AG.VertexArrayObject(readExtra()))
                CMD_VAO_USE -> processor.vaoUse(data.extract16(0))
                // UBO
                CMD_UBO_CREATE -> processor.uboCreate(data.extract16(0))
                CMD_UBO_DELETE -> {
                    val id = data.extract16(0)
                    processor.uboDelete(id)
                    globalState.uboIndices.free(id)
                }

                CMD_UBO_SET -> {
                    val ubo = readExtra<AG.UniformValues>()
                    processor.uboSet(data.extract16(0), ubo)
                    uniformValuesPool.free(ubo)
                }

                CMD_UBO_USE -> processor.uboUse(data.extract16(0))
                // BUFFER
                CMD_BUFFER_CREATE -> processor.bufferCreate(data.extract16(0))
                CMD_BUFFER_DELETE -> processor.bufferDelete(data.extract16(0))
                // TEXTURES
                CMD_TEXTURE_CREATE -> processor.textureCreate(data.extract16(0))
                CMD_TEXTURE_DELETE -> {
                    val textureId = data.extract16(0)
                    processor.textureDelete(textureId)
                    globalState.textureIndices.free(textureId)
                }

                CMD_TEXTURE_BIND -> processor.textureBind(
                    data.extract16(0),
                    AG.TextureTargetKind.VALUES[data.extract4(16)],
                    readInt()
                )

                CMD_TEXTURE_BIND_ENSURING -> processor.textureBindEnsuring(readExtra())
                CMD_TEXTURE_UPDATE -> processor.textureUpdate(
                    textureId = data.extract16(0),
                    target = AG.TextureTargetKind.VALUES[data.extract4(16)],
                    index = readInt(),
                    bmp = readExtra(),
                    source = readExtra(),
                    doMipmaps = data.extract(20),
                    premultiplied = data.extract(21)
                )
                // FRAMEBUFFERS
                CMD_FRAMEBUFFER_CREATE -> processor.frameBufferCreate(data.extract16(0))
                CMD_FRAMEBUFFER_DELETE -> processor.frameBufferDelete(data.extract16(0))
                CMD_FRAMEBUFFER_SET -> processor.frameBufferSet(
                    data.extract16(0),
                    readInt(),
                    readInt(),
                    readInt(),
                    data.extractBool(17),
                    data.extractBool(18)
                )

                CMD_FRAMEBUFFER_USE -> processor.frameBufferUse(data.extract16(0))
                else -> TODO("Unknown AG command ${cmd.hex}")
            }
        }
        return false
    }

    fun enable(kind: AGEnable): Unit = currentWrite.add(CMD(CMD_ENABLE).finsert4(kind.ordinal, 0))
    fun disable(kind: AGEnable): Unit = currentWrite.add(CMD(CMD_DISABLE).finsert4(kind.ordinal, 0))

    fun sync(deferred: CompletableDeferred<Unit>) {
        currentWrite.addExtra(deferred)
        currentWrite.add(CMD(CMD_SYNC))
    }

    @KoragExperimental
    suspend fun sync() {
        // @TODO: Will only work if we are processing stuff in another thread
        val deferred = CompletableDeferred<Unit>()
        sync(deferred)
        deferred.await()
    }

    inline fun enableDisable(kind: AGEnable, enable: Boolean, block: () -> Unit = {}) {
        if (enable) {
            enable(kind)
            block()
        } else {
            disable(kind)
        }
    }

    fun flush() {
        currentWrite.add(CMD(CMD_FLUSH))
    }

    fun listFlush() {
        writeListFlush()
    }

    fun contextLost() {
        currentWrite.add(CMD(CMD_CONTEXT_LOST))
    }

    fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {
        currentWrite.add(CMD(CMD_COLOR_MASK).finsert(red, 0).finsert(green, 1).finsert(blue, 2).finsert(alpha, 3))
    }

    fun depthMask(depth: Boolean) {
        currentWrite.add(CMD(CMD_DEPTH_MASK).finsert(depth, 0))
    }

    fun clearColor(red: Float, green: Float, blue: Float, alpha: Float) {
        currentWrite.addFloat(red, green, blue, alpha)
        currentWrite.add(CMD(CMD_CLEAR_COLOR))
    }

    fun clearDepth(depth: Float) {
        currentWrite.addFloat(depth)
        currentWrite.add(CMD(CMD_CLEAR_DEPTH))
    }

    fun clearStencil(stencil: Int) {
        currentWrite.add(CMD(CMD_CLEAR_STENCIL).finsert24(stencil, 0))
    }

    fun clear(color: Boolean, depth: Boolean, stencil: Boolean) {
        currentWrite.add(CMD(CMD_CLEAR).finsert(color, 0).finsert(depth, 1).finsert(stencil, 2))
    }

    fun depthRange(near: Float, far: Float) {
        currentWrite.addFloat(near, far)
        currentWrite.add(CMD(CMD_DEPTH_RANGE))
    }

    fun blendEquation(rgb: AGBlendEquation, a: AGBlendEquation = rgb) {
        currentWrite.add(CMD(CMD_BLEND_EQ).finsert4(rgb.ordinal, 0).finsert4(a.ordinal, 4))
    }

    fun blendFunction(
        srcRgb: AGBlendFactor,
        dstRgb: AGBlendFactor,
        srcA: AGBlendFactor = srcRgb,
        dstA: AGBlendFactor = dstRgb
    ) {
        currentWrite.add(
            CMD(CMD_BLEND_FUNC)
                .finsert4(srcRgb.ordinal, 0).finsert4(dstRgb.ordinal, 4)
                .finsert4(srcA.ordinal, 8).finsert4(dstA.ordinal, 12)
        )
    }

    fun cullFace(face: AGCullFace) {
        currentWrite.add(CMD(CMD_CULL_FACE).finsert4(face.ordinal, 0))
    }

    fun frontFace(face: AGFrontFace) {
        currentWrite.add(CMD(CMD_FRONT_FACE).finsert4(face.ordinal, 0))
    }

    fun scissor(x: Int, y: Int, width: Int, height: Int) {
        //if (width < 200) println("AGList.scissor: $x, $y, $width, $height")
        currentWrite.addInt(x, y, width, height)
        currentWrite.add(CMD(CMD_SCISSOR))
    }

    fun viewport(x: Int, y: Int, width: Int, height: Int) {
        currentWrite.addInt(x, y, width, height)
        currentWrite.add(CMD(CMD_VIEWPORT))
    }

    fun finish() {
        currentWrite.add(CMD(CMD_FINISH))
        listFlush()
    }

    fun depthFunction(depthTest: AGCompareMode) {
        currentWrite.add(CMD(CMD_DEPTH_FUNCTION).finsert4(depthTest.ordinal, 0))
    }

    ////////////////////////////////////////
    // PROGRAMS
    ////////////////////////////////////////

    fun createProgram(program: Program, programConfig: ProgramConfig? = null): Int {
        val programId = globalState.programIndices.alloc()
        currentWrite.addExtra(program)
        currentWrite.addExtra(programConfig)
        currentWrite.add(CMD(CMD_PROGRAM_CREATE).finsert16(programId, 0))
        return programId
    }

    fun deleteProgram(programId: Int) {
        currentWrite.add(CMD(CMD_PROGRAM_DELETE).finsert16(programId, 0))
    }

    fun useProgram(programId: Int) {
        currentWrite.add(CMD(CMD_PROGRAM_USE).finsert16(programId, 0))
    }

    fun useProgram(program: AG.AgProgram) {
        program.ensure(this)
        useProgram(program.programId)
    }

    ////////////////////////////////////////
    // TEXTURES
    ////////////////////////////////////////

    fun createTexture(): Int {
        val textureId = globalState.textureIndices.alloc()
        currentWrite.add(CMD(CMD_TEXTURE_CREATE).finsert16(textureId, 0))
        return textureId
    }

    fun deleteTexture(textureId: Int) {
        currentWrite.add(CMD(CMD_TEXTURE_DELETE).finsert16(textureId, 0))
    }

    fun updateTexture(
        textureId: Int,
        target: AG.TextureTargetKind,
        index: Int,
        data: Any?,
        source: AG.BitmapSourceBase,
        doMipmaps: Boolean,
        premultiplied: Boolean
    ) {
        currentWrite.addExtra(data, source)
        currentWrite.addInt(index)
        currentWrite.add(
            CMD(CMD_TEXTURE_UPDATE).finsert16(textureId, 0).finsert4(target.ordinal, 16).finsert(doMipmaps, 20)
                .finsert(premultiplied, 21)
        )
    }

    fun bindTexture(textureId: Int, target: AG.TextureTargetKind, implForcedTexId: Int = -1) {
        currentWrite.addInt(implForcedTexId)
        currentWrite.add(CMD(CMD_TEXTURE_BIND).finsert16(textureId, 0).finsert4(target.ordinal, 16))
    }

    fun bindTextureEnsuring(texture: AG.Texture?) {
        currentWrite.addExtra(texture)
        currentWrite.add(CMD(CMD_TEXTURE_BIND_ENSURING))
    }

    ////////////////////////////////////////
    // UNIFORMS
    ////////////////////////////////////////

    fun uniformsSet(layout: UniformLayout, data: FBuffer) {
        currentWrite.addExtra(layout, data)
        currentWrite.add(CMD(CMD_UNIFORMS_SET))
    }

    ////////////////////////////////////////
    // DRAW
    ////////////////////////////////////////

    fun draw(
        type: AGDrawType,
        vertexCount: Int,
        offset: Int = 0,
        instances: Int = 1,
        indexType: AGIndexType? = null,
        indices: AG.Buffer? = null
    ) {
        currentWrite.addInt(vertexCount, offset, instances)
        currentWrite.addExtra(indices)
        currentWrite.add(CMD(CMD_DRAW).finsert4(type.ordinal, 0).finsert4(indexType?.ordinal ?: 0xF, 4))
    }

    fun stencilFunction(compareMode: AG.CompareMode, referenceValue: Int, readMask: Int) {
        currentWrite.add(CMD(CMD_STENCIL_FUNC).finsert4(compareMode.ordinal, 0).finsert8(referenceValue, 8).finsert8(readMask, 16))
    }

    fun stencilOperation(
        actionOnDepthFail: AG.StencilOp,
        actionOnDepthPassStencilFail: AG.StencilOp,
        actionOnBothPass: AG.StencilOp
    ) {
        currentWrite.add(
            CMD(CMD_STENCIL_OP).finsert4(actionOnDepthFail.ordinal, 0).finsert4(actionOnDepthPassStencilFail.ordinal, 4)
                .finsert8(actionOnBothPass.ordinal, 8)
        )
    }

    fun stencilMask(writeMask: Int) {
        currentWrite.addInt(writeMask)
        currentWrite.add(CMD(CMD_STENCIL_MASK))
        //add(CMD(CMD_STENCIL_MASK).finsert8(writeMask, 0))
    }

    ////////////////////////////////////////
    // VAO: Vertex Array Object
    ////////////////////////////////////////
    fun vaoCreate(): Int {
        val id = globalState.vaoIndices.alloc()
        currentWrite.add(CMD(CMD_VAO_CREATE).finsert16(id, 0))
        return id
    }

    fun vaoDelete(id: Int) {
        currentWrite.add(CMD(CMD_VAO_DELETE).finsert16(id, 0))
    }

    fun vaoSet(id: Int, vao: AG.VertexArrayObject) {
        currentWrite.addExtra(vao.list)
        currentWrite.add(CMD(CMD_VAO_SET).finsert16(id, 0))
    }

    fun vaoUse(id: Int) {
        currentWrite.add(CMD(CMD_VAO_USE).finsert16(id, 0))
    }

    ////////////////////////////////////////
    // UBO: Uniform Buffer Object
    ////////////////////////////////////////
    fun uboCreate(): Int {
        val id = globalState.uboIndices.alloc()
        currentWrite.add(CMD(CMD_UBO_CREATE).finsert16(id, 0))
        return id
    }

    fun uboDelete(id: Int) {
        currentWrite.add(CMD(CMD_UBO_DELETE).finsert16(id, 0))
    }

    fun uboSet(id: Int, ubo: AG.UniformValues) {
        val uboCopy = uniformValuesPool.alloc()
        uboCopy.setTo(ubo)
        currentWrite.addExtra(uboCopy)
        currentWrite.add(CMD(CMD_UBO_SET).finsert16(id, 0))
    }

    // @TODO: If we have a layout we can have the objects already arranged.
    // @TODO: We have to put only integers and floats here, so textures should use the textureId for example
    @KorIncomplete
    fun uboSet(id: Int, data: FBuffer, layout: UniformLayout) {
        TODO()
    }

    fun uboUse(id: Int) {
        currentWrite.add(CMD(CMD_UBO_USE).finsert16(id, 0))
    }

    fun readPixels(x: Int, y: Int, width: Int, height: Int, data: Any, kind: AG.ReadKind) {
        currentWrite.addInt(x, y, width, height)
        currentWrite.addExtra(data)
        currentWrite.add(CMD(CMD_READ_PIXELS).finsert4(kind.ordinal, 0))
    }

    fun readPixelsToTexture(textureId: Int, x: Int, y: Int, width: Int, height: Int, kind: AG.ReadKind) {
        currentWrite.addInt(textureId)
        currentWrite.addInt(x, y, width, height)
        currentWrite.add(CMD(CMD_READ_PIXELS_TO_TEXTURE).finsert4(kind.ordinal, 0))
    }

    // BUFFERS
    fun bufferCreate(): Int {
        val id = globalState.bufferIndices.alloc()
        currentWrite.add(CMD(CMD_BUFFER_CREATE).finsert16(id, 0))
        return id
    }

    fun bufferDelete(id: Int) {
        currentWrite.add(CMD(CMD_BUFFER_DELETE).finsert16(id, 0))
    }

    ////////////////////////////////////////
    // Frame Buffers
    ////////////////////////////////////////
    fun frameBufferCreate(): Int {
        val id = globalState.frameBufferIndices.alloc()
        currentWrite.add(CMD(CMD_FRAMEBUFFER_CREATE).finsert16(id, 0))
        return id
    }

    fun frameBufferDelete(id: Int) {
        globalState.uboIndices.free(id)
        currentWrite.add(CMD(CMD_FRAMEBUFFER_DELETE).finsert16(id, 0))
    }

    fun frameBufferSet(id: Int, textureId: Int, width: Int, height: Int, hasStencil: Boolean, hasDepth: Boolean) {
        currentWrite.addInt(textureId, width, height)
        currentWrite.add(CMD(CMD_FRAMEBUFFER_SET).finsert16(id, 0).finsert(hasStencil, 17).finsert(hasDepth, 18))
    }

    fun frameBufferUse(id: Int) {
        currentWrite.add(CMD(CMD_FRAMEBUFFER_USE).finsert16(id, 0))
    }

    companion object {
        private fun CMD(cmd: Int): Int = 0.finsert8(cmd, 24)

        // Special
        private const val CMD_FLUSH = 0xF9
        private const val CMD_CONTEXT_LOST = 0xFA
        private const val CMD_READ_PIXELS = 0xFB
        private const val CMD_READ_PIXELS_TO_TEXTURE = 0xFC
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
        private const val CMD_VIEWPORT = 0x0C
        // Clear
        private const val CMD_CLEAR = 0x10
        private const val CMD_CLEAR_COLOR = 0x11
        private const val CMD_CLEAR_DEPTH = 0x12
        private const val CMD_CLEAR_STENCIL = 0x13

        // Programs
        private const val CMD_PROGRAM_CREATE = 0x30
        private const val CMD_PROGRAM_DELETE = 0x31
        private const val CMD_PROGRAM_USE = 0x32
        private const val CMD_PROGRAM_USE_EXT = 0x33
        // Textures
        private const val CMD_TEXTURE_CREATE = 0x40
        private const val CMD_TEXTURE_DELETE = 0x41
        private const val CMD_TEXTURE_UPDATE = 0x42
        private const val CMD_TEXTURE_BIND = 0x43
        private const val CMD_TEXTURE_BIND_ENSURING = 0x44
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
        // VAO - Vertex Array Object
        private const val CMD_VAO_CREATE = 0x90
        private const val CMD_VAO_DELETE = 0x91
        private const val CMD_VAO_SET = 0x92
        private const val CMD_VAO_USE = 0x93
        // UBO - Uniform Buffer Object
        private const val CMD_UBO_CREATE = 0xA0
        private const val CMD_UBO_DELETE = 0xA1
        private const val CMD_UBO_SET = 0xA2
        private const val CMD_UBO_USE = 0xA3
        // BUFFERS
        private const val CMD_BUFFER_CREATE = 0xB0
        private const val CMD_BUFFER_DELETE = 0xB1
        private const val CMD_BUFFER_SET = 0xB2
        private const val CMD_BUFFER_USE = 0xB3
        // FRAME BUFFERS
        private const val CMD_FRAMEBUFFER_CREATE = 0xC0
        private const val CMD_FRAMEBUFFER_DELETE = 0xC1
        private const val CMD_FRAMEBUFFER_SET = 0xC2
        private const val CMD_FRAMEBUFFER_USE = 0xC3
    }
}
