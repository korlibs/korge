package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korag.shader.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*

/**
 * New Accelerated Graphics
 */
abstract class NAG {
    @PublishedApi internal var contextVersion: Int = 0
    open val frameBufferWidth: Int = 0
    open val frameBufferHeight: Int = 0
    open val devicePixelRatio: Double = 1.0

    open fun contextLost() {
        contextVersion++
    }

    abstract fun execute(command: NAGCommand)

    fun draw(batches: List<NAGBatch>) = execute(NAGCommandFullBatch(batches))
    fun draw(vararg batches: NAGBatch) = draw(batches.toList())

    fun readToTexture(renderBuffer: NAGFrameBuffer, texture: NAGTexture, x: Int, y: Int, width: Int, height: Int, completed: Signal<Unit>? = null) {
        execute(NAGCommandTransfer.CopyToTexture(renderBuffer, texture, x, y, width, height, completed))
    }

    fun readBits(renderBuffer: NAGFrameBuffer, kind: AGReadKind, x: Int, y: Int, width: Int, height: Int, completed: Signal<Unit>? = null, target: Any? = null) {
        execute(NAGCommandTransfer.ReadBits(renderBuffer, kind, x, y, width, height, completed, target))
    }

    fun finish(completed: Signal<Unit>? = null) {
        execute(NAGCommandFinish(completed))
    }

    suspend fun finishSuspend() {
        Signal<Unit>().also { finish(it) }.waitOne()
    }
}

open class NAGTextureUnit : NAGObject() {
    var texture: NAGTexture? = null
    var wrap: AGTextureWrap = AGTextureWrap.REPEAT
    var magFilter: AGMAGFilter = AGMAGFilter.LINEAR
    var minFilter: AGMINFilter = AGMINFilter(magFilter.ordinal)
    var unitId: Int = -1

    fun set(
        unitId: Int = -1,
        texture: NAGTexture,
        wrap: AGTextureWrap = AGTextureWrap.REPEAT,
        magFilter: AGMAGFilter = AGMAGFilter.LINEAR,
        minFilter: AGMINFilter = AGMINFilter(magFilter.ordinal),
    ): NAGTextureUnit {
        this.unitId = unitId
        this.texture = texture
        this.wrap = wrap
        this.magFilter = magFilter
        this.minFilter = minFilter
        this.invalidate()
        return this
    }
}

open class NAGTexture : NAGObject() {
    var content: Bitmap? = null

    fun upload(content: Bitmap? = null) {
        this.content = content
        this.invalidate()
    }
}

open class NAGFrameBuffer : NAGObject() {
    val texture = NAGTexture()
    var width: Int = 0
    var height: Int = 0

    var hasStencil: Boolean = true
    var hasDepth: Boolean = true
    val hasStencilAndDepth: Boolean get() = hasStencil && hasDepth

    fun set(width: Int = this.width, height: Int = this.height, hasStencil: Boolean = this.hasStencil, hasDepth: Boolean = this.hasDepth): NAGFrameBuffer {
        if (this.width == width && this.height == height && this.hasStencil == hasStencil && this.hasDepth == hasDepth) return this
        this.width = width
        this.height = height
        this.hasStencil = hasStencil
        this.hasDepth = hasDepth
        this.invalidate()
        return this
    }
}

class NAGProgram(val program: Program) : NAGObject()

class NAGBuffer : NAGObject() {
    var content: Buffer? = null
    var usage: Usage = Usage.DYNAMIC

    enum class Usage { DYNAMIC, STATIC, STREAM }

    fun upload(content: Buffer? = null, usage: Usage = Usage.DYNAMIC): NAGBuffer {
        this.content = content
        this.usage = usage
        this.invalidate()
        return this
    }
}

interface NAGNativeObject {
    fun markDelete()
}

open class NAGObject {
    @PublishedApi internal var _nativeContextVersion: Int = -1
    @PublishedApi internal var _nativeObjectVersion: Int = -1
    @PublishedApi internal var _native: NAGNativeObject? = null
    @PublishedApi internal var _version: Int = 0

    fun invalidate() {
        _version++
    }

    fun delete() {
        _native?.markDelete()
        _native = null
    }
}

class NAGVerticesPart(val layout: VertexLayout, val buffer: NAGBuffer)
class NAGVertices(val data: List<NAGVerticesPart>) {
    constructor(vararg data: NAGVerticesPart) : this(data.toList())
}

sealed interface NAGCommand

class NAGCommandFinish(
    val completed: Signal<Unit>? = null,
) : NAGCommand

data class NAGCommandFullBatch(
    val batches: List<NAGBatch>,
) : NAGCommand

data class NAGBatch(
    val vertexData: NAGVertices,
    val indexData: NAGBuffer? = null,
    val batches: List<NAGUniformBatch> = emptyList(),
)

data class NAGUniformBatch(
    val renderBuffer: NAGFrameBuffer?,
    val program: Program,
    val uniforms: AGUniformValues,
    val state: AGFullState,
    val drawCommands: NAGDrawCommandArray = NAGDrawCommandArray.EMPTY,
    var clear: ClearState? = null,
)

data class ClearState(
    val color: RGBA? = null,
    val depth: Float? = null,
    val stencil: Int? = null,
)

sealed interface NAGCommandTransfer : NAGCommand {
    val renderBuffer: NAGFrameBuffer
    val completed: Signal<Unit>?
    class CopyToTexture(override val renderBuffer: NAGFrameBuffer, val texture: NAGTexture, val x: Int, val y: Int, val width: Int, val height: Int, override val completed: Signal<Unit>? = null) : NAGCommandTransfer
    class ReadBits(override val renderBuffer: NAGFrameBuffer, val readKind: AGReadKind, val x: Int, val y: Int, val width: Int, val height: Int, override val completed: Signal<Unit>? = null, val target: Any? = null) : NAGCommandTransfer
}

inline class NAGDrawCommandArrayWriter(private val data: IntArrayList = IntArrayList()) {
    fun toArray(): NAGDrawCommandArray = NAGDrawCommandArray(data.toIntArray())
    fun clear() {
        data.clear()
    }
    fun add(drawType: AGDrawType, indexType: AGIndexType, offset: Int, count: Int, instances: Int = 1) {
        data.add(
            0.insert3(drawType.ordinal, 0).insert2(indexType.ordinal, 3).insert24(count, 8),
            offset, instances
        )
    }
}

inline class NAGDrawCommandArray(val data: IntArray) {
    companion object {
        val EMPTY = NAGDrawCommandArray(intArrayOf())

        operator fun invoke(build: (NAGDrawCommandArrayWriter) -> Unit): NAGDrawCommandArray =
            NAGDrawCommandArrayWriter().also(build).toArray()
    }
    val size: Int get() = data.size / 3

    private fun v0(n: Int) = data[n * 3 + 0]
    private fun v1(n: Int) = data[n * 3 + 1]
    private fun v2(n: Int) = data[n * 3 + 2]

    inline fun fastForEach(block: (drawType: AGDrawType, indexType: AGIndexType, offset: Int, count: Int, instances: Int) -> Unit) {
        for (n in 0 until size) {
            block(getDrawType(n), getIndexType(n), getOffset(n), getCount(n), getInstances(n))
        }
    }

    fun getDrawType(n: Int): AGDrawType = AGDrawType(v0(n).extract3(0))
    fun getIndexType(n: Int): AGIndexType = AGIndexType(v0(n).extract2(3))
    fun getCount(n: Int): Int = v0(n).extract24(8)
    fun getOffset(n: Int): Int = v1(n)
    fun getInstances(n: Int): Int = v2(n)
}
