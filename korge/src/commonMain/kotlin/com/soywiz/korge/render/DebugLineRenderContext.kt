package com.soywiz.korge.render

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.internal.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.vector.*

/** Creates/gets a [DebugLineRenderContext] associated to [this] [RenderContext] */
val RenderContext.debugLineRenderContext: DebugLineRenderContext by Extra.PropertyThis<RenderContext, DebugLineRenderContext> { DebugLineRenderContext(this) }

/**
 * A context that allows to draw lines using [AG] (Accelerated Graphics).
 *
 * You should use it by calling:
 *
 * ctx.draw(matrix) {
 *  ctx.line(0, 0, 100, 100)
 *  ctx.line(100, 100, 0, 100)
 *  // ...
 * }
 */
class DebugLineRenderContext(
    @KorgeInternal
    val ctx: RenderContext
) {
    init {
        ctx.flushers.add { flush() }
    }

    private val ag: AG = ctx.ag

    var color: RGBA = Colors.YELLOW

    @KorgeInternal
    val LAYOUT = VertexLayout(DefaultShaders.a_Pos, DefaultShaders.a_Col)

    @KorgeInternal
    val VERTEX = VertexShader {
        DefaultShaders.apply {
            SET(out, (u_ProjMat * u_ViewMat) * vec4(a_Pos, 0f.lit, 1f.lit))
            SET(v_Col, a_Col)
        }
    }

    @KorgeInternal
    val FRAGMENT = FragmentShader {
        DefaultShaders.apply {
            //out set vec4(1f.lit, 1f.lit, 0f.lit, 1f.lit)
            out set v_Col
        }
    }

    internal val uniforms by lazy {
        AG.UniformValues(
            DefaultShaders.u_ProjMat to projMat,
            DefaultShaders.u_ViewMat to viewMat
        )
    }

    private val vertexBuffer = ag.createVertexBuffer()
    private val program = Program(VERTEX, FRAGMENT)
    private val maxVertexCount = 1024
    private val vertices = FBuffer.alloc(6 * 4 * maxVertexCount)
    private val tempRect = Rectangle()
    private val projMat = Matrix3D()
    @PublishedApi
    internal val viewMat = Matrix3D()
    @PublishedApi
    internal val tempViewMat = Pool { Matrix3D() }
    private var vertexCount = 0
    private var vertexPos = 0

    /** Draw a line from [x0],[y0] to [x1],[y1] */
    fun line(x0: Float, y0: Float, x1: Float, y1: Float) {
        if (vertexCount >= maxVertexCount - 2) {
            flush()
        }
        addVertex(x0, y0)
        addVertex(x1, y1)
    }

    fun drawVector(path: VectorPath) {
        var lastX = 0.0
        var lastY = 0.0
        path.emitPoints2 { x, y, move ->
            if (!move) {
                line(lastX, lastY, x, y)
            }
            lastX = x
            lastY = y
        }
    }

    inline fun drawVector(block: VectorBuilder.() -> Unit) {
        drawVector(VectorPath().apply(block))
    }

    inline fun drawVector(color: RGBA, block: VectorBuilder.() -> Unit) {
        color(color) {
            drawVector(block)
        }
    }

    /** Draw a line from [x0],[y0] to [x1],[y1] */
    @Deprecated("Kotlin/Native boxes inline+Number")
    inline fun line(x0: Number, y0: Number, x1: Number, y1: Number) = line(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat())
    fun line(x0: Double, y0: Double, x1: Double, y1: Double) = line(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat())

    /** Prepares for drawing a set of lines with the specified [matrix]. It flushes all other contexts, and the set [matrix]. */
    inline fun <T> draw(matrix: Matrix, body: () -> T): T {
        ctx.flush()
        val temp = tempViewMat.alloc()
        temp.copyFrom(viewMat)
        matrix.toMatrix3D(viewMat)
        try {
            return body()
        } finally {
            flush()
            viewMat.copyFrom(temp)
        }
    }

    /** Actually flushes all the pending lines. Shouldn't be called manually. You should call the [draw] method instead. */
    @KorgeInternal
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

    private fun addVertex(x: Float, y: Float) {
        vertices.setAlignedFloat32(vertexPos + 0, x)
        vertices.setAlignedFloat32(vertexPos + 1, y)
        vertices.setAlignedInt32(vertexPos + 2, color.value)
        vertexPos += LAYOUT.totalSize / Int.SIZE_BYTES
        vertexCount++
    }

    inline fun color(color: RGBA, block: () -> Unit) {
        val oldColor = this.color
        this.color = color
        try {
            block()
        } finally {
            this.color = oldColor
        }
    }
}
