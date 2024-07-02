package korlibs.webgpu

import io.ygdrasil.wgpu.internal.js.*
import korlibs.image.bitmap.*
import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.js.*
import korlibs.math.geom.*
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
            jsObjectOf(
                "device" to device,
                "format" to presentationFormat,
                "alphaMode" to "premultiplied",
            ).unsafeCast<GPUCanvasConfiguration>()
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
            jsObjectOf(
                "layout" to "auto",
                "vertex" to jsObjectOf(
                    "module" to device.createShaderModule(
                        jsObjectOf(
                            "code" to triangleVertWGSL,
                        )
                    ),
                ),
                "fragment" to jsObjectOf(
                    "module" to device.createShaderModule(
                        jsObjectOf(
                            "code" to redFragWGSL,
                        )
                    ),
                    "targets" to arrayOf(
                        jsObjectOf(
                            "format" to presentationFormat,
                        ),
                    ),
                ),
                "primitive" to jsObjectOf(
                    "topology" to "triangle-list",
                ),
            )
        );

        fun frame() {
            val commandEncoder = device.createCommandEncoder()
            val textureView = context.getCurrentTexture().createView()

            val renderPassDescriptor: GPURenderPassDescriptor = jsObjectOf(
                "colorAttachments" to arrayOf(
                    jsObjectOf(
                        "view" to textureView,
                        "clearValue" to arrayOf(0, 0, 0, 1),
                        "loadOp" to "clear",
                        "storeOp" to "store",
                    ),
                ),
            );

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
            jsObjectOf("code" to shaderCode)
        );

        val pipelineLayout = device.createPipelineLayout(
            jsObjectOf(
                "bindGroupLayouts" to jsEmptyArray(),
            )
        );

        val renderPipeline = device.createRenderPipeline(
            jsObjectOf(
                "layout" to pipelineLayout,
                "vertex" to jsObjectOf(
                    "module" to shaderModule,
                    "entryPoint" to "vs_main",
                ),
                "fragment" to jsObjectOf(
                    "module" to shaderModule,
                    "entryPoint" to "fs_main",
                    "targets" to arrayOf(
                        jsObjectOf(
                            "format" to "rgba8unorm-srgb",
                        ),
                    ),
                ),
            )
        );

        val (texture, outputBuffer) = createCapture(
            device,
            dimensions.width,
            dimensions.height,
        );

        val encoder = device.createCommandEncoder();
        val renderPass = encoder.beginRenderPass(
            jsObjectOf(
                "colorAttachments" to arrayOf(
                    jsObjectOf(
                        "view" to texture.createView(),
                        "storeOp" to "store",
                        "loadOp" to "clear",
                        "clearValue" to arrayOf(0, 1, 0, 1),
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
    }

    data class CreateCapture(val texture: GPUTexture, val outputBuffer: GPUBuffer)

    private fun createCapture(
        device: GPUDevice,
        width: Int,
        height: Int,
    ): CreateCapture {
        val padded = getRowPadding(width).padded;
        val outputBuffer = device.createBuffer(jsObjectOf(
            "label" to "Capture",
            "size" to padded * height,
            "usage" to (GPUBufferUsage.MAP_READ or GPUBufferUsage.COPY_DST),
        ));
        val texture = device.createTexture(jsObjectOf(
            "label" to "Capture",
            "size" to jsObjectOf(
                "width" to width,
                "height" to height,
            ),
            "format" to "rgba8unorm-srgb",
            "usage" to (GPUTextureUsage.RENDER_ATTACHMENT or GPUTextureUsage.COPY_SRC),
        ));

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
            jsObjectOf(
                    "texture" to texture,
            ).unsafeCast<GPUImageCopyTexture>(),
            jsObjectOf(
                    "buffer" to outputBuffer,
                    "bytesPerRow" to padded,
            ).unsafeCast<GPUImageCopyBuffer>(),
            jsObjectOf("width" to dimensions.width, "height" to dimensions.height).unsafeCast<GPUExtent3DDictStrict>(),
        );
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

        val nativeImage = nativeImageFormatProvider.create(dimensions.width, dimensions.height).unsafeCast<HtmlNativeImage>()
        //HtmlNativeImage()
        //println(PNG.encode(bitmap).base64)
        nativeImage.context2d {
            drawImage(bitmap, Point(0, 0))
        }

        println(nativeImage.element.unsafeCast<HTMLCanvasElement>().toDataURL("image/png"))

        //PNG.encode()
        //val image = png.encode(
        //    outputBuffer,
        //dimensions.width,
        //dimensions.height,
        //{
        //        stripAlpha: true,
        //        color: 2,
        //},
        //);
        //Deno.writeFileSync("./output.png", image);

        buffer.unmap();
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
