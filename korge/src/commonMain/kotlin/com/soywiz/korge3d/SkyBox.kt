package com.soywiz.korge3d

import com.soywiz.korag.AG
import com.soywiz.korag.AGOpengl
import com.soywiz.korag.shader.*
import com.soywiz.korge3d.internal.toFBuffer
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.format.readNativeImage
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.Vector3D
import com.soywiz.korma.geom.translate

interface CubeMap {
    val right: NativeImage
    val left: NativeImage
    val top: NativeImage
    val bottom: NativeImage
    val back: NativeImage
    val front: NativeImage

    fun faces(): List<NativeImage> = listOf(right, left, top, bottom, back, front)

}

suspend fun cubeMapFromResourceDirectory(directory: String, ext: String): CubeMap {
    val rightImage = resourcesVfs.get("$directory/right.$ext").readNativeImage()
    val leftImage = resourcesVfs.get("$directory/left.$ext").readNativeImage()
    val topImage = resourcesVfs.get("$directory/top.$ext").readNativeImage()
    val bottomImage = resourcesVfs.get("$directory/bottom.$ext").readNativeImage()
    val backImage = resourcesVfs.get("$directory/back.$ext").readNativeImage()
    val frontImage = resourcesVfs.get("$directory/front.$ext").readNativeImage()
    return CubeMapSimple(rightImage, leftImage, topImage, bottomImage, backImage, frontImage)
}

class CubeMapSimple(
    override val right: NativeImage,
    override val left: NativeImage,
    override val top: NativeImage,
    override val bottom: NativeImage,
    override val back: NativeImage,
    override val front: NativeImage
) : CubeMap {

}

@Korge3DExperimental
fun Stage3D.skyBox(cubemap: CubeMap, centerX: Float = 0f, centerY: Float = 0f, centerZ: Float = 0f): SkyBox {
    return SkyBox(cubemap, Vector3D(centerX, centerY, centerZ)).addTo(this)
}

