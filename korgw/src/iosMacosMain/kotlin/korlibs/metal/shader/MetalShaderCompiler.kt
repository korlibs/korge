package korlibs.metal.shader

import korlibs.graphics.metal.shader.*
import korlibs.graphics.shader.*
import korlibs.logger.Logger
import korlibs.metal.*
import kotlinx.cinterop.*
import platform.Foundation.NSError
import platform.Metal.*

/**
 * construct a metal program with following step :
 * - Convert vertex and fragment to get a String and the list of expected buffers
 * - Create a function library from the Shader String
 * - Extract vertex and fragment functions from this library
 * - create a MTLRenderPipelineStateProtocol https://developer.apple.com/documentation/metal/mtlrenderpipelinestate
 * - use a value class to store the compiled program and the list of input buffers
 */
internal object MetalShaderCompiler {

    fun compile(
        device: MTLDeviceProtocol,
        program: Program,
        bufferInputLayouts: Lazy<MetalShaderBufferInputLayouts>
    ): MetalProgram {
        return program.toMetalShader(bufferInputLayouts.value)
            .toInternalMetalProgram(device)
    }
}

private val logger by lazy { Logger("MetalShaderCompiler") }


private fun MetalShaderGenerator.Result.toInternalMetalProgram(device: MTLDeviceProtocol) =
    let { (shaderAsString, inputBuffers) ->
        fake
            .also { logger.debug { "generated shader:\n$shaderAsString" } }
            .toFunctionsLibrary(device)
            .let { it.toFunction(vertexMainFunctionName) to it.toFunction(fragmentMainFunctionName) }
            .toCompiledProgram(device, inputBuffers)
            .toInternalMetalProgram(inputBuffers)

    }

private fun Program.toMetalShader(bufferInputLayouts: MetalShaderBufferInputLayouts) = (vertex to fragment)
    .toNewMetalShaderStringResult(bufferInputLayouts)

private fun String.toFunctionsLibrary(device: MTLDeviceProtocol): MTLLibraryProtocol = let { result ->
    memScoped {
        val errorPtr = alloc<ObjCObjectVar<NSError?>>()
        return device.newLibraryWithSource(result, null, errorPtr.ptr).let {
            errorPtr.value?.let { error -> error(error.localizedDescription) }
            it ?: error("fail to create library")
        }
    }
}

private fun MTLLibraryProtocol.toFunction(name: String) = newFunctionWithName(name)
    ?: error("fail to create function with name $name using metal function library")

private fun Pair<MTLFunctionProtocol, MTLFunctionProtocol>.toCompiledProgram(
    device: MTLDeviceProtocol,
    inputBuffers: MetalShaderBufferInputLayouts
) =
    let { (vertex, fragment) -> createPipelineState(device, vertex, fragment, inputBuffers) }

private fun createPipelineState(
    device: MTLDeviceProtocol,
    vertexFunction: MTLFunctionProtocol,
    fragmentFunction: MTLFunctionProtocol,
    inputBuffers: MetalShaderBufferInputLayouts
): MTLRenderPipelineStateProtocol {
    memScoped {
        val renderPipelineDescriptor = MTLRenderPipelineDescriptor().apply {
            setVertexFunction(vertexFunction)
            setFragmentFunction(fragmentFunction)
            colorAttachments.objectAtIndexedSubscript(0)
                .setPixelFormat(MTLPixelFormatBGRA8Unorm_sRGB)

            vertexDescriptor = MTLVertexDescriptor().apply {
                inputBuffers
                    .mapIndexed { bufferIndex, layout -> bufferIndex to layout }
                    .filter { (_, layout) -> layout.none { it is Uniform } }
                    .filterIsInstance<Pair<Int, List<Attribute>>>()
                    .forEach {  (bufferIndex, layout) ->
                        logger.debug { "will create attributes on vertex descriptor with layout $layout" }

                        var offset = 0
                        layout.forEach { attribute ->
                            val format = attribute.type.toMetalVertexFormat(attribute.normalized)
                            val attributeIndex = inputBuffers.attributeIndexOf(attribute)
                                ?: error("attribute $attribute not found in input buffers $inputBuffers")
                            logger.debug {
                                val type = attribute.type
                                val normalized = if (attribute.normalized) "normalized" else ""
                                "will add attribute at buffer index $bufferIndex and attribute index $attributeIndex and offset $offset and format $type $normalized"
                            }
                            attributes.objectAtIndexedSubscript(attributeIndex.toULong()).apply {
                                setBufferIndex(bufferIndex.toULong())
                                setOffset(offset.toULong())
                                setFormat(format)
                                offset += attribute.totalBytes
                            }
                        }

                        layouts.objectAtIndexedSubscript(bufferIndex.toULong()).apply {
                            val stride = layout.sumOf { it.totalBytes }.toULong()
                            logger.debug {
                                "will set layout stride to $stride at index $bufferIndex"
                            }
                            setStride(stride)
                            stepFunction = MTLStepFunctionPerVertex
                        }
                    }

                logger.debug { "vertex descriptor will be added to render pipeline descriptor" }
                vertexDescriptor = this


            }

        }

        val errorPtr = alloc<ObjCObjectVar<NSError?>>()
        return device.newRenderPipelineStateWithDescriptor(renderPipelineDescriptor, errorPtr.ptr).let {
            errorPtr.value?.let { error -> error(error.localizedDescription) }
            it ?: error("fail to create metal render pipeline state")
        }
    }
}

private fun MTLRenderPipelineStateProtocol.toInternalMetalProgram(inputBuffers: MetalShaderBufferInputLayouts) =
    MetalProgram(
        this,
        inputBuffers
    )


val fake = """
    #include <metal_stdlib>
    using namespace metal;
    struct v2f {
    	float4 v_Col;
    	float4 position [[position]];
    };
    
    struct V1 {
    	float4 a_Col [[attribute(0)]];
    	float2 a_Pos [[attribute(1)]];
    };
    
    vertex v2f vertexMain(
    	uint vertexId [[vertex_id]],
    	V1 v1 [[stage_in]],
    	constant float4x4& u_ProjMat [[buffer(2)]]
    ) {
        auto a_Col = v1.a_Col;
        auto a_Pos = v1.a_Pos;
    	v2f out;
    	out.v_Col = a_Col;
    	out.position = (u_ProjMat * float4(a_Pos, 0.0, 1.0));
    	return out;
    }
    fragment float4 fragmentMain(
    	v2f in [[stage_in]]
    ) {
    	float4 out;
    	out = in.v_Col;
    	return out;
    }
""".trimIndent()

