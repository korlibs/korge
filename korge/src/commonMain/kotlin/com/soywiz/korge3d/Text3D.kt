package com.soywiz.korge3d

import com.soywiz.kds.iterators.fastForEachWithIndex
import com.soywiz.kmem.clamp
import com.soywiz.korag.AG
import com.soywiz.korge.ui.*
import com.soywiz.korim.font.BitmapFont
import com.soywiz.korma.geom.Matrix3D
import com.soywiz.korma.geom.Vector3D
import com.soywiz.korma.geom.invert

@Korge3DExperimental
fun Container3D.text3D(str: String, v1: Vector3D, v2: Vector3D, v3: Vector3D, v4: Vector3D): Text3D =
    Text3D(str, v1, v2, v3, v4).addTo(this)

@Korge3DExperimental
class Text3D(
    var text: String,
    var v1: Vector3D, var v2: Vector3D, var v3: Vector3D, var v4: Vector3D
) : View3D() {

    var font: BitmapFont = DefaultUIBitmapFont

    protected open fun prepareExtraModelMatrix(mat: Matrix3D) {
        mat.identity()
    }

    private val uniformValues = AG.UniformValues()
    private val rs = AG.RenderState(depthFunc = AG.CompareMode.LESS_EQUAL)
    private val tempMat1 = Matrix3D()
    private val tempMat2 = Matrix3D()
    private val tempMat3 = Matrix3D()
    private val identity = Matrix3D()


    fun AG.UniformValues.setMaterialLight(
        ctx: RenderContext3D,
        uniform: Shaders3D.MaterialLightUniform,
        actual: Material3D.Light
    ) {
        when (actual) {
            is Material3D.LightColor -> {
                this[uniform.u_color] = actual.colorVec
            }
            is Material3D.LightTexture -> {
                actual.textureUnit.texture =
                    actual.bitmap?.let { ctx.rctx.agBitmapTextureManager.getTextureBase(it).base }
                actual.textureUnit.linear = true
                this[uniform.u_texUnit] = actual.textureUnit
            }
        }
    }

    override fun render(ctx: RenderContext3D) {
        val ag = ctx.ag
        val mesh = this.createMesh(ctx)

        ctx.dynamicVertexBufferPool.alloc { vertexBuffer ->
            //vertexBuffer.upload(mesh.data)
            vertexBuffer.upload(mesh.vertexBuffer)

            //tempMat2.invert()
            //tempMat3.multiply(ctx.cameraMatInv, this.localTransform.matrix)
            //tempMat3.multiply(ctx.cameraMatInv, Matrix3D().invert(this.localTransform.matrix))
            //tempMat3.multiply(this.localTransform.matrix, ctx.cameraMat)

            Shaders3D.apply {
                val meshMaterial = mesh.material
                ag.draw(
                    vertexBuffer,
                    type = mesh.drawType,
                    program = mesh.program ?: ctx.shaders.getProgram3D(
                        ctx.lights.size.clamp(0, 4),
                        mesh.maxWeights,
                        meshMaterial,
                        mesh.hasTexture
                    ),
                    vertexLayout = mesh.layout,
                    vertexCount = mesh.vertexCount,
                    blending = AG.Blending.NONE,
                    //vertexCount = 6 * 6,
                    uniforms = uniformValues.apply {
                        this[u_ProjMat] = ctx.projCameraMat
                        this[u_ViewMat] = transform.globalMatrix
                        this[u_ModMat] = tempMat2.multiply(tempMat1.apply { prepareExtraModelMatrix(this) }, modelMat)
                        //this[u_NormMat] = tempMat3.multiply(tempMat2, localTransform.matrix).invert().transpose()
                        this[u_NormMat] = tempMat3.multiply(tempMat2, transform.globalMatrix).invert()

                        this[u_Shininess] = meshMaterial?.shininess ?: 0.5f
                        this[u_IndexOfRefraction] = meshMaterial?.indexOfRefraction ?: 1f

                        if (meshMaterial != null) {
                            setMaterialLight(ctx, ambient, meshMaterial.ambient)
                            setMaterialLight(ctx, diffuse, meshMaterial.diffuse)
                            setMaterialLight(ctx, emission, meshMaterial.emission)
                            setMaterialLight(ctx, specular, meshMaterial.specular)
                        }

                        this[u_BindShapeMatrix] = identity
                        this[u_BindShapeMatrixInv] = identity

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
                    renderState = rs
                )
            }
        }
    }

    private fun createMesh(ctx: RenderContext3D): Mesh3D {
        val str = this.text

        val meshBuilder3D = MeshBuilder3D()

        var dx = 0.0
        var dy = 0.0
        val dv1 = Vector3D(v1.x, v1.y, v1.z, v1.w)
        val dv2 = Vector3D(v2.x, v2.y, v3.z, v2.w)
        val dv3 = Vector3D(v3.x, v3.y, v3.z, v3.w)
        val dv4 = Vector3D(v4.x, v4.y, v4.z, v4.w)
        val t1 = Vector3D()
        val t2 = Vector3D()
        val t3 = Vector3D()
        val t4 = Vector3D()
        for (n in str.indices) {
            val c1 = str[n].toInt()
            if (c1 == '\n'.toInt()) {
                dx = 0.0
                dy += font.fontSize
                continue
            }
            val c2 = str.getOrElse(n + 1) { ' ' }.toInt()
            val glyph = font[c1]
            val bms = glyph.texture
            val tex = ctx.rctx.getTex(bms)
            val w = tex.width //(tex.x1 - tex.x0) * font.fontSize.toFloat()
            val h = tex.height //(tex.y1-tex.y0) * font.fontSize.toFloat()
            dv1.x = (dx + glyph.xoffset).toFloat()
            dv1.y = (dy + glyph.yoffset).toFloat()
            dv2.x = dv1.x + w
            dv2.y = dv1.y
            dv3.x = dv1.x + w
            dv3.y = dv1.y + h
            dv4.x = dv1.x
            dv4.y = dv1.y + h
            t1.x = tex.left.toFloat()
            t1.y = tex.top.toFloat()
            t2.x = tex.right.toFloat()
            t2.y = tex.top.toFloat()
            t3.x = tex.right.toFloat()
            t3.y = tex.bottom.toFloat()
            t4.x = tex.left.toFloat()
            t4.y = tex.bottom.toFloat()
            meshBuilder3D.faceRectangle(
                dv1, dv2, dv3, dv4,
                t1, t2, t3, t4
            )

            val kerningOffset = font.kernings[BitmapFont.Kerning.buildKey(c1, c2)]?.amount ?: 0
            dx += glyph.xadvance + kerningOffset
        }
        return meshBuilder3D.build()
    }

}
