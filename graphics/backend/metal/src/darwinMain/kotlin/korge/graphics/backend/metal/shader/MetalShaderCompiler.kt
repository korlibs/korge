package korge.graphics.backend.metal.shader

import com.soywiz.korag.shader.*
import korge.graphics.backend.metal.MetalProgram
import kotlinx.cinterop.*
import platform.Foundation.NSError
import platform.Metal.*

object MetalShaderCompiler {

    fun compile(device: MTLDeviceProtocol, program: Program): MetalProgram {
        return program.toMetalShaders(device)
            .toCompiledProgram(device)
    }
}

private fun Program.toMetalShaders(device: MTLDeviceProtocol) = (vertex to fragment)
    .toFunctionsLibrary(device)
    .let { it.toFunction(vertexMainFunctionName) to it.toFunction(fragmentMainFunctionName) }

private fun Pair<VertexShader, FragmentShader>.toFunctionsLibrary(device: MTLDeviceProtocol) = toNewMetalShaderStringResult()
    .toFunctionsLibrary(device)

private fun MetalShaderGenerator.Result.toFunctionsLibrary(device: MTLDeviceProtocol): MTLLibraryProtocol {
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
        .toMetalProgram()

private fun createPipelineState(
    device: MTLDeviceProtocol,
    vertexFunction: MTLFunctionProtocol,
    fragmentFunction: MTLFunctionProtocol
): MTLRenderPipelineStateProtocol {
    memScoped {
        val renderPipelineDescriptor = MTLRenderPipelineDescriptor().apply {
            setVertexFunction(vertexFunction)
            setFragmentFunction(fragmentFunction)
        }

        val errorPtr = alloc<ObjCObjectVar<NSError?>>()
        return device.newRenderPipelineStateWithDescriptor(renderPipelineDescriptor, errorPtr.ptr).let {
            errorPtr.value?.let { error -> error(error.localizedDescription) }
            it ?: error("fail to create metal render pipeline state")
        }
    }
}

private fun MTLRenderPipelineStateProtocol.toMetalProgram() = MetalProgram(this)
