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
    open var finalFrameBufferWidth: Int = 0
    open var finalFrameBufferHeight: Int = 0
    open var devicePixelRatio: Double = 1.0
    open val pixelsPerInch: Double get() = 96.0 * devicePixelRatio

    open val graphicExtensions: List<String> = emptyList()
    open val isFloatTextureSupported: Boolean get() = false
    open val isInstancedSupported: Boolean get() = true

    open val totalRenderBuffers: Int get() = 0

    @Deprecated("")
    inline fun doRender(block: () -> Unit) {
        //beforeDoRender()
        //mainRenderBuffer.init()
        //setRenderBufferTemporally(mainRenderBuffer) {
            block()
        //}
    }


    fun resized(x: Int, y: Int, width: Int, height: Int, fullWidth: Int = width, fullHeight: Int = height) {
        finalFrameBufferWidth = width
        finalFrameBufferHeight = height
    }

    open fun contextLost() {
        contextVersion++
    }

    abstract fun execute(command: NAGCommand)

    fun startFrame() {
        execute(NAGCommandStart)
    }

    fun clear(renderBuffer: NAGFrameBuffer?, color: RGBA? = null, depth: Float? = null, stencil: Int? = null) {
        execute(NAGCommandFullBatch(NAGBatch(batches = listOf(NAGUniformBatch(
            frameBuffer = renderBuffer,
            clear = ClearState(color, depth, stencil)
        )))))
    }

    fun draw(batches: List<NAGBatch>) = execute(NAGCommandFullBatch(batches))
    fun draw(vararg batches: NAGBatch) = draw(batches.toList())

    fun readToTexture(renderBuffer: NAGFrameBuffer?, texture: NAGTexture, x: Int, y: Int, width: Int, height: Int, completed: Signal<Unit>? = null, sync: Boolean = true) {
        execute(NAGCommandTransfer.CopyToTexture(renderBuffer, texture, x, y, width, height, completed))
    }
    fun readBits(renderBuffer: NAGFrameBuffer?, kind: AGReadKind, x: Int, y: Int, width: Int, height: Int, target: Any?, completed: Signal<Unit>? = null, sync: Boolean = true) {
        execute(NAGCommandTransfer.ReadBits(renderBuffer, kind, x, y, width, height, target, completed))
    }
    fun finish(completed: Signal<Unit>? = null) {
        execute(NAGCommandFinish(completed))
    }

    suspend fun readToTextureSuspend(renderBuffer: NAGFrameBuffer, texture: NAGTexture, x: Int, y: Int, width: Int, height: Int) {
        Signal<Unit>().also { readToTexture(renderBuffer, texture, x, y, width, height, it) }.awaitOne()
    }
    suspend fun readBitsSuspend(renderBuffer: NAGFrameBuffer, kind: AGReadKind, x: Int, y: Int, width: Int, height: Int, target: Any? = null) {
        Signal<Unit>().also { readBits(renderBuffer, kind, x, y, width, height, target, it) }.awaitOne()
    }
    suspend fun finishSuspend() {
        Signal<Unit>().also { finish(it) }.waitOne()
    }

    fun drawV2(
        frameBuffer: NAGFrameBuffer?,
        vertexData: NAGVertices,
        indices: NAGBuffer? = null,
        indexType: AGIndexType = AGIndexType.NONE,
        type: AGDrawType,
        program: Program,
        vertexCount: Int,
        blending: AGBlending = AGBlending.NORMAL,
        uniforms: AGUniformValues,
        renderState: AGRenderState = AGRenderState.DEFAULT,
        offset: Int = 0,
        instances: Int = 1,
    ) {
        draw(NAGBatch(
            vertexData,
            indices,
            listOf(
                NAGUniformBatch(
                    frameBuffer,
                    program,
                    uniforms,
                    AGFullState().also {
                        it.blending = blending
                        it.render = renderState
                   },
                    NAGDrawCommandArray.invoke {
                        it.add(type, indexType, offset, vertexCount, instances)
                    }
                )
            )
        ))
    }
}

open class NAGTextureUnit(var unitId: Int) : NAGObject() {
    var texture: NAGTexture? = null
    var wrap: AGTextureWrap = AGTextureWrap.REPEAT
    var linear: Boolean = true
    var trilinear: Boolean = true
    //var magFilter: AGMAGFilter = AGMAGFilter.LINEAR
    //var minFilter: AGMINFilter = AGMINFilter(magFilter.ordinal)

    val mipmaps: Boolean get() = texture?.mipmaps == true
    val minFilter: AGMINFilter get() = when {
        mipmaps -> when {
            linear -> if (trilinear) AGMINFilter.LINEAR_MIPMAP_LINEAR else AGMINFilter.LINEAR_MIPMAP_NEAREST
            else -> if (trilinear) AGMINFilter.NEAREST_MIPMAP_LINEAR else AGMINFilter.NEAREST_MIPMAP_NEAREST
        }
        else -> if (linear) AGMINFilter.LINEAR else AGMINFilter.NEAREST
    }
    val magFilter: AGMAGFilter get() = if (linear) AGMAGFilter.LINEAR else AGMAGFilter.NEAREST

