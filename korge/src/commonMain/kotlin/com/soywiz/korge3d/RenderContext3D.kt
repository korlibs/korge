package com.soywiz.korge3d

import com.soywiz.kds.FastArrayList
import com.soywiz.kds.Pool
import com.soywiz.korag.*
import com.soywiz.korge.render.AgBitmapTextureManager
import com.soywiz.korge.render.AgBufferManager
import com.soywiz.korge.render.RenderContext
import com.soywiz.korma.geom.MMatrix3D
import com.soywiz.korma.geom.MVector4

@Korge3DExperimental
class RenderContext3D() {
    lateinit var ag: AG
    lateinit var rctx: RenderContext
    val shaders = Shaders3D()
    val bindMat4 = MMatrix3D()
    val bones = Array(128) { MMatrix3D() }
    val tempMat = MMatrix3D()
    val projMat: MMatrix3D = MMatrix3D()
    val lights = arrayListOf<Light3D>()
    val projCameraMat: MMatrix3D = MMatrix3D()
    val cameraMat: MMatrix3D = MMatrix3D()
    val cameraMatInv: MMatrix3D = MMatrix3D()
    val dynamicVertexBufferPool = Pool { AGBuffer() }
    val dynamicVertexDataPool = Pool { AGVertexData() }
    val dynamicIndexBufferPool = Pool { AGBuffer() }
    val ambientColor: MVector4 = MVector4()
    val textureManager by lazy { AgBitmapTextureManager(ag) }
    val bufferManager by lazy { AgBufferManager(ag) }

    val tempAgVertexData = FastArrayList<AGVertexData>()

    inline fun useDynamicVertexData(vertexBuffers: FastArrayList<BufferWithVertexLayout>, block: (AGVertexArrayObject) -> Unit) {
        dynamicVertexDataPool.allocMultiple(vertexBuffers.size, tempAgVertexData) { vertexData ->
            for (n in 0 until vertexBuffers.size) {
                val bufferWithVertexLayout = vertexBuffers[n]
                vertexData[n].buffer.upload(bufferWithVertexLayout.buffer)
                vertexData[n].layout = bufferWithVertexLayout.layout
            }
            block(AGVertexArrayObject(vertexData))
        }
    }
}
