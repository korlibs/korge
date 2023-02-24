package korge.graphics.backend.metal

import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korma.geom.*
import kotlinx.cinterop.*
import platform.Metal.*
import platform.MetalKit.*

class Renderer01(device: MTLDeviceProtocol) : Renderer(device) {

    private var ag: AGMetal? = null

    private val vertexShader = VertexShader {
        SET(DefaultShaders.v_Col, DefaultShaders.a_Col)
        SET(out, DefaultShaders.u_ProjMat /* DefaultShaders.u_ViewMat*/ * vec4(DefaultShaders.a_Pos, 0f.lit, 1f.lit))
        //SET(out, vec4(DefaultShaders.a_Pos, 0f.lit, 1f.lit))

        //SET(out, DefaultShaders.u_ProjMat * DefaultShaders.u_ViewMat * vec4(1f.lit, 1f.lit, 0f.lit, 1f.lit))
    }

    private val fragmentShader = FragmentShader {
        SET(out, DefaultShaders.v_Col)
    }

    private val program = Program(vertexShader, fragmentShader)

    private val vertex1 = 200f
    private val vertex2 = 0f
    private val vertexData = AGVertexArrayObject(
        AGVertexData(
            layout = VertexLayout(DefaultShaders.a_Col),
            buffer = AGBuffer().upload(floatArrayOf(
                1f, 1f, 1f, 0.0f, // White
                1f, 0f, 0f, 0.0f, // Red
                0f, 1f, 0.0f, 0.0f, // Blue
                0f, 0.0f, 1f, 0.0f, // Green
            ))
        ),
        AGVertexData(
            layout = VertexLayout(DefaultShaders.a_Pos),
            buffer = AGBuffer().upload(floatArrayOf(
                vertex1, vertex1,
                vertex2, vertex1,
                vertex2, vertex2,
                vertex1, vertex2
            ))
        )
    )

    private val indices = AGBuffer().upload(shortArrayOf(0, 1, 2, 2, 3, 0))

    override fun drawOnView(view: MTKView) {

        val width = view.drawableSize.useContents { width }.toInt()
        val height = view.drawableSize.useContents { height }.toInt()

        if (ag == null) {
            ag = AGMetal(view)

            MMatrix3D()
                .setToOrtho(0f, width.toFloat(), 0f, height.toFloat(), -1f, +1f)
                .also(::println)

            MMatrix3D()
                .setToOrtho(0f, width.toFloat(), 0f, height.toFloat(), -1f, +1f)
                .transform(200f, 0f, 0f, 1f)
                .also(::println)

        }
        val frameBuffer = AGFrameBufferBase(false)
        val frameBufferInfo = AGFrameBufferInfo(0)
            .withSize(width, height)
            .withSamples(1)
            .withHasDepth(true)
            .withHasStencil(true)


        ag?.draw(
            frameBuffer,
            frameBufferInfo,
            vertexData,
            program,
            drawType = AGDrawType.TRIANGLES,
            vertexCount = 6, // Draw 2 triangles
            indices,
            indexType = AGIndexType.USHORT,
            drawOffset = 0, // This value can be != of 0 ?
            blending = AGBlending.NORMAL, // Pure guess
            uniforms = AGUniformValues().apply {
                val floatBuffer = MMatrix3D()
                    .setToOrtho(0f, width.toFloat(), 0f, height.toFloat(), -1f, +1f)
                    .data
                    .let { Float32Buffer(it, 0, it.size) }
                values.add(
                    AGUniformValue(
                        DefaultShaders.u_ProjMat,
                        floatBuffer.buffer,
                        texture = null,
                        AGTextureUnitInfo.INVALID
                    )
                )
            }, // Not yet supported on shader generation
            //AGUniformValues(
            //  u_ProjMat=AGUniformValue[Uniform(u_ProjMat)][AGValue[Mat4]([[0.0015625, 0, 0, 0, 0, -0.0027777778, 0, 0, 0, 0, -1, 0, -1, 1, 0, 1]])],
            //  u_ViewMat=AGUniformValue[Uniform(u_ViewMat)][AGValue[Mat4]([[1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]])],
            //  u_Tex0=AGUniformValue[Uniform(u_Tex0)][AGValue[Sampler2D]([[-1],AGTexture(size=1,1,pre=true),AGTextureUnitInfo(wrap=CLAMP_TO_EDGE, linear=true, trilinear=true)])],
            //  u_Tex1=AGUniformValue[Uniform(u_Tex1)][AGValue[Sampler2D]([[-1]])],
            //  u_Tex2=AGUniformValue[Uniform(u_Tex2)][AGValue[Sampler2D]([[-1]])],
            //  u_Tex3=AGUniformValue[Uniform(u_Tex3)][AGValue[Sampler2D]([[-1]])],
            //  u_Radius=AGUniformValue[Uniform(u_Radius)][AGValue[Float4]([[6, 6, 6, 6]])],
            //  u_Size=AGUniformValue[Uniform(u_Size)][AGValue[Float2]([[200, 32]])],
            //  u_BackgroundColor=AGUniformValue[Uniform(u_BackgroundColor)][AGValue[Float4]([[1, 1, 1, 1]])],
            //  u_HighlightPos=AGUniformValue[Uniform(u_HighlightPos)][AGValue[Float2]([[0, 0]])],
            //  u_HighlightRadius=AGUniformValue[Uniform(u_HighlightRadius)][AGValue[Float1]([[0]])],
            //  u_HighlightColor=AGUniformValue[Uniform(u_HighlightColor)][AGValue[Float4]([[1, 1, 1, 1]])],
            //  u_BorderSizeHalf=AGUniformValue[Uniform(u_BorderSizeHalf)][AGValue[Float1]([[0.5]])],
            //  u_BorderColor=AGUniformValue[Uniform(u_BorderColor)][AGValue[Float4]([[0.7411765, 0.7411765, 0.7411765, 1]])],
            //  u_ShadowColor=AGUniformValue[Uniform(u_ShadowColor)][AGValue[Float4]([[0, 0, 0, 0.29803923]])],
            //  u_ShadowOffset=AGUniformValue[Uniform(u_ShadowOffset)][AGValue[Float2]([[0, 0]])],
            //  u_ShadowRadius=AGUniformValue[Uniform(u_ShadowRadius)][AGValue[Float1]([[10]])])
            stencilRef = AGStencilReference.DEFAULT, // Pure guess
            stencilOpFunc = AGStencilOpFunc.DEFAULT, // Pure guess
            colorMask = AGColorMask.DEFAULT,// Pure guess
            depthAndFrontFace= AGDepthAndFrontFace.DEFAULT,// Pure guess
            scissor= AGScissor.FULL,// Pure guess
            cullFace= AGCullFace.NONE,// Pure guess
            instances = 1// Pure guess
        )
    }


}
