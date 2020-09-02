package com.soywiz.korge3d

import com.soywiz.kds.*
import com.soywiz.korag.*
import com.soywiz.korge.render.*
import com.soywiz.korma.geom.*

@Korge3DExperimental
class RenderContext3D() {
	lateinit var ag: AG
	lateinit var rctx: RenderContext
	val shaders = Shaders3D()
	val textureUnit = AG.TextureUnit()
	val bindMat4 = Matrix3D()
	val bones = Array(128) { Matrix3D() }
	val tmepMat = Matrix3D()
	val projMat: Matrix3D = Matrix3D()
	val lights = arrayListOf<Light3D>()
	val projCameraMat: Matrix3D = Matrix3D()
	val cameraMat: Matrix3D = Matrix3D()
	val cameraMatInv: Matrix3D = Matrix3D()
	val dynamicVertexBufferPool = Pool { ag.createVertexBuffer() }
	val ambientColor: Vector3D = Vector3D()

    /** Allows to draw things using a precomputed global matrix or raw vertices */
    val batch by lazy { BatchBuilder3D(this) }
}
