@file:OptIn(KorgeInternal::class)

package com.soywiz.korge.render

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.vector.*
import kotlin.native.concurrent.ThreadLocal

/** Creates/gets a [LineRenderBatcher] associated to [this] [RenderContext] */
@Deprecated("USe useDebugLineRenderContext instead")
@ThreadLocal
val RenderContext.debugLineRenderContext: LineRenderBatcher by Extra.PropertyThis<RenderContext, LineRenderBatcher> { LineRenderBatcher(this) }

@Suppress("DEPRECATION")
inline fun RenderContext.useLineBatcher(matrix: MMatrix? = null, block: (LineRenderBatcher) -> Unit) = debugLineRenderContext.use { batcher ->
    debugLineRenderContext.drawWithGlobalMatrix(matrix) {
        block(batcher)
    }
}

typealias DebugLineRenderContext = LineRenderBatcher

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
class LineRenderBatcher(
    @property:KorgeInternal
    val ctx: RenderContext
) {
    inline fun use(block: (LineRenderBatcher) -> Unit) = ctx.useBatcher(this, block)

    val beforeFlush = Signal<LineRenderBatcher>()

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
            //SET(out, vec4(1f.lit, 1f.lit, 0f.lit, 1f.lit))
            SET(out, v_Col)
        }
    }

    private val vertexBuffer = AGBuffer()
    private val vertexData = AGVertexArrayObject(AGVertexData(LAYOUT, vertexBuffer))
    private val program = Program(VERTEX, FRAGMENT)
    private val maxVertexCount = 1024
    private val vertices = Buffer.allocDirect(6 * 4 * maxVertexCount)
    private val tempRect = MRectangle()
    @PublishedApi
    internal val viewMat = MMatrix3D()
    @PublishedApi
    internal val tempViewMat = Pool { MMatrix3D() }
    @PublishedApi
    internal var vertexCount = 0
    @PublishedApi
    internal var vertexPos = 0

    fun line(p0: Point, p1: Point, color0: RGBA = color, color1: RGBA = color0, m: MMatrix = currentMatrix) =
        line(p0.x, p0.y, p1.x, p1.y, color, color1, m)

    /** Draw a line from [x0],[y0] to [x1],[y1] */
    fun line(x0: Float, y0: Float, x1: Float, y1: Float, color0: RGBA = color, color1: RGBA = color0, m: MMatrix = currentMatrix) {
        if (vertexCount >= maxVertexCount - 2) {
            flush()
        }
        addVertex(x0, y0, color0, m)
        addVertex(x1, y1, color1, m)
    }
    fun line(x0: Double, y0: Double, x1: Double, y1: Double, color0: RGBA = color, color1: RGBA = color0, m: MMatrix = currentMatrix) = line(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat(), color0, color1, m)
    fun line(x0: Int, y0: Int, x1: Int, y1: Int, color0: RGBA = color, color1: RGBA = color0, m: MMatrix = currentMatrix) = line(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat(), color0, color1, m)

    fun drawVector(path: VectorPath, m: MMatrix = currentMatrix) {
        var lastPos = Point()
        path.emitPoints2 { p, move ->
            if (!move) {
                line(lastPos, p, m = m)
            }
            lastPos = p
        }
    }

    inline fun drawVector(m: MMatrix = currentMatrix, block: VectorBuilder.() -> Unit) {
        drawVector(VectorPath().apply(block), m = m)
    }

    inline fun drawVector(color: RGBA, m: MMatrix = currentMatrix, block: VectorBuilder.() -> Unit) {
        color(color) {
            drawVector(m, block)
        }
    }

    /** Prepares for drawing a set of lines with the specified [matrix]. It flushes all other contexts, and the set [matrix]. */
    inline fun <T> draw(matrix: MMatrix, body: () -> T): T {
        ctx.flush()
        return tempViewMat.alloc { temp ->
            temp.copyFrom(viewMat)
            matrix.toMatrix4(viewMat)
            try {
                body()
            } finally {
                flush()
                viewMat.copyFrom(temp)
            }
        }
    }

    var blendMode: BlendMode = BlendMode.NORMAL

    inline fun <T> blending(blending: BlendMode, block: () -> T): T {
        val doUpdate = this.blendMode !== blending
        val old = this.blendMode
        try {
            if (doUpdate) {
                ctx.flush()
                this.blendMode = blending
            }
            return block()
        } finally {
            if (doUpdate) {
                ctx.flush()
            }
            this.blendMode = old
        }
    }

    /** Actually flushes all the pending lines. Shouldn't be called manually. You should call the [draw] method instead. */
    @KorgeInternal
    fun flush() {
        //println("BLENDING=$blending")
        if (vertexCount > 0) {
            beforeFlush(this)
            vertexBuffer.upload(vertices, 0, vertexPos * 4)
            ctx.updateStandardUniforms()
            //projMat.setToOrtho(tempRect.setBounds(0, 0, ag.backWidth, ag.backHeight), -1f, 1f)

            ag.draw(
                ctx.currentFrameBuffer,
                vertexData,
                program = program,
                drawType = AGDrawType.LINES,
                vertexCount = vertexCount,
                uniformBlocks = ctx.createCurrentUniformsRef(program),
                blending = blendMode.factors
            )
        }
        vertexCount = 0
        vertexPos = 0
    }

    @PublishedApi
    internal val currentMatrix: MMatrix = MMatrix()

    inline fun <T> drawWithGlobalMatrix(matrix: MMatrix?, block: () -> T): T = currentMatrix.keepMatrix {
        if (matrix != null) it.copyFrom(matrix)
        block()
    }

    private fun addVertex(x: Float, y: Float, color: RGBA = this.color, m: MMatrix = currentMatrix) {
        vertices.setFloat32(vertexPos + 0, m.transformXf(x, y))
        vertices.setFloat32(vertexPos + 1, m.transformYf(x, y))
        vertices.setInt32(vertexPos + 2, color.value)
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
