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
        bufferInputLayouts: MetalShaderBufferInputLayouts
    ): MetalProgram {
        return program.toMetalShader(bufferInputLayouts)
            .toInternalMetalProgram(device)
    }
}

private val logger by lazy { Logger("MetalShaderCompiler") }


private fun MetalShaderGenerator.Result.toInternalMetalProgram(device: MTLDeviceProtocol) =
    let { (shaderAsString, inputBuffers) ->
        shaderAsString
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
    inputBuffers: List<List<VariableWithOffset>>
) =
    let { (vertex, fragment) -> createPipelineState(device, vertex, fragment, inputBuffers) }

private fun createPipelineState(
    device: MTLDeviceProtocol,
    vertexFunction: MTLFunctionProtocol,
    fragmentFunction: MTLFunctionProtocol,
    inputBuffers: List<List<VariableWithOffset>>
): MTLRenderPipelineStateProtocol {
    memScoped {
        val renderPipelineDescriptor = MTLRenderPipelineDescriptor().apply {
            setVertexFunction(vertexFunction)
            setFragmentFunction(fragmentFunction)
            colorAttachments.objectAtIndexedSubscript(0)
                .setPixelFormat(MTLPixelFormatBGRA8Unorm_sRGB)

            var attributeIndex = -1
            vertexDescriptor = MTLVertexDescriptor().apply {
                inputBuffers
                    .mapIndexed { bufferIndex, layout -> bufferIndex to layout }
                    .filter { (_, layout) -> layout.size > 1 && layout.none { it is Uniform } }
                    .filterIsInstance<Pair<Int, List<Attribute>>>()
                    .forEachIndexed { layoutIndex, (bufferIndex, layout) ->
                        logger.debug { "will create attributes on vertex descriptor with layout $layout" }

                        var offset = 0
                        layout.forEach { attribute ->
                            logger.debug { "${attribute.type} "}
                            val format = attribute.type.toMetalVertexFormat()
                            attributeIndex += 1
                            logger.debug {
                                "will add attribute at buffer index $bufferIndex and attribute index $attributeIndex and offset $offset and format $format"
                            }
                            attributes.objectAtIndexedSubscript(attributeIndex.toULong()).apply {
                                setBufferIndex(bufferIndex.toULong())
                                setOffset(offset.toULong())
                                setFormat(format)
                                offset += attribute.totalBytes
                            }
                        }

                        layouts.objectAtIndexedSubscript(layoutIndex.toULong()).apply {
                            stride = layout.sumOf { it.totalBytes }.toULong()
                        }
                    }

                if (attributeIndex >= 0) {
                    logger.debug { "vertex descriptor will be added to render pipeline descriptor" }
                    vertexDescriptor = this
                }

            }

        }

        val errorPtr = alloc<ObjCObjectVar<NSError?>>()
        return device.newRenderPipelineStateWithDescriptor(renderPipelineDescriptor, errorPtr.ptr).let {
            errorPtr.value?.let { error -> error(error.localizedDescription) }
            it ?: error("fail to create metal render pipeline state")
        }
    }
}

private fun MTLRenderPipelineStateProtocol.toInternalMetalProgram(inputBuffers: List<List<VariableWithOffset>>) =
    MetalProgram(
        this,
        inputBuffers
    )
