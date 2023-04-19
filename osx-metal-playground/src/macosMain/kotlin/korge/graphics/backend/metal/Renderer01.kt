@file:OptIn(ExperimentalUnsignedTypes::class)

package korge.graphics.backend.metal

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.math.geom.*
import korlibs.memory.*
import korlibs.metal.*
import kotlinx.cinterop.*
import platform.Metal.*
import platform.MetalKit.*

class Renderer01(view: MTKView, private val simple: Boolean) : Renderer() {

    private var ag = AGMetal(view)
    object uniformBlock : UniformBlock(fixedLocation = 0) {
        val u_ProjMat by mat4()
    }

    private val vertexShader = VertexShader {
        SET(DefaultShaders.v_Col, DefaultShaders.a_Col)
        //SET(DefaultShaders.v_Col, vec4(1f.lit, 1f.lit, 1f.lit, 1f.lit))
        //SET(out, vec4(DefaultShaders.a_Pos, 0f.lit, 1f.lit))
        SET(out, uniformBlock.u_ProjMat /* DefaultShaders.u_ViewMat*/ * vec4(DefaultShaders.a_Pos, 0f.lit, 1f.lit))
        //SET(out, vec4(DefaultShaders.a_Pos, 0f.lit, 1f.lit))

        //SET(out, DefaultShaders.u_ProjMat * DefaultShaders.u_ViewMat * vec4(1f.lit, 1f.lit, 0f.lit, 1f.lit))
    }

    private val fragmentShader = FragmentShader {
        SET(out, DefaultShaders.v_Col)
        //SET(out, vec4(1f.lit, 1f.lit, 1f.lit, 0f.lit))
    }

    private val program = Program(vertexShader, fragmentShader)

    private val vertex1 = 50f
    private val vertex2 = 200f
    private val colors = listOf(
        ubyteArrayOf(255u, 255u, 255u, 255u), // White
        ubyteArrayOf(255u, 0u, 0u, 255u), // Red
        ubyteArrayOf(0u, 255u, 0u, 255u), // Blue
        ubyteArrayOf(0u, 0u, 255u, 255u), // Green
    )
    private val vertices = listOf(
        listOf(vertex1, vertex1),
        listOf(vertex1, vertex2),
        listOf(vertex2, vertex2),
        listOf(vertex2, vertex1)
    )

    private val simpleVertexData = AGVertexArrayObject(
        AGVertexData(
            layout = VertexLayout(DefaultShaders.a_Col),
            buffer = AGBuffer().apply {
                upload(colors.flatten().toUByteArray())
            }
        ),
        AGVertexData(
            layout = VertexLayout(DefaultShaders.a_Pos),
            buffer = AGBuffer().apply {
                upload(vertices.flatten().toFloatArray())
            }
        )
    )

    private val complexVertexData = AGVertexArrayObject(
        AGVertexData(
            layout = VertexLayout(DefaultShaders.a_Col, DefaultShaders.a_Pos),
            buffer = AGBuffer().apply {
                Buffer(48).apply {
                    var position = 0
                    (0..3).forEach { index ->
                        colors[index].forEach { color ->
                            setUnalignedUInt8(position, color.toInt())
                            position += 1
                        }
                        vertices[index].forEach { vertex ->
                            println("vertex $vertex")
                            setUnalignedFloat32(position, vertex)
                            position += 4
                        }
                    }
                    upload(this)
                }.let {
                    mem?.getUInt8(0).let { println(it) }
                    mem?.getUInt8(1).let { println(it) }
                    mem?.getUInt8(2).let { println(it) }
                    mem?.getUInt8(3).let { println(it) }
                    mem?.getUnalignedFloat32(4).let { println(it) }
                    mem?.getUnalignedFloat32(8).let { println(it) }
                    mem?.getUInt8(12 + 0).let { println(it) }
                    mem?.getUInt8(12 + 1).let { println(it) }
                    mem?.getUInt8(12 + 2).let { println(it) }
                    mem?.getUInt8(12 + 3).let { println(it) }
                    mem?.getUnalignedFloat32(12 + 4).let { println(it) }
                    mem?.getUnalignedFloat32(12 + 8).let { println(it) }
                    mem?.getUInt8(24 + 0).let { println(it) }
                    mem?.getUInt8(24 + 1).let { println(it) }
                    mem?.getUInt8(24 + 2).let { println(it) }
                    mem?.getUInt8(24 + 3).let { println(it) }
                    mem?.getUnalignedFloat32(24 + 4).let { println(it) }
                    mem?.getUnalignedFloat32(24 + 8).let { println(it) }
                    mem?.getUInt8(36 + 0).let { println(it) }
                    mem?.getUInt8(36 + 1).let { println(it) }
                    mem?.getUInt8(36 + 2).let { println(it) }
                    mem?.getUInt8(36 + 3).let { println(it) }
                    mem?.getUnalignedFloat32(36 + 4).let { println(it) }
                    mem?.getUnalignedFloat32(36 + 8).let { println(it) }
                }
            }
        )
    )

    private val indices = AGBuffer().upload(shortArrayOf(0, 1, 2, 2, 3, 0))

    override fun drawOnView(view: MTKView) {

        val width = view.drawableSize.useContents { width }.toInt()
        val height = view.drawableSize.useContents { height }.toInt()

        val frameBuffer = AGFrameBufferBase(false)
        val frameBufferInfo = AGFrameBufferInfo(0)
            .withSize(width, height)
            .withSamples(1)
            .withHasDepth(true)
            .withHasStencil(true)


        val uniformBlockBuffer = UniformBlockBuffer(uniformBlock)
        val uniformBuffer = MMatrix3D()
                .setToOrtho(0f, width.toFloat(), 0f, height.toFloat(), -1f, +1f)
                .data
                .let { AGBuffer().upload(it, 0, it.size) }

        ag.draw(
            frameBuffer,
            frameBufferInfo,
            if (simple) simpleVertexData else complexVertexData,
            program,
            drawType = AGDrawType.TRIANGLES,
            vertexCount = 6, // Draw 2 triangles
            indices,
            indexType = AGIndexType.USHORT,
            drawOffset = 0, // This value can be != of 0 ?
            blending = AGBlending.NORMAL, // Pure guess
            //uniformBlocks = UniformBlocksBuffersRef.EMPTY,
            uniformBlocks = UniformBlocksBuffersRef(
                blocks = arrayOf(uniformBlockBuffer),
                buffers = arrayOf(uniformBuffer),
                valueIndices = intArrayOf(0)
            ),
            stencilRef = AGStencilReference.DEFAULT, // Pure guess
            stencilOpFunc = AGStencilOpFunc.DEFAULT, // Pure guess
            colorMask = AGColorMask.DEFAULT,// Pure guess
            depthAndFrontFace = AGDepthAndFrontFace.DEFAULT,// Pure guess
            scissor = AGScissor.FULL,// Pure guess
            cullFace = AGCullFace.NONE,// Pure guess
            instances = 1// Pure guess
        )
    }


}
