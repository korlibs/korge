package com.soywiz.metal

import platform.Metal.MTLDeviceProtocol
import platform.MetalKit.MTKView

abstract class Renderer(val device: MTLDeviceProtocol) {

    val commandQueue = device.newCommandQueue() ?: error("fail to create command queue")

    abstract fun drawOnView(view: MTKView)
}
