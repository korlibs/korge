package korge.graphics.backend.metal

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import korge.graphics.backend.metal.shader.*
import kotlinx.cinterop.*
import platform.Foundation.NSError
import platform.Foundation.NSMakeRange
import platform.Metal.*
import platform.MetalKit.MTKView
import platform.posix.memmove

class Renderer01(device: MTLDeviceProtocol) : Renderer(device) {

    private lateinit var vertexPositionsBuffer: MTLBufferProtocol
    private lateinit var vertexColorsBuffer: MTLBufferProtocol
    private lateinit var renderPipelineStateProtocol: MTLRenderPipelineStateProtocol

    init {
        buildShaders()
        buildBuffers()
    }

    override fun drawOnView(view: MTKView) {
        autoreleasepool {

            val commandBuffer = commandQueue.commandBuffer() ?: error("fail to get command buffer")
            val currentRenderPassDescriptor =
                view.currentRenderPassDescriptor() ?: error("fail to get render pass descriptor")
            val renderCommanderEncoder = commandBuffer.renderCommandEncoderWithDescriptor(currentRenderPassDescriptor)
                ?: error("fail to get render commander encoder")

            renderCommanderEncoder.apply {
                setRenderPipelineState(renderPipelineStateProtocol)
                setVertexBuffer(vertexColorsBuffer, 0, 0)
                setVertexBuffer(vertexPositionsBuffer, 0, 1)
                drawPrimitives(MTLPrimitiveTypeTriangle, 0, 3)
            }

            renderCommanderEncoder.endEncoding()
            commandBuffer.presentDrawable(view.currentDrawable()!!)
            commandBuffer.commit()

        }
    }

    private fun buildShaders() = memScoped {

        val vertexShader = VertexShader {
            SET(DefaultShaders.v_Col, DefaultShaders.a_Col)
            SET(out, vec4(DefaultShaders.a_Pos, 1f.lit, 1f.lit))
        }
        val fragmentShader = FragmentShader {
            SET(out, DefaultShaders.v_Col)
        }

        vertexShader.toNewGlslStringResult()
            .result
            .also(::println)

        fragmentShader.toNewGlslStringResult()
            .result
            .also(::println)

        var shaderSrc = """
                #include <metal_stdlib>
                using namespace metal;
        
                struct v2f
                {
                    float4 v_Col;
                    float4 position [[position]];
                };
        
                vertex v2f vertexMain( uint vertexId [[vertex_id]],
                                       device const float4* colors [[buffer(0)]],
                                       device const float2* positions [[buffer(1)]] )
                {
                    v2f o;
                    o.position = float4( positions[ vertexId ], 0.0, 1.0 );
                    o.v_Col =  colors[ vertexId ];
                    return o;
                }
        
                fragment float4 fragmentMain( v2f in [[stage_in]] )
                {
                    float4 out;
                    out = in.v_Col;
                    return out;
                }
        """.trimIndent()

        shaderSrc =
            (vertexShader to fragmentShader).toNewMetalShaderStringResult()
            .result
            .also(::println)

        val errorPtr = alloc<ObjCObjectVar<NSError?>>()
        val library = device.newLibraryWithSource(shaderSrc, null, errorPtr.ptr).let {
            errorPtr.value?.let { error -> error(error.localizedDescription) }
            it ?: error("fail to create library")
        }

        val vertexFunction = library.newFunctionWithName("vertexMain")
        val fragmentFunction = library.newFunctionWithName("fragmentMain")
        val renderPipelineDescriptor = MTLRenderPipelineDescriptor().apply {
            setVertexFunction(vertexFunction)
            setFragmentFunction(fragmentFunction)
            colorAttachments.objectAtIndexedSubscript(0)
                .setPixelFormat(MTLPixelFormatBGRA8Unorm_sRGB)
        }

        renderPipelineStateProtocol =
            device.newRenderPipelineStateWithDescriptor(renderPipelineDescriptor, errorPtr.ptr).let {
                errorPtr.value?.let { error -> error(error.localizedDescription) }
                it ?: error("fail to create render pipeline state")
            }

    }

    private fun buildBuffers() = memScoped {
        val numVertices = 3

        val position = allocArrayOf(
            -0.8f, 0.8f,
            0.0f, -0.8f,
            +0.8f, 0.8f
        )

        val colors = allocArrayOf(
            1f, 0.3f, 0.2f, 0.0f,
            0.8f, 1f, 0.0f, 0.0f,
            0.8f, 0.0f, 1f, 0.0f
        )

        val positionsDataSize = numVertices * sizeOf<FloatVar>() * 2
        val colorDataSize = numVertices * sizeOf<FloatVar>() * 4
        println("positionsDataSize $positionsDataSize colorDataSize $colorDataSize")

        vertexPositionsBuffer = device.newBufferWithLength(positionsDataSize.toULong(), MTLResourceStorageModeManaged)
            ?: error("fail to create vertexPositionsBuffer")
        vertexColorsBuffer = device.newBufferWithLength(colorDataSize.toULong(), MTLResourceStorageModeManaged)
            ?: error("fail to create vertexColorsBuffer")

        memmove(vertexPositionsBuffer.contents(), position.reinterpret<CPointed>(), positionsDataSize.toULong())
        memmove(vertexColorsBuffer.contents(), colors.reinterpret<CPointed>(), colorDataSize.toULong())

        vertexPositionsBuffer.didModifyRange(NSMakeRange(0, vertexPositionsBuffer.length))
        vertexColorsBuffer.didModifyRange(NSMakeRange(0, vertexColorsBuffer.length))
    }
}
