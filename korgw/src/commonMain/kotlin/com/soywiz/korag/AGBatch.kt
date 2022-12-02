package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korag.shader.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*

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
    var scissor: AGRect = AGRect.NIL,
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
