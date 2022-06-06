package com.soywiz.korge.view.debug

import com.soywiz.kds.fastArrayListOf
import com.soywiz.korag.AG
import com.soywiz.korag.DefaultShaders
import com.soywiz.korag.shader.FragmentShader
import com.soywiz.korag.shader.Uniform
import com.soywiz.korag.shader.VarType
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
import com.soywiz.korma.geom.vectorArrayListOf

inline fun Container.debugVertexView(
    points: IVectorArrayList = VectorArrayList(5),
    color: RGBA = Colors.WHITE,
    type: AG.DrawType = AG.DrawType.TRIANGLE_STRIP,
    callback: @ViewDslMarker DebugVertexView.() -> Unit = {}
): DebugVertexView = DebugVertexView(points, color, type).addTo(this, callback)

class DebugVertexView(points: IVectorArrayList, var color: RGBA = Colors.WHITE, type: AG.DrawType = AG.DrawType.TRIANGLE_STRIP) : View() {
    companion object {
        val u_Col: Uniform = Uniform("u_Col", VarType.Float4)
        val PROGRAM = DefaultShaders.PROGRAM_DEBUG_WITH_PROJ.copy(
            fragment = FragmentShader {
                SET(out, u_Col)
            }
        )
    }

    var points: IVectorArrayList = points
        set(value) {
            if (field !== value) {
                field = value
                updatedPoints()
            }
        }
    var type: AG.DrawType = type
    var buffer: FloatArray = floatArrayOf(0f, 0f, 100f, 0f, 0f, 100f, 100f, 100f)

    private fun updatedPoints() {
        this.buffer = FloatArray(points.size * 2)
        val buffer = this.buffer
        if (points.dimensions >= 5) {
            points.fastForEachGeneric {
                val x = this.get(it, 0).toFloat()
                val y = this.get(it, 1).toFloat()
                val dx = this.get(it, 2).toFloat()
                val dy = this.get(it, 3).toFloat()
                val scale = this.get(it, 4).toFloat()
                val px = x + dx * scale
                val py = y + dy * scale
                buffer[it * 2 + 0] = px
                buffer[it * 2 + 1] = py
            }
        } else {
            points.fastForEachGeneric {
                val x = this.get(it, 0).toFloat()
                val y = this.get(it, 1).toFloat()
                buffer[it * 2 + 0] = x
                buffer[it * 2 + 1] = y
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
        ctx.dynamicVertexBufferPool.alloc { vb ->
            vb.upload(this@DebugVertexView.buffer)
            ctx.ag.drawV2(
                fastArrayListOf(
                    AG.VertexData(vb, DefaultShaders.LAYOUT_DEBUG)
                ),
                type = type,
                program = PROGRAM,
                uniforms = this.uniforms,
                vertexCount = buffer.size / 2
            )
        }
    }
}
