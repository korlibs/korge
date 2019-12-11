package com.soywiz.korge.render

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korma.geom.*

val RenderContext.debugLineRenderContext: DebugLineRenderContext by Extra.PropertyThis<RenderContext, DebugLineRenderContext> { DebugLineRenderContext(this) }
class DebugLineRenderContext(val ctx: RenderContext) {
    init {
        ctx.flushers.add { flush() }
    }

    private val ag: AG = ctx.ag
    val LAYOUT = VertexLayout(DefaultShaders.a_Pos)
    val VERTEX = VertexShader {
        DefaultShaders.apply {
            SET(out, (u_ProjMat * u_ViewMat) * vec4(a_Pos, 0f.lit, 1f.lit))
        }
    }

    val FRAGMENT = FragmentShader {
        DefaultShaders.apply {
            out set vec4(1f.lit, 1f.lit, 0f.lit, 1f.lit)
        }
    }

    private val vertexBuffer = ag.createVertexBuffer()
    private val program = Program(VERTEX, FRAGMENT)
    private val maxVertexCount = 1024
    private val vertices = FBuffer.alloc(6 * 4 * maxVertexCount)
    private val tempRect = Rectangle()
    private val projMat = Matrix3D()
    @PublishedApi
    internal val viewMat = Matrix3D()
    private var vertexCount = 0
    private var vertexPos = 0

    internal val uniforms by lazy {
        AG.UniformValues(
            DefaultShaders.u_ProjMat to projMat,
            DefaultShaders.u_ViewMat to viewMat
        )
    }

    private fun addVertex(x: Float, y: Float) {
        vertices.setAlignedFloat32(vertexPos++, x)
        vertices.setAlignedFloat32(vertexPos++, y)
        vertexCount++
    }

    fun line(x0: Float, y0: Float, x1: Float, y1: Float) {
        if (vertexCount >= maxVertexCount - 2) {
            flush()
        }
        addVertex(x0, y0)
        addVertex(x1, y1)
    }

    inline fun line(x0: Number, y0: Number, x1: Number, y1: Number) = line(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat())

    inline fun <T> draw(matrix: Matrix, body: () -> T): T {
        ctx.flush()
        matrix.toMatrix3D(viewMat)
        try {
            return body()
        } finally {
            flush()
        }
    }

    fun flush() {
        if (vertexCount > 0) {
            vertexBuffer.upload(vertices, 0, vertexPos * 4)
            projMat.setToOrtho(tempRect.setBounds(0, 0, ag.backWidth, ag.backHeight), -1f, 1f)

            ag.draw(
                vertices = vertexBuffer,
                program = program,
                type = AG.DrawType.LINES,
                vertexLayout = LAYOUT,
                vertexCount = vertexCount,
                uniforms = uniforms
            )
        }
        vertexCount = 0
        vertexPos = 0
    }
}