    fun set(
        texture: NAGTexture?,
        linear: Boolean = true,
        trilinear: Boolean = linear,
        //wrap: AGTextureWrap = AGTextureWrap.REPEAT,
        wrap: AGTextureWrap = AGTextureWrap.CLAMP_TO_EDGE,
    ): NAGTextureUnit {
        //this.unitId = unitId
        this.texture = texture
        this.wrap = wrap
        this.linear = linear
        this.trilinear = trilinear
        this.invalidate()
        return this
    }
}

open class NAGTexture(val targetKind: AGTextureTargetKind = AGTextureTargetKind.TEXTURE_2D) : NAGObject() {
    var content: Bitmap? = null
    var width: Int = 0
    var height: Int = 0
    var mipmaps: Boolean = false
    var premultiplied: Boolean = true

    fun set(content: Bitmap? = null, mipmaps: Boolean = false): NAGTexture {
        if (content === this.content) return this
        this.content = content
        this.width = content?.width ?: 0
        this.height = content?.height ?: 0
        this.mipmaps = mipmaps
        this.premultiplied = content?.premultiplied ?: true
        this.invalidate()
        return this
    }
    fun set(content: BitmapSlice<Bitmap>?, mipmaps: Boolean = false): NAGTexture {
        return set(content?.bmp, mipmaps)
    }

    fun set(list: List<Bitmap>, width: Int, height: Int): NAGTexture {
        this.width = width
        this.height = height
        TODO()
    }
}

open class NAGFrameBuffer : NAGObject() {
    val texture = NAGTexture(AGTextureTargetKind.TEXTURE_2D)
    var width: Int = 0
    var height: Int = 0

    var hasStencil: Boolean = true
    var hasDepth: Boolean = false
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
        this.content = content?.clone(direct = true)
        this.usage = usage
        this.invalidate()
        return this
    }

    fun upload(content: Buffer, offset: Int, size: Int, usage: Usage = Usage.DYNAMIC): NAGBuffer = upload(content.sliceWithSize(offset, size), usage)
    fun upload(data: FloatArray, offset: Int = 0, size: Int = data.size - offset, usage: Usage = Usage.DYNAMIC): NAGBuffer = upload(Float32Buffer(data, offset, size).buffer, usage)
    fun upload(data: IntArray, offset: Int = 0, size: Int = data.size - offset, usage: Usage = Usage.DYNAMIC): NAGBuffer = upload(Int32Buffer(data, offset, size).buffer, usage)
    fun upload(data: ShortArray, offset: Int = 0, size: Int = data.size - offset, usage: Usage = Usage.DYNAMIC): NAGBuffer = upload(Int16Buffer(data, offset, size).buffer, usage)
    fun upload(data: ByteArray, offset: Int = 0, size: Int = data.size - offset, usage: Usage = Usage.DYNAMIC): NAGBuffer = upload(Buffer(data, offset, size), usage)
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

class NAGVerticesPart(val layout: VertexLayout, val buffer: NAGBuffer = NAGBuffer())
class NAGVertices(val data: List<NAGVerticesPart>) {
    constructor(vararg data: NAGVerticesPart) : this(data.toList())
}

sealed interface NAGCommand

object NAGCommandStart : NAGCommand

class NAGCommandFinish(
    val completed: Signal<Unit>? = null,
) : NAGCommand

data class NAGCommandFullBatch(
    val batches: List<NAGBatch>,
) : NAGCommand {
    constructor(vararg batches: NAGBatch) : this(batches.toList())
}

data class NAGBatch(
    val vertexData: NAGVertices? = null,
    val indexData: NAGBuffer? = null,
    val batches: List<NAGUniformBatch> = emptyList(),
)

data class NAGUniformBatch(
    val frameBuffer: NAGFrameBuffer?,
    val program: Program? = null,
    val uniforms: AGUniformValues? = null,
    val state: AGFullState? = null,
    val drawCommands: NAGDrawCommandArray = NAGDrawCommandArray.EMPTY,
    var clear: ClearState? = null,
)

data class ClearState(
    val color: RGBA? = null,
    val depth: Float? = null,
    val stencil: Int? = null,
)

sealed interface NAGCommandTransfer : NAGCommand {
    val frameBuffer: NAGFrameBuffer?
    val completed: Signal<Unit>?
    class CopyToTexture(override val frameBuffer: NAGFrameBuffer?, val texture: NAGTexture, val x: Int, val y: Int, val width: Int, val height: Int, override val completed: Signal<Unit>? = null) : NAGCommandTransfer
    class ReadBits(override val frameBuffer: NAGFrameBuffer?, val readKind: AGReadKind, val x: Int, val y: Int, val width: Int, val height: Int, val target: Any?, override val completed: Signal<Unit>? = null) : NAGCommandTransfer
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
