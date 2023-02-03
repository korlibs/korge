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

class Renderer02(device: MTLDeviceProtocol) : Renderer(device) {

    private lateinit var vertexPositionsBuffer: MTLBufferProtocol
    private lateinit var vertexColorsBuffer: MTLBufferProtocol
    private lateinit var textureCoordinate: MTLBufferProtocol
    private lateinit var indexBuffer: MTLBufferProtocol
    private lateinit var renderPipelineStateProtocol: MTLRenderPipelineStateProtocol
    private lateinit var texture: MTLTextureProtocol

    init {
        buildShaders()
        buildBuffers()
        buildTextures()
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
                drawIndexedPrimitives(MTLPrimitiveTypeTriangle, 6u, MTLIndexTypeUInt32, indexBuffer, 0)
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

        vertexColorsBuffer = createBufferUsing(
            1f, 1f, 1f, 0.0f, // White
            1f, 0f, 0f, 0.0f, // Red
            0f, 1f, 0.0f, 0.0f, // Blue
            0f, 0.0f, 1f, 0.0f, // Green
        )

        vertexPositionsBuffer = createBufferUsing(
            0f, 0f,
            1f, 0f,
            1f, 1f,
            0f, 1f
        )

        textureCoordinate = createBufferUsing(
            0f, 0f,
            1f, 0f,
            1f, 1f,
            0f, 1f
        )

        indexBuffer = createBufferUsing(
            0,1,2, 2, 3, 0
        )
    }


    private fun NativePlacement.createBufferUsing(vararg values: Int): MTLBufferProtocol {
        val contentSize = values.size * sizeOf<IntVar>()
        val contents = allocArrayOf(*values)
        val buffer = device.newBufferWithLength(contentSize.toULong(), MTLResourceStorageModeManaged)
            ?: error("fail to create vertexPositionsBuffer")
        memmove(buffer.contents(), contents.reinterpret<CPointed>(), contentSize.toULong())
        buffer.didModifyRange(NSMakeRange(0, buffer.length))

        return buffer
    }

    private fun NativePlacement.createBufferUsing(vararg values: Float): MTLBufferProtocol {
        val contentSize = values.size * sizeOf<FloatVar>()
        val contents = allocArrayOf(*values)
        val buffer = device.newBufferWithLength(contentSize.toULong(), MTLResourceStorageModeManaged)
            ?: error("fail to create vertexPositionsBuffer")
        memmove(buffer.contents(), contents.reinterpret<CPointed>(), contentSize.toULong())
        buffer.didModifyRange(NSMakeRange(0, buffer.length))

        return buffer
    }

    private fun buildTextures() = memScoped {
        println("buildTextures")

        val tw = 128uL
        val th = 128uL

        val mtlTextureDescriptor = MTLTextureDescriptor()
        mtlTextureDescriptor.width = tw
        mtlTextureDescriptor.height = th
        mtlTextureDescriptor.pixelFormat = MTLPixelFormatA8Unorm
        mtlTextureDescriptor.textureType = MTLTextureType2D
        mtlTextureDescriptor.storageMode = MTLStorageModeManaged
        mtlTextureDescriptor.usage = MTLResourceUsageSample or MTLResourceUsageRead

        texture = device.newTextureWithDescriptor(mtlTextureDescriptor) ?: error("fail to create texture")

        val pTextureData = allocArray<IntVar>((tw * th * 4u).toInt())
        for (y in (0 until th.toInt())) {
            for (x in (0 until tw.toInt())) {
                val isWhite = ((x xor y) and 0b1000000) != 0
                val c = if (isWhite) 0xFF else 0xA

                val i = y * tw.toInt() + x

                pTextureData[i * 4 + 0] = c
                pTextureData[i * 4 + 1] = c
                pTextureData[i * 4 + 2] = c
                pTextureData[i * 4 + 3] = 0xFF
            }
        }

        texture.replaceRegion(MTLRegionMake3D(0uL, 0uL, 0uL, tw, th, 1uL), 0, pTextureData, tw * 4uL)

    }
}

public fun NativePlacement.allocArrayOf(vararg elements: Int): CArrayPointer<IntVar> {
    val res = allocArray<IntVar>(elements.size)
    var index = 0
    while (index < elements.size) {
        res[index] = elements[index]
        ++index
    }
    return res
}
