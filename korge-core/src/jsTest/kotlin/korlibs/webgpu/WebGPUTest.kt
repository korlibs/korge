package korlibs.webgpu

import io.ygdrasil.wgpu.internal.js.*
import korlibs.image.bitmap.*
import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.io.net.*
import korlibs.math.geom.*
import korlibs.platform.*
import kotlinx.browser.*
import kotlinx.coroutines.*
import org.khronos.webgl.*
import org.w3c.dom.*
import kotlin.test.*

class WebGPUTest {
    @Test
    @Ignore
    fun test() = suspendTest {
        assertEquals(1, 1)
        val navigator = window.navigator.unsafeCast<NavigatorGPU>()
        val canvas = document.createElement("canvas").unsafeCast<HTMLCanvasElement>()
        val adapter = navigator.gpu.requestAdapter().await() ?: error("Couldn't get adapter")
        val device = adapter.requestDevice().await()
        val context = canvas.getContext("webgpu").unsafeCast<GPUCanvasContext>()

        val devicePixelRatio = window.devicePixelRatio
        canvas.width = (canvas.clientWidth * devicePixelRatio).toInt()
        canvas.height = (canvas.clientHeight * devicePixelRatio).toInt()
        val presentationFormat = navigator.gpu.getPreferredCanvasFormat()
        context.configure(
            GPUCanvasConfiguration(
                device = device,
                format = presentationFormat,
                alphaMode = GPUAlphaMode.PREMULTIPLIED
            )
        )

        val triangleVertWGSL = """
            @vertex
            fn main(
              @builtin(vertex_index) VertexIndex : u32
            ) -> @builtin(position) vec4<f32> {
              var pos = array<vec2<f32>, 3>(
                vec2<f32>(0.0, 0.5),
                vec2<f32>(-0.5, -0.5),
                vec2<f32>(0.5, -0.5)
              );

              return vec4<f32>(pos[VertexIndex], 0.0, 1.0);
            }
        """.trimIndent()

        val redFragWGSL = """
            @fragment
            fn main() -> [[location(0)]] vec4<f32> {
              return vec4<f32>(1.0, 0.0, 0.0, 1.0);
            }
        """.trimIndent()

        val pipeline = device.createRenderPipeline(
            GPURenderPipelineDescriptor(
                layout = "auto",
                vertex = GPUVertexState(
                    module = device.createShaderModule(
                        GPUShaderModuleDescriptor(
                            code = triangleVertWGSL
                        )
                    )
                ),
                fragment = GPUFragmentState(
                    module = device.createShaderModule(
                        GPUShaderModuleDescriptor(
                            code = redFragWGSL
                        )
                    ),
                    targets = arrayOf(
                        GPUColorTargetState(
                            format = presentationFormat
                        )
                    )
                ),
                primitive = GPUPrimitiveState(
                    topology = GPUTopology.TRIANGLE_LIST
                )
            )
        );

        fun frame() {
            val commandEncoder = device.createCommandEncoder()
            val textureView = context.getCurrentTexture().createView()

            val renderPassDescriptor = GPURenderPassDescriptor(
                colorAttachments = arrayOf(
                    GPURenderPassColorAttachment(
                        view = textureView,
                        clearValue = arrayOf(0, 0, 0, 1),
                        loadOp = GPULoadOP.CLEAR,
                        storeOp = GPUStoreOP.STORE
                    )
                )
            )

            val passEncoder = commandEncoder.beginRenderPass(renderPassDescriptor);
            passEncoder.setPipeline(pipeline);
            passEncoder.draw(3);
            passEncoder.end();

            device.queue.submit(arrayOf(commandEncoder.finish()));
            //requestAnimationFrame(frame);
        }

        frame()
    }

    // https://github.com/denoland/webgpu-examples/blob/e8498aa0e001168b77762dde8a1f5fca30c551a7/hello-triangle/mod.ts
    @Test
    fun testOffscreen() = suspendTest {
        if (Platform.isJsDenoJs) return@suspendTest

        val dimensions = SizeInt(200, 200)
        val adapter: GPUAdapter = navigator.gpu.requestAdapter().await()
            ?: (return@suspendTest Unit.also {
                //asserter.assertEquals()
                println("No adapter found. Cannot run test")
            })
        val device = adapter.requestDevice().await()

        val shaderCode = """
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
        """

        val shaderModule = device.createShaderModule(
            GPUShaderModuleDescriptor(code = shaderCode)
        )

        val pipelineLayout = device.createPipelineLayout(
            GPUPipelineLayoutDescriptor(
                bindGroupLayouts = emptyArray()
            )
        )

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

        val (texture, outputBuffer) = createCapture(
            device,
            dimensions.width,
            dimensions.height,
        );

        val encoder = device.createCommandEncoder();
        val renderPass = encoder.beginRenderPass(
            GPURenderPassDescriptor(
                colorAttachments = arrayOf(
                    GPURenderPassColorAttachment(
                        view = texture.createView(),
                        storeOp = GPUStoreOP.STORE,
                        loadOp = GPULoadOP.CLEAR,
                        clearValue = arrayOf(0, 1, 0, 1),
                    ),
                ),
            )
        );
        renderPass.setPipeline(renderPipeline);
        renderPass.draw(3, 1);
        renderPass.end();

        copyToBuffer(encoder, texture, outputBuffer, dimensions);

        device.queue.submit(arrayOf(encoder.finish()));

        createPng(outputBuffer, dimensions)

        device.destroy()
    }

