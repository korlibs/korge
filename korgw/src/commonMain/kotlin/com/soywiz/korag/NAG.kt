package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korag.shader.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*

/**
 * New Accelerated Graphics
 */
abstract class NAG {
    protected var contextVersion: Int = 0

    open fun contextLost() {
        contextVersion++
    }

    abstract fun execute(command: NAGCommand)

    fun draw(batches: List<NAGBatch>) {
        execute(NAGCommandFullBatch(batches))
    }

    fun readToTexture(renderBuffer: NAGRenderBuffer, texture: NAGTexture, x: Int, y: Int, width: Int, height: Int, completed: Signal<Unit>? = null) {
        execute(NAGCommandTransfer.CopyToTexture(renderBuffer, texture, x, y, width, height, completed))
    }

    fun readBits(renderBuffer: NAGRenderBuffer, kind: AGReadKind, x: Int, y: Int, width: Int, height: Int, completed: Signal<Unit>? = null, targetBitmap: Bitmap32? = null, targetFloats: FloatArray? = null) {
        execute(NAGCommandTransfer.ReadBits(renderBuffer, kind, x, y, width, height, completed, targetBitmap, targetFloats))
    }

    fun finish(completed: Signal<Unit>? = null) {
        execute(NAGCommandFinish(completed))
    }

    suspend fun finishSuspend() {
        Signal<Unit>().also { finish(it) }.waitOne()
    }
}

open class NAGMainRenderBuffer : NAGRenderBuffer() {
}

open class NAGRenderBuffer : NAGObject() {
    var width: Int = 0
    var height: Int = 0

    fun setSize(width: Int, height: Int) {
        if (this.width == width && this.height == height) return
        this.width = width
        this.height = height
        this.invalidate()
    }
}

class NAGProgram : NAGObject() {
    var program: Program? = null

    fun set(program: Program) {
        this.program = program
        this.invalidate()
    }
}

class NAGTexture : NAGObject() {
    var content: Bitmap? = null

    fun upload(content: Bitmap? = null) {
        this.content = content
        this.invalidate()
    }
}

class NAGBuffer : NAGObject() {
    var content: Buffer? = null
    var usage: Usage = Usage.DYNAMIC

    enum class Usage { DYNAMIC, STATIC, STREAM }

    fun upload(content: Buffer? = null, usage: Usage = Usage.DYNAMIC) {
        this.content = content
        this.usage = usage
        this.invalidate()
    }
}

open class NAGObject {
    var _nativeContextVersion: Int = -1
    var _nativeObjectVersion: Int = -1
    var _nativeInt: Int = -1
    var _nativeInt1: Int = -1
    var _nativeInt2: Int = -1
    var _nativeDelete: ((NAGObject) -> Unit)? = null
    var _nativeDeleteRegister: ((NAGObject) -> Unit)? = null
    var _nativeLong: Long = 0L
    var _nativeObject: Any? = null
    var deleted = false
    var version: Int = 0

    inline fun _nativeCreateObject(contextVersion: Int, noinline _nativeDeleteRegister: (NAGObject) -> Unit, block: (NAGObject) -> Unit) {
        if (this._nativeContextVersion != contextVersion) {
            this._nativeContextVersion = contextVersion
            this._nativeDeleteRegister = _nativeDeleteRegister
            block(this)
        }
    }

    inline fun _nativeUploadObject(block: (NAGObject) -> Unit) {
        if (this._nativeObjectVersion != version) {
            this._nativeObjectVersion = version
            block(this)
        }
    }

    fun invalidate() {
        version++
    }

    fun delete() {
        _nativeDeleteRegister?.invoke(this)
        _nativeDeleteRegister = null
        deleted = true
    }
}

class NAGVertices(val data: List<Pair<VertexLayout, NAGBuffer>>)

sealed interface NAGCommand

class NAGCommandFinish(
    val completed: Signal<Unit>? = null,
) : NAGCommand

data class NAGCommandFullBatch(
    val batches: List<NAGBatch>,
) : NAGCommand

data class NAGBatch(
    val vertexData: NAGVertices,
    val indexData: NAGBuffer?,
    val batches: List<NAGUniformBatch>,
)

data class NAGUniformBatch(
    val renderBuffer: NAGRenderBuffer,
    val program: Program,
    val uniforms: AGUniformValues,
    val state: AGFullState,
    val drawCommands: NAGDrawCommandArray = NAGDrawCommandArray.EMPTY,
)

sealed interface NAGCommandTransfer : NAGCommand {
    val renderBuffer: NAGRenderBuffer
    val completed: Signal<Unit>?
    class CopyToTexture(override val renderBuffer: NAGRenderBuffer, val texture: NAGTexture, val x: Int, val y: Int, val width: Int, val height: Int, override val completed: Signal<Unit>? = null) : NAGCommandTransfer
    class ReadBits(override val renderBuffer: NAGRenderBuffer, val readKind: AGReadKind, val x: Int, val y: Int, val width: Int, val height: Int, override val completed: Signal<Unit>? = null, val targetBitmap: Bitmap32? = null, val targetFloats: FloatArray? = null) : NAGCommandTransfer
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
