package korlibs.webgpu

import io.ygdrasil.wgpu.internal.js.*
import korlibs.graphics.*
import korlibs.image.color.*
import korlibs.js.*
import korlibs.math.geom.*
import kotlinx.coroutines.*

private external val navigator: NavigatorGPU

class WebGPUAG(val device: GPUDevice) : AG() {
    val dimensions = SizeInt(320, 240)

    val shaderModule = device.createShaderModule(GPUShaderModuleDescriptor(
        code = """
            @vertex
            fn vs_main(@builtin(vertex_index) in_vertex_index: u32) -> @builtin(position) vec4<f32> {
                let x = f32(i32(in_vertex_index) - 1);
                let y = f32(i32(in_vertex_index & 1u) * 2 - 1);
                return vec4<f32>(x, y, 0.0, 1.0);
            }
    
            @fragment
            fn fs_main() -> @location(0) vec4<f32> {
                return vec4<f32>(1.0, 0.0, 0.0, 1.0);
            }
        """.trimIndent()
    ))

    val pipelineLayout = device.createPipelineLayout(GPUPipelineLayoutDescriptor(
        bindGroupLayouts = arrayOf(),
    ))

    val renderPipeline = device.createRenderPipeline(
        GPURenderPipelineDescriptor(
            layout = pipelineLayout,
            vertex = GPUVertexState(
                module = shaderModule,
                entryPoint = "vs_main",
            ),
            fragment = GPUFragmentState(
                module = shaderModule,
                entryPoint = "fs_main",
                targets = arrayOf(
                    GPUColorTargetState(
                        format = GPUTextureFormat.RGBA8UNORM_SRGB,
                    ),
                ),
            ),
        )
    )

    val captureInfo = createCapture(
        device,
        dimensions.width,
        dimensions.height,
    )
    val texture = captureInfo.texture
    val outputBuffer = captureInfo.outputBuffer

    companion object {
        suspend operator fun invoke(): WebGPUAG {
            val adapter: GPUAdapter = navigator.gpu.requestAdapter().await()
                ?: error("No WebGPU adapter found")
            return WebGPUAG(adapter.requestDevice().await())
        }
    }


    override fun clear(
        frameBuffer: AGFrameBufferBase,
        frameBufferInfo: AGFrameBufferInfo,
        color: RGBA,
        depth: Float,
        stencil: Int,
        clearColor: Boolean,
        clearDepth: Boolean,
        clearStencil: Boolean,
        scissor: AGScissor
    ) {
        val encoder = device.createCommandEncoder();
        val renderPass = encoder.beginRenderPass(
            GPURenderPassDescriptor(
                colorAttachments = arrayOf(
                    GPURenderPassColorAttachment(
                        view = texture.createView(),
                        storeOp = "store",
                        loadOp = "clear",
                        clearValue = arrayOf(0, 1, 0, 1),
                    ),
                ),
            )
            //GPURenderPassDescriptor()
        );
        renderPass.setPipeline(renderPipeline);
        renderPass.draw(3, 1);
        renderPass.end();
        copyToBuffer(encoder, texture, outputBuffer, dimensions);
        device.queue.submit(arrayOf(encoder.finish()));
    }

    override fun readToMemory(frameBuffer: AGFrameBufferBase, frameBufferInfo: AGFrameBufferInfo, x: Int, y: Int, width: Int, height: Int, data: Any, kind: AGReadKind) {

    }

    private fun copyToBuffer(
        encoder: GPUCommandEncoder,
        texture: GPUTexture,
        outputBuffer: GPUBuffer,
        dimensions: SizeInt,
    ) {
        val padded = getRowPadding(dimensions.width).padded;

        encoder.copyTextureToBuffer(
            GPUImageCopyTexture(texture = texture,),
            GPUImageCopyBuffer(buffer = outputBuffer, bytesPerRow = padded),
            GPUExtent3DDictStrict(width = dimensions.width, height = dimensions.height),
        );
    }

    data class CreateCapture(val texture: GPUTexture, val outputBuffer: GPUBuffer)

    private fun createCapture(
        device: GPUDevice,
        width: Int,
        height: Int,
    ): CreateCapture {
        val padded = getRowPadding(width).padded;
        val outputBuffer = device.createBuffer(GPUBufferDescriptor(
            label = "Capture",
            size = padded * height,
            usage = (GPUBufferUsage.MAP_READ or GPUBufferUsage.COPY_DST
        )))
        val texture = device.createTexture(
            GPUTextureDescriptor(
                label = "Capture",
                size = GPUExtent3DDictStrict(width = width, height = height),
                format = GPUTextureFormat.RGBA8UNORM_SRGB,
                usage = (GPUTextureUsage.RENDER_ATTACHMENT or GPUTextureUsage.COPY_SRC),
            )
        )

        return CreateCapture(texture, outputBuffer)
    }


    /** Return value for {@linkcode getRowPadding}. */
    data class Padding(
        /** The number of bytes per row without padding calculated. */
        val unpadded: Int,
        /** The number of bytes per row with padding calculated. */
        val padded: Int,
    )
    /** Buffer-Texture copies must have [`bytes_per_row`] aligned to this number. */
    val COPY_BYTES_PER_ROW_ALIGNMENT = 256;
    /** Number of bytes per pixel. */
    val BYTES_PER_PIXEL = 4;


    private fun getRowPadding(width: Int): Padding {
        // It is a WebGPU requirement that
        // GPUImageCopyBuffer.layout.bytesPerRow % COPY_BYTES_PER_ROW_ALIGNMENT == 0
        // So we calculate paddedBytesPerRow by rounding unpaddedBytesPerRow
        // up to the next multiple of COPY_BYTES_PER_ROW_ALIGNMENT.

        val unpaddedBytesPerRow = width * BYTES_PER_PIXEL;
        val paddedBytesPerRowPadding = (COPY_BYTES_PER_ROW_ALIGNMENT -
            (unpaddedBytesPerRow % COPY_BYTES_PER_ROW_ALIGNMENT)) %
            COPY_BYTES_PER_ROW_ALIGNMENT;
        val paddedBytesPerRow = unpaddedBytesPerRow + paddedBytesPerRowPadding;

        return Padding(
            unpadded = unpaddedBytesPerRow,
            padded = paddedBytesPerRow,
        )
    }
}
