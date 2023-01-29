package korge.graphics.backend.metal

import kotlinx.cinterop.autoreleasepool
import platform.Metal.MTLDeviceProtocol
import platform.MetalKit.MTKView

class Renderer00(device: MTLDeviceProtocol) : Renderer(device) {

    override fun drawOnView(view: MTKView) {
        autoreleasepool {
            val commandBuffer = commandQueue.commandBuffer() ?: error("fail to get command buffer")
            val currentRenderPassDescriptor = view.currentRenderPassDescriptor() ?: error("fail to get render pass descriptor")
            val renderCommanderEncoder = commandBuffer.renderCommandEncoderWithDescriptor(currentRenderPassDescriptor)
                ?: error("fail to get render commander encoder")

            renderCommanderEncoder.endEncoding()
            commandBuffer.presentDrawable(view.currentDrawable()!!)
            commandBuffer.commit()

        }
    }

}