    data class CreateCapture(val texture: GPUTexture, val outputBuffer: GPUBuffer)

    private fun createCapture(
        device: GPUDevice,
        width: Int,
        height: Int,
    ): CreateCapture {
        val padded = getRowPadding(width).padded;
        val outputBuffer = device.createBuffer(
            GPUBufferDescriptor(
                label = "Capture",
                size = padded * height,
                usage = (GPUBufferUsage.MAP_READ or GPUBufferUsage.COPY_DST),
            )
        )
        val texture = device.createTexture(
            GPUTextureDescriptor(
                label = "Capture",
                size = GPUExtent3DDict(
                    width = width,
                    height = height,
                ),
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

    private fun copyToBuffer(
        encoder: GPUCommandEncoder,
        texture: GPUTexture,
        outputBuffer: GPUBuffer,
        dimensions: SizeInt,
    ) {
        val padded = getRowPadding(dimensions.width).padded;

        encoder.copyTextureToBuffer(
            GPUImageCopyTexture(texture = texture),
            GPUImageCopyBuffer(buffer = outputBuffer, bytesPerRow = padded),
            GPUExtent3DDictStrict(width = dimensions.width, height = dimensions.height),
        )
    }

    private suspend fun createPng(
        buffer: GPUBuffer,
        dimensions: SizeInt,
    ) {
        buffer.mapAsync(1).await()
        val inputBuffer = Uint8Array(buffer.getMappedRange())
        val padding = getRowPadding(dimensions.width)
        val padded = padding.padded
        val unpadded = padding.unpadded
        val outputBuffer = Uint8Array(unpadded * dimensions.height);

        for (i in 0 until dimensions.height) {
            val slice: Uint8Array = inputBuffer.asDynamic()
                .slice(i * padded, (i + 1) * padded)
                .slice(0, unpadded)
                .unsafeCast<Uint8Array>()

            outputBuffer.set(slice, (i * unpadded));
        }

        val bitmap = Bitmap32(dimensions.width, dimensions.height, Int32Array(outputBuffer.buffer).unsafeCast<IntArray>())

        RegisteredImageFormats.register(PNG)
        println(DataURL(nativeImageFormatProvider.encodeSuspend(bitmap, ImageEncodingProps("file.png")), "image/png").url)

        buffer.unmap()
    }

}

private external val navigator: NavigatorGPU

/*
import triangleVertWGSL from '../../shaders/triangle.vert.wgsl';
import redFragWGSL from '../../shaders/red.frag.wgsl';

const canvas = document.querySelector('canvas') as HTMLCanvasElement;
const adapter = await navigator.gpu.requestAdapter();
const device = await adapter.requestDevice();

const context = canvas.getContext('webgpu') as GPUCanvasContext;

const devicePixelRatio = window.devicePixelRatio;
canvas.width = canvas.clientWidth * devicePixelRatio;
canvas.height = canvas.clientHeight * devicePixelRatio;
const presentationFormat = navigator.gpu.getPreferredCanvasFormat();

context.configure({
  device,
  format: presentationFormat,
  alphaMode: 'premultiplied',
});

const pipeline = device.createRenderPipeline({
  layout: 'auto',
  vertex: {
    module: device.createShaderModule({
      code: triangleVertWGSL,
    }),
  },
  fragment: {
    module: device.createShaderModule({
      code: redFragWGSL,
    }),
    targets: [
      {
        format: presentationFormat,
      },
    ],
  },
  primitive: {
    topology: 'triangle-list',
  },
});

function frame() {
  const commandEncoder = device.createCommandEncoder();
  const textureView = context.getCurrentTexture().createView();

  const renderPassDescriptor: GPURenderPassDescriptor = {
    colorAttachments: [
      {
        view: textureView,
        clearValue: [0, 0, 0, 1],
        loadOp: 'clear',
        storeOp: 'store',
      },
    ],
  };

  const passEncoder = commandEncoder.beginRenderPass(renderPassDescriptor);
  passEncoder.setPipeline(pipeline);
  passEncoder.draw(3);
  passEncoder.end();

  device.queue.submit([commandEncoder.finish()]);
  requestAnimationFrame(frame);
}

requestAnimationFrame(frame);

 */

/*
@vertex
fn main(
  @builtin(vertex_index) VertexIndex : u32
) -> @builtin(position) vec4f {
  var pos = array<vec2f, 3>(
    vec2(0.0, 0.5),
    vec2(-0.5, -0.5),
    vec2(0.5, -0.5)
  );

  return vec4f(pos[VertexIndex], 0.0, 1.0);
}

 */

/*
@fragment
fn main() -> @location(0) vec4f {
  return vec4(1.0, 0.0, 0.0, 1.0);
}
 */
