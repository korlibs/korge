package com.soywiz.korge3d

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*

data class BufferWithVertexLayout(
    val buffer: FBuffer,
    val layout: VertexLayout
) {
    val vertexSizeInBytes = layout.totalSize
    val vertexSizeInFloats = vertexSizeInBytes / 4
}

@Korge3DExperimental
data class Mesh3D constructor(
    val vertexBuffers: FastArrayList<BufferWithVertexLayout>,
    val indexBuffer: FBuffer,
    val indexType: AG.IndexType,
    val vertexCount:Int,
    val program: Program?,
    val drawType: AG.DrawType,
    val hasTexture: Boolean = false,
    val maxWeights: Int = 0,
    var skin: Skin3D? = null,
    var material: Material3D? = null
) {
    //val modelMat = Matrix3D()
    //val vertexCount = vertexBuffer.size / 4 / vertexSizeInFloats

    /*

    val fbuffer by lazy {
        FBuffer.alloc(data.size * 4).apply {
            setAlignedArrayFloat32(0, this@Mesh3D.data, 0, this@Mesh3D.data.size)
        }
        //FBuffer.wrap(MemBufferAlloc(data.size * 4)).apply {
        //	arraycopy(this@Mesh3D.data, 0, this@apply.mem, 0, this@Mesh3D.data.size) // Bug in kmem-js?
        //}
    }

     */

    init {
        //println("vertexCount: $vertexCount, vertexSizeInFloats: $vertexSizeInFloats, data.size: ${data.size}")
    }
}
