package com.soywiz.korge3d

import com.soywiz.kds.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.render.AgBitmapTextureManager
import com.soywiz.korge.render.AgBufferManager
import com.soywiz.korge.render.RenderContext
import com.soywiz.korma.geom.Matrix3D
import com.soywiz.korma.geom.Vector3D

@Korge3DExperimental
class RenderContext3D() {
    lateinit var nag: NAG
    lateinit var rctx: RenderContext
    val shaders = Shaders3D()
    val bindMat4 = Matrix3D()
    val bones = Array(128) { Matrix3D() }
    val tempMat = Matrix3D()
    val projMat: Matrix3D = Matrix3D()
    val lights = arrayListOf<Light3D>()
    val projCameraMat: Matrix3D = Matrix3D()
    val cameraMat: Matrix3D = Matrix3D()
    val cameraMatInv: Matrix3D = Matrix3D()
    val dynamicBufferPool = Pool { NAGBuffer() }
    val ambientColor: Vector3D = Vector3D()
    val textureManager by lazy { AgBitmapTextureManager() }
    val bufferManager by lazy { AgBufferManager() }

    @PublishedApi internal val tempBuffers = FastArrayList<NAGBuffer>()

    inline fun useDynamicVertexData(vertexBuffers: FastArrayList<BufferWithVertexLayout>, block: (NAGVertices) -> Unit) {
        dynamicBufferPool.allocMultiple(vertexBuffers.size, tempBuffers) { buffers ->
            val vertices = NAGVertices(vertexBuffers.mapIndexed { index, bufferWithVertexLayout -> NAGVerticesPart(bufferWithVertexLayout.layout, buffers[index]) })
            block(vertices)
        }
    }
}