@Korge3DExperimental
class SkyBox(
    val cubemap: CubeMap,
    val center: Vector3D
) : View3D() {
    /*
        inner class Texture3DDrawer {
            val VERTEX_COUNT = 4
            val vertices = createBuffer(AG.Buffer.Kind.VERTEX)
            val vertexLayout = VertexLayout(DefaultShaders.a_Pos, DefaultShaders.a_Tex)
            val verticesData = FBuffer(VERTEX_COUNT * vertexLayout.totalSize)
            val program = Program(VertexShader {
                DefaultShaders.apply {
                    v_Tex setTo a_Tex
                    out setTo vec4(a_Pos, 0f.lit, 1f.lit)
                }
            }, FragmentShader {
                DefaultShaders.apply {
                    //out setTo vec4(1f, 1f, 0f, 1f)
                    out setTo texture2D(u_Tex, v_Tex["xy"])
                }
            })
            val uniforms = AG.UniformValues()

            fun setVertex(n: Int, px: Float, py: Float, tx: Float, ty: Float) {
                val offset = n * 4
                verticesData.setAlignedFloat32(offset + 0, px)
                verticesData.setAlignedFloat32(offset + 1, py)
                verticesData.setAlignedFloat32(offset + 2, tx)
                verticesData.setAlignedFloat32(offset + 3, ty)
            }

            fun draw(tex: AG.Texture, left: Float, top: Float, right: Float, bottom: Float) {
                //tex.upload(Bitmap32(32, 32) { x, y -> Colors.RED })
                uniforms[DefaultShaders.u_Tex] = AG.TextureUnit(tex)

                val texLeft = -1f
                val texRight = +1f
                val texTop = -1f
                val texBottom = +1f

                setVertex(0, left, top, texLeft, texTop)
                setVertex(1, right, top, texRight, texTop)
                setVertex(2, left, bottom, texLeft, texBottom)
                setVertex(3, right, bottom, texRight, texBottom)

                vertices.upload(verticesData)
                draw(
                    vertices = vertices,
                    program = program,
                    type = AG.DrawType.TRIANGLE_STRIP,
                    vertexLayout = vertexLayout,
                    vertexCount = 4,
                    uniforms = uniforms,
                    blending = AG.Blending.NONE
                )
            }
        }
       */
    companion object {
        val a_pos = Attribute("a_Pos", VarType.Float3, normalized = false)
        val u_ProjMat = Uniform("u_ProjMat", VarType.Mat4)
        val u_ViewMat = Uniform("u_ViewMat", VarType.Mat4)
        val v_TexCoords = Varying("v_TexCoords", VarType.Float3)
        val u_SkyBox = Uniform("u_SkyBox", VarType.SamplerCube)
        val layout = VertexLayout(a_pos)
        val skyboxVertices = floatArrayOf( // positions
            -1f, -1f, +1f, // 0, left, bottom, back
            +1f, -1f, +1f, // 1, right, bottom, back
            -1f, +1f, +1f, // 2, left, top, back
            +1f, +1f, +1f, // 3, right, top, back
            -1f, -1f, -1f, // 4, left, bottom, front
            +1f, -1f, -1f, // 5, right, bottom, front
            -1f, +1f, -1f, // 6, left, top, front
            +1f, +1f, -1f  // 7, right, top, front
        )
        val skyboxIndices = shortArrayOf(
            1, 5, 3, 3, 5, 7, // right
            0, 2, 4, 4, 2, 6, // left
            2, 3, 6, 3, 6, 7, // top
            1, 0, 5, 5, 0, 4, // bottom
            1, 3, 0, 3, 0, 2, // back
            4, 6, 5, 5, 6, 7  // front
        )
        val skyBoxProgram = Program(VertexShader {
            SET(v_TexCoords, a_pos)
            val temp = createTemp(VarType.Float4)
            SET(temp, u_ProjMat * u_ViewMat * vec4(a_pos, 1f.lit))
            SET(out, temp["xyww"])
        }, FragmentShader {
            SET(out, func("textureCube", u_SkyBox, v_TexCoords))
        })
    }

    private val uniformValues = AG.UniformValues()
    private val rs = AG.RenderState(depthMask = false, depthFunc = AG.CompareMode.LESS_EQUAL)

    private var init = true
    private val cubeMapTexUnit = AG.TextureUnit()

    override fun render(ctx: RenderContext3D) {
        //println("----------- SkyBox render ---------------")
        val ag = ctx.ag

        if (init) {
            val faces = cubemap.faces()
            val texCubeMap = ag.createTexture(AG.TextureTargetKind.TEXTURE_CUBE_MAP) { gl ->
                gl.bindTexture(gl.TEXTURE_CUBE_MAP, (this as AGOpengl.TextureGeneric).texRef)
                for (i in 0..5) {
                    val face = faces[i]
                    val tgt = gl.TEXTURE_CUBE_MAP_POSITIVE_X + i
                    gl.texImage2D(tgt, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, face)
                }
                gl.texParameteri(gl.TEXTURE_CUBE_MAP, gl.TEXTURE_MIN_FILTER, gl.LINEAR)
                gl.texParameteri(gl.TEXTURE_CUBE_MAP, gl.TEXTURE_MAG_FILTER, gl.LINEAR)
                gl.texParameteri(gl.TEXTURE_CUBE_MAP, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE)
                gl.texParameteri(gl.TEXTURE_CUBE_MAP, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE)
                gl.texParameteri(gl.TEXTURE_CUBE_MAP, gl.TEXTURE_WRAP_R, gl.CLAMP_TO_EDGE)
            }
            cubeMapTexUnit.texture = texCubeMap
            init = false
        }

        val indexBuffer = ag.createIndexBuffer()
        ctx.dynamicVertexBufferPool.alloc { vertexBuffer ->
            vertexBuffer.upload(skyboxVertices)
            indexBuffer.upload(skyboxIndices)
            val projection = ctx.projCameraMat
            val view = transform.globalMatrix
            // remove translation from the view matrix
            val viewNoTrans = view.clone()
                .setColumn(3, 0f, 0f, 0f, 0f)
                .setRow(3, 0f, 0f, 0f, 0f)
                .translate(center)
            ag.draw(
                vertices = vertexBuffer,
                type = AG.DrawType.TRIANGLES,
                program = skyBoxProgram,
                vertexLayout = layout,
                vertexCount = 36,
                indices = indexBuffer,
                indexType = AG.IndexType.USHORT,
                blending = AG.Blending.NONE,
                uniforms = uniformValues.apply {
                    this[u_ProjMat] = projection
                    this[u_ViewMat] = viewNoTrans
                    this[u_SkyBox] = cubeMapTexUnit
                },
                renderState = rs
            )
        }
        indexBuffer.close()
        //println("-----------------------------------------")
    }

}
