package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korag.shader.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*

data class AGFullBatch(
    val macros: FastArrayList<AGMacroBatch>,
    val completed: Signal<Unit>? = null,
)

data class AGMacroBatch(
    val renderBuffer: AGBaseRenderBuffer,
    val vertexData: AGVertexArrayObject,
    val indexData: AGBuffer?,
    val batches: FastArrayList<AGMiniBatch>,
)

data class AGMiniBatch(
    val program: Program,
    val uniforms: AGUniformValues,
    val state: AGFullState,
    val drawCommands: AGDrawCommandArray = AGDrawCommandArray.EMPTY,
    val transferCommands: FastArrayList<AGTransferCommand> = fastArrayListOf(),
)

sealed interface AGTransferCommand {
    val completed: Signal<Unit>?
    class CopyToTexture(val target: AGTexture, val x: Int, val y: Int, val width: Int, val height: Int, override val completed: Signal<Unit>? = null) : AGTransferCommand
    class ReadBits(val readKind: AGReadKind, val x: Int, val y: Int, val width: Int, val height: Int, override val completed: Signal<Unit>? = null, val targetBitmap: Bitmap32? = null, val targetFloats: FloatArray? = null) : AGTransferCommand
}

inline class AGDrawCommandArrayWriter(private val data: IntArrayList = IntArrayList()) {
    fun toArray(): AGDrawCommandArray = AGDrawCommandArray(data.toIntArray())
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

inline class AGDrawCommandArray(val data: IntArray) {
    companion object {
        val EMPTY = AGDrawCommandArray(intArrayOf())
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


///////////// OLD BATCH ////////////////////////////////////

data class AGBatch constructor(
    var vertexData: FastArrayList<AGVertexData> = fastArrayListOf(AGVertexData(null)),
    var program: Program = DefaultShaders.PROGRAM_DEBUG,
    var type: AGDrawType = AGDrawType.TRIANGLES,
    var vertexCount: Int = 0,
    var indices: AGBuffer? = null,
    var indexType: AGIndexType = AGIndexType.USHORT,
    var offset: Int = 0,
    var blending: AGBlending = AGBlending.NORMAL,
    var uniforms: AGUniformValues = AGUniformValues.EMPTY,
    var stencilOpFunc: AGStencilOpFuncState = AGStencilOpFuncState.DEFAULT,
    var stencilRef: AGStencilReferenceState = AGStencilReferenceState.DEFAULT,
    var colorMask: AGColorMaskState = AGColorMaskState(),
    var renderState: AGRenderState = AGRenderState(),
    var scissor: AGScissor = AGScissor.NIL,
    var instances: Int = 1
) {

    var stencilFull: AGStencilFullState
        get() = AGStencilFullState(stencilOpFunc, stencilRef)
        set(value) {
            stencilOpFunc = value.opFunc
            stencilRef = value.ref
        }

    private val singleVertexData = FastArrayList<AGVertexData>()

    private fun ensureSingleVertexData() {
        if (singleVertexData.isEmpty()) singleVertexData.add(AGVertexData(null))
        vertexData = singleVertexData
    }

    @Deprecated("Use vertexData instead")
    var vertices: AGBuffer
        get() = (singleVertexData.firstOrNull() ?: vertexData.first()).buffer
        set(value) {
            ensureSingleVertexData()
            singleVertexData[0]._buffer = value
        }
    @Deprecated("Use vertexData instead")
    var vertexLayout: VertexLayout
        get() = (singleVertexData.firstOrNull() ?: vertexData.first()).layout
        set(value) {
            ensureSingleVertexData()
            singleVertexData[0].layout = value
        }
}
