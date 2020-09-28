package com.soywiz.korge3d

import com.soywiz.kds.iterators.fastForEachWithIndex
import com.soywiz.kmem.clamp
import com.soywiz.korag.AG
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.VfsFile
import com.soywiz.korma.geom.Matrix3D
import com.soywiz.korma.geom.Vector3D
import kotlin.math.max

interface HeightMap {
    operator fun get(x: Float, z: Float): Float
}

class HeightMapConstant(val height: Float) : HeightMap {
    override fun get(x: Float, z: Float): Float = height
}

class HeightMapBitmap(val bitmap:Bitmap) : HeightMap{
    override fun get(x: Float, z: Float): Float {
        val x1 = when {
            x < 0 -> 0.0
            x > bitmap.width -> bitmap.width.toDouble()
            else -> x.toDouble()
        }
        val z1 = when {
            z < 0 -> 0.0
            z > bitmap.height -> bitmap.height.toDouble()
            else -> z.toDouble()
        }
        return bitmap.getRgbaSampled(x1, z1).rgb.toFloat()
    }
}

@Korge3DExperimental
fun Container3D.terrain(
    centerX: Float = 0f,
    centerZ: Float = 0f,
    width: Float = 100.0f,
    depth: Float = 100.0f,
    heightMap: HeightMap = HeightMapConstant(0f),
    heightScale:Float = 1f
): Terrain3D {
    return Terrain3D(centerX, centerZ, width, depth, heightMap,heightScale).addTo(this)
}

@Korge3DExperimental
class Terrain3D(
    val centerX: Float,
    val centerZ: Float,
    val width: Float,
    val depth: Float,
    var heightMap: HeightMap,
    val heightScale:Float
) : View3D() {

    var stepX = 1f
    var stepZ = 1f

    private val meshBuilder3D = MeshBuilder3D(AG.DrawType.TRIANGLE_STRIP)
    private var mesh: Mesh3D = createMesh()


    private val uniformValues = AG.UniformValues()
    private val rs = AG.RenderState(depthFunc = AG.CompareMode.LESS_EQUAL)
    private val tempMat1 = Matrix3D()
    private val tempMat2 = Matrix3D()
    private val tempMat3 = Matrix3D()

    private fun getHeight(x: Float, z: Float) : Float  = heightMap[x,z] * heightScale

    fun calcNormal(x: Float, z: Float): Vector3D {
        val hl = getHeight(x - 1, z)
        val hr = getHeight(x + 1, z)
        val hf = getHeight(x, z + 1)
        val hb = getHeight(x, z - 1)
        val v = Vector3D(hl - hr, 2f, hb - hf).normalize()
        return v
    }

    fun createMesh(): Mesh3D {
        meshBuilder3D.reset()
        val vec = Vector3D()
        val tex = Vector3D() //TODO:
        var z = 0f
        val adjX = (width / 2) + centerX
        val adjZ = (depth / 2) + centerZ
        // Vertices
        while (z < depth) {
            var x = 0f
            while (x < width) {
                val norm = calcNormal(x, z)
                vec.setTo(x - adjX, getHeight(x, z), z - adjZ)
                meshBuilder3D.addVertex(vec, norm, tex)
                x += stepX
            }
            z += stepZ
        }

        //Indices
        val totalX: Int = (width / stepX).toInt()
        val totalZ: Int = (depth / stepZ).toInt()
        for (zi in 0 until totalZ - 1) {
            for (xi in 0 until totalX - 1) {
                val i1 = (zi * totalX) + xi
                val i2 = i1 + totalX
                meshBuilder3D.addIndices(i1, i2)
            }
            //add degenerate to indicate end of row
            val d1 = (zi * totalX) + totalX
            val d2 = ((zi + 1) * totalX) + 1
            meshBuilder3D.addIndices(d1, d2)
        }

        return meshBuilder3D.build()
    }

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
        val indexBuffer = ag.createIndexBuffer()
        ctx.dynamicVertexBufferPool.alloc { vertexBuffer ->
            vertexBuffer.upload(mesh.vertexBuffer)
            indexBuffer.upload(mesh.indexBuffer)
            Shaders3D.apply {
                val meshMaterial = mesh.material
                ag.draw(
                    vertices = vertexBuffer,
                    indices = indexBuffer,
                    indexType = mesh.indexType,
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
                        this[u_ModMat] = modelMat
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

}
