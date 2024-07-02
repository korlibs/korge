package korlibs.webgpu

import io.ygdrasil.wgpu.internal.js.*
import korlibs.io.async.*
import korlibs.js.*
import korlibs.render.*
import kotlinx.browser.*
import kotlinx.coroutines.*
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
            val commandEncoder = device.createCommandEncoder();
            val textureView = context.getCurrentTexture().createView();

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
}

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
