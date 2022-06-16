package com.soywiz.korge.view.debug

import com.soywiz.kds.fastArrayListOf
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korag.AG
import com.soywiz.korag.DefaultShaders
import com.soywiz.korag.VertexShaderDefault
import com.soywiz.korag.shader.FragmentShader
import com.soywiz.korag.shader.Uniform
import com.soywiz.korag.shader.VarType
import com.soywiz.korag.shader.VertexShader
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View
import com.soywiz.korge.view.ViewDslMarker
import com.soywiz.korge.view.addTo
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.IVectorArrayList
import com.soywiz.korma.geom.VectorArrayList
import com.soywiz.korma.geom.fastForEachGeneric
import com.soywiz.korma.geom.toMatrix3D

inline fun Container.debugVertexView(
    pointsList: List<IVectorArrayList> = listOf(),
    color: RGBA = Colors.WHITE,
    type: AG.DrawType = AG.DrawType.TRIANGLE_STRIP,
    callback: @ViewDslMarker DebugVertexView.() -> Unit = {}
): DebugVertexView = DebugVertexView(pointsList, color, type).addTo(this, callback)

class DebugVertexView(pointsList: List<IVectorArrayList>, var color: RGBA = Colors.WHITE, type: AG.DrawType = AG.DrawType.TRIANGLE_STRIP) : View() {
    companion object {
        val u_Col: Uniform = Uniform("u_Col", VarType.Float4)
        val u_Matrix: Uniform = Uniform("u_Matrix", VarType.Mat4)
        val PROGRAM = DefaultShaders.PROGRAM_DEBUG_WITH_PROJ.copy(
            vertex = VertexShaderDefault {
                SET(out, u_ProjMat * u_ViewMat * u_Matrix * vec4(a_Pos, 0f.lit, 1f.lit))
            },
            fragment = FragmentShader {
                SET(out, u_Col)
            }
        )
    }

    var pointsList: List<IVectorArrayList> = pointsList
        set(value) {
            if (field !== value) {
                field = value
                updatedPoints()
            }
        }

    @Deprecated("Use pointsList instead")
    var points: IVectorArrayList
        get() = pointsList.firstOrNull() ?: VectorArrayList(5)
        set(value) { pointsList = listOf(value) }

    var type: AG.DrawType = type
    class Batch(val offset: Int, val count: Int)
    var buffer: FloatArray = floatArrayOf(0f, 0f, 100f, 0f, 0f, 100f, 100f, 100f)
    val batches = arrayListOf<Batch>()

    private fun updatedPoints() {
        this.buffer = FloatArray(pointsList.sumOf { it.size } * 2)
        val buffer = this.buffer
        var n = 0
        batches.clear()
        pointsList.fastForEach { points ->
            batches.add(Batch(n / 2, points.size))
            if (points.dimensions >= 5) {
                points.fastForEachGeneric {
                    val x = this.get(it, 0).toFloat()
                    val y = this.get(it, 1).toFloat()
                    val dx = this.get(it, 2).toFloat()
                    val dy = this.get(it, 3).toFloat()
                    val scale = this.get(it, 4).toFloat()
                    val px = x + dx * scale
                    val py = y + dy * scale
                    buffer[n++] = px
                    buffer[n++] = py
                }
            } else {
                points.fastForEachGeneric {
                    val x = this.get(it, 0).toFloat()
                    val y = this.get(it, 1).toFloat()
                    buffer[n++] = x
                    buffer[n++] = y
                }
            }
        }
    }

    init {
        updatedPoints()
    }

    private val uniforms: AG.UniformValues = AG.UniformValues()

    override fun renderInternal(ctx: RenderContext) {
        ctx.flush()
        ctx.updateStandardUniforms()
        this.uniforms.put(ctx.uniforms)
        this.uniforms.put(u_Col, color * renderColorMul)
        this.uniforms.put(u_Matrix, globalMatrix.toMatrix3D())

        ctx.dynamicVertexBufferPool.alloc { vb ->
            vb.upload(this@DebugVertexView.buffer)
            val vData = fastArrayListOf(
                AG.VertexData(vb, DefaultShaders.LAYOUT_DEBUG)
            )
            batches.fastForEach { batch ->
                ctx.ag.drawV2(
                    vData,
                    type = type,
                    program = PROGRAM,
                    uniforms = this.uniforms,
                    vertexCount = batch.count,
                    offset = batch.offset
                )
            }
        }
    }
}
