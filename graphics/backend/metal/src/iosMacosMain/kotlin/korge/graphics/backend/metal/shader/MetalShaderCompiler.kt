package korge.graphics.backend.metal.shader

import com.soywiz.korag.shader.*
import korge.graphics.backend.metal.MetalProgram
import kotlinx.cinterop.*
import platform.Foundation.NSError
import platform.Metal.*

object MetalShaderCompiler {

    fun compile(device: MTLDeviceProtocol, program: Program): MetalProgram {
        return program.toMetalShaders().let { (shaderAsString, inputBuffers) ->

            shaderAsString
                .toFunctionsLibrary(device)
                .let { it.toFunction(vertexMainFunctionName) to it.toFunction(fragmentMainFunctionName) }
                .toCompiledProgram(device)
                .toMetalProgram(inputBuffers)
        }
    }
}

private fun Program.toMetalShaders() = (vertex to fragment)
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

private fun MTLRenderPipelineStateProtocol.toMetalProgram(inputBuffers: List<VariableWithOffset>) = MetalProgram(
    this,
    inputBuffers
)
