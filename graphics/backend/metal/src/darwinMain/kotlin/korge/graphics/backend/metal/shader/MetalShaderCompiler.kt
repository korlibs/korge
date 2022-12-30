package korge.graphics.backend.metal.shader

import com.soywiz.korag.shader.Program
import com.soywiz.korag.shader.Shader
import korge.graphics.backend.metal.MetalProgram
import kotlinx.cinterop.*
import platform.Foundation.NSError
import platform.Metal.MTLDeviceProtocol
import platform.Metal.MTLFunctionProtocol
import platform.Metal.MTLLibraryProtocol

object MetalShaderCompiler {

    fun compile(device: MTLDeviceProtocol, program: Program): MetalProgram {
        return program.toMetalShaders(device)
            .toProgram()
    }
}

private fun Program.toMetalShaders(device: MTLDeviceProtocol)
    = vertex.toFunction(device) to fragment.toFunction(device)

private fun Shader.toFunction(device: MTLDeviceProtocol) = toNewMetalShaderStringResult()
    .toFunction(device)

private fun MetalShaderGenerator.Result.toFunction(device: MTLDeviceProtocol): MTLFunctionProtocol {
    memScoped {
        val errorPtr = alloc<ObjCObjectVar<NSError?>>()
        return device.newLibraryWithSource(result, null, errorPtr.ptr).let {
            errorPtr.value?.let { error -> error(error.localizedDescription) }
            it ?: error("fail to create library")
        }.toFunction()
    }
}

private fun MTLLibraryProtocol.toFunction() = newFunctionWithName("main")
    ?: error("fail to create function")


private fun Pair<MTLFunctionProtocol, MTLFunctionProtocol>.toProgram() =
    let { (vertex, fragment) -> MetalProgram(vertex, fragment) }
