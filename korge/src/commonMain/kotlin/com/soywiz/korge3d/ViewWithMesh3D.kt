package com.soywiz.korge3d

import com.soywiz.kds.get
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kds.iterators.fastForEachWithIndex
import com.soywiz.kmem.clamp
import com.soywiz.korag.*
import com.soywiz.korma.geom.Matrix3D
import com.soywiz.korma.geom.invert

@Korge3DExperimental
inline fun Container3D.mesh(mesh: Mesh3D, callback: ViewWithMesh3D.() -> Unit = {}): ViewWithMesh3D {
    return ViewWithMesh3D(mesh).addTo(this, callback)
}

@Korge3DExperimental
open class ViewWithMesh3D(
    var mesh: Mesh3D,
    var skeleton: Skeleton3D? = null
) : View3D() {

    private val uniformValues = AGUniformValues()
    private val rs = AGDepthAndFrontFace.DEFAULT.withDepthFunc(depthFunc = AGCompareMode.LESS_EQUAL)
    //private val rs = AGRenderState(depthFunc = AGCompareMode.ALWAYS)

    private val tempMat1 = Matrix3D()
    private val tempMat2 = Matrix3D()
    private val tempMat3 = Matrix3D()

    protected open fun prepareExtraModelMatrix(mat: Matrix3D) {
        mat.identity()
    }

    fun AGUniformValues.setMaterialLight(
        ctx: RenderContext3D,
        uniform: Shaders3D.MaterialLightUniform,
        actual: Material3D.Light
    ) {
        when (actual) {
            is Material3D.LightColor -> {
                this[uniform.u_color] = actual.colorVec
            }
            is Material3D.LightTexture -> {
                actual.textureUnit.texture = actual.bitmap?.let { ctx.rctx.agBitmapTextureManager.getTextureBase(it).base }
                actual.textureUnit.linear = true
                this[uniform.u_texUnit] = actual.textureUnit
            }
        }
    }

    private val identity = Matrix3D()
    private val identityInv = identity.clone().invert()

    override fun render(ctx: RenderContext3D) {
        val ag = ctx.ag

        // @TODO: We should have a managed object for index and vertex buffers like Bitmap -> Texture
        // @TODO:   that handles this automatically. So they are released and allocated automatically on the GPU
        ctx.dynamicIndexBufferPool.alloc { indexBuffer ->
            ctx.useDynamicVertexData(mesh.vertexBuffers) { vertexData ->
                indexBuffer.upload(mesh.indexBuffer)
                //tempMat2.invert()
                //tempMat3.multiply(ctx.cameraMatInv, this.localTransform.matrix)
                //tempMat3.multiply(ctx.cameraMatInv, Matrix3D().invert(this.localTransform.matrix))
                //tempMat3.multiply(this.localTransform.matrix, ctx.cameraMat)

                Shaders3D.apply {
                    val meshMaterial = mesh.material
                    ag.draw(
                        ctx.rctx.currentFrameBuffer,
                        vertexData = vertexData,
                        indices = indexBuffer,
                        indexType = mesh.indexType,
                        drawType = mesh.drawType,
                        program = mesh.program ?: ctx.shaders.getProgram3D(
                            ctx.lights.size.clamp(0, 4),
                            mesh.maxWeights,
                            meshMaterial,
                            mesh.hasTexture
                        ),
                        vertexCount = mesh.vertexCount,
                        blending = AGBlending.NONE,
                        //vertexCount = 6 * 6,
                        uniforms = uniformValues.apply {
                            this[u_ProjMat] = ctx.projCameraMat
                            this[u_ViewMat] = transform.globalMatrix
                            this[u_ModMat] = tempMat2.multiply(tempMat1.apply { prepareExtraModelMatrix(this) }, modelMat)
                            //this[u_NormMat] = tempMat3.multiply(tempMat2, localTransform.matrix).invert().transpose()
                            this[u_NormMat] = tempMat3.multiply(tempMat2, transform.globalMatrix)//.invert()

                            this[u_Shininess] = meshMaterial?.shininess ?: 0.5f
                            this[u_IndexOfRefraction] = meshMaterial?.indexOfRefraction ?: 1f

                            if (meshMaterial != null) {
                                setMaterialLight(ctx, ambient, meshMaterial.ambient)
                                setMaterialLight(ctx, diffuse, meshMaterial.diffuse)
                                setMaterialLight(ctx, emission, meshMaterial.emission)
                                setMaterialLight(ctx, specular, meshMaterial.specular)
                            }

                            val skeleton = this@ViewWithMesh3D.skeleton
                            val skin = mesh.skin
                            this[u_BindShapeMatrix] = identity
                            this[u_BindShapeMatrixInv] = identity
                            //println("skeleton: $skeleton, skin: $skin")
                            if (skeleton != null && skin != null) {
                                skin.bones.fastForEach { bone ->
                                    val jointsBySid = skeleton.jointsBySid
                                    val joint = jointsBySid[bone.name]
                                    if (joint != null) {
                                        skin.matrices[bone.index].multiply(
                                            joint.transform.globalMatrix,
                                            joint.poseMatrixInv
                                        )
                                    } else {
                                        error("Can't find joint with name '${bone.name}'")
                                    }

                                }
                                this[u_BindShapeMatrix] = skin.bindShapeMatrix
                                this[u_BindShapeMatrixInv] = skin.bindShapeMatrixInv

                                this[u_BoneMats] = skin.matrices
                            }

                            this[u_AmbientColor] = ctx.ambientColor

                            ctx.lights.fastForEachWithIndex { index, light: Light3D ->
                                val lightColor = light.color
                                this[lights[index].u_sourcePos] = light.transform.translation
                                this[lights[index].u_color] =
                                    light.colorVec.setTo(lightColor.rf, lightColor.gf, lightColor.bf, 1f)
                                this[lights[index].u_attenuation] = light.attenuationVec.setTo(
                                    light.constantAttenuation,
                                    light.linearAttenuation,
                                    light.quadraticAttenuation
                                )
                            }
                        },
                        depthAndFrontFace = rs
                    )
                }
            }
        }
    }
}
