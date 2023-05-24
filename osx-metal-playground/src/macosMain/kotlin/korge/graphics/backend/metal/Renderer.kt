package korge.graphics.backend.metal

import platform.Metal.*
import platform.MetalKit.*

abstract class Renderer {

    val device = MTLCreateSystemDefaultDevice() ?: error("fail to create device")
    val commandQueue = device.newCommandQueue() ?: error("fail to create command queue")

    abstract fun drawOnView(view: MTKView)
}
