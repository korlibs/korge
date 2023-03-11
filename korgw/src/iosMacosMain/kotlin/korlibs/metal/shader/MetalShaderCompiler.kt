package korlibs.metal.shader

import korlibs.graphics.metal.shader.*
import korlibs.graphics.metal.shader.fragmentMainFunctionName
import korlibs.graphics.metal.shader.vertexMainFunctionName
import korlibs.graphics.shader.*
import korlibs.metal.*
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.Metal.*

/**
 * construct a metal program with following step :
 * - Convert vertex and fragment to get a String and the list of expected buffers
 * - Create a function library from the Shader String
 * - Extract vertex and fragment functions from this library
 * - create a MTLRenderPipelineStateProtocol https://developer.apple.com/documentation/metal/mtlrenderpipelinestate
 * - use a value class to store the compiled program and the list of input buffers
 */
object MetalShaderCompiler {

    fun compile(device: MTLDeviceProtocol, program: Program): MetalProgram {
        return program.toMetalShader()
            .also {
                println(it.result)
                println(it.inputBuffers)
            }
            .toInternalMetalProgram(device)
    }
}


private fun MetalShaderGenerator.Result.toInternalMetalProgram(device: MTLDeviceProtocol) =
    let { (shaderAsString, inputBuffers) ->
        shaderAsString
            .toFunctionsLibrary(device)
            .let { it.toFunction(vertexMainFunctionName) to it.toFunction(fragmentMainFunctionName) }
            .toCompiledProgram(device)
            .toInternalMetalProgram(inputBuffers)

    }

private fun Program.toMetalShader() = (vertex to fragment)
    .toNewMetalShaderStringResult()

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

private fun Pair<MTLFunctionProtocol, MTLFunctionProtocol>.toCompiledProgram(device: MTLDeviceProtocol) =
    let { (vertex, fragment) -> createPipelineState(device, vertex, fragment) }

private fun createPipelineState(
    device: MTLDeviceProtocol,
    vertexFunction: MTLFunctionProtocol,
    fragmentFunction: MTLFunctionProtocol
): MTLRenderPipelineStateProtocol {
    memScoped {
        val renderPipelineDescriptor = MTLRenderPipelineDescriptor().apply {
            setVertexFunction(vertexFunction)
            setFragmentFunction(fragmentFunction)
            colorAttachments.objectAtIndexedSubscript(0)
                .setPixelFormat(MTLPixelFormatBGRA8Unorm_sRGB)
        }

        val errorPtr = alloc<ObjCObjectVar<NSError?>>()
        return device.newRenderPipelineStateWithDescriptor(renderPipelineDescriptor, errorPtr.ptr).let {
            errorPtr.value?.let { error -> error(error.localizedDescription) }
            it ?: error("fail to create metal render pipeline state")
        }
    }
}

private fun MTLRenderPipelineStateProtocol.toInternalMetalProgram(inputBuffers: List<VariableWithOffset>) =
    MetalProgram(
        this,
        inputBuffers
    )
