package korge.graphics.backend.metal

import platform.Metal.*

class MetalProgram(
    val vertex: MTLFunctionProtocol,
    val fragment: MTLFunctionProtocol
)
