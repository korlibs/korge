package korlibs.render.osx.metal

import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.memory.*
import korlibs.memory.dyn.osx.*
import kotlinx.coroutines.*
import org.junit.Test
import kotlin.test.*

// https://developer.apple.com/documentation/objectivec/objective-c_runtime
// https://github.com/korlibs/korge/discussions/1155
class AGMetalTest {
    @Test
    fun test() = macTestWithAutoreleasePool{
        val device = MetalGlobals.MTLCreateSystemDefaultDevice()?.asObjcDynamicInterface<MTLDevice>()
            ?: error("Can't get MTLDevice")

        println("device.name=${device.name}")
        println("device.architecture.name=${device.architecture.name}")
        println("device.maxBufferLength=${device.maxBufferLength}")

        val width = 50
        val height = 50

        //NSClass("CAMetalLayer").alloc().msgSend("init").msgSend("setPixelFormat", 1L)

        val layer = CAMetalLayer().also {
            it.setPixelFormat(MTLPixelFormatBGRA8Unorm)
            it.setDevice(device)
            it.setFramebufferOnly(false)
            it.setFrame(CGRect.make(0.0, 0.0, width.toDouble(), height.toDouble()))
        }

        val data = floatArrayOf(0.0f,  1.0f, 0.0f, -1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f)
        val vertexBuffer = device.newBuffer(length = (data.size * Float.SIZE_BYTES).toLong(), options = 0L).also {
            it.contents.write(0L, data, 0, data.size)
        }
        //println("vertexBuffer.contents=${vertexBuffer.contents}")
        //println("vertexBuffer.length=${vertexBuffer.length}")
        val library = device.newLibrary(/*language=c*/NSString("""
            typedef struct {
                packed_float3 position;
                // [[flat]]
            } VertexInput;
            
            typedef struct {
                float4 position [[position]];
            } Varyings;
            
            vertex Varyings basic_vertex(
                unsigned int vid [[ vertex_id ]],
                const device VertexInput* vertex_array [[buffer(0)]]
            ) {
                Varyings out;
                auto input = vertex_array[vid];
                {
                    out.position = float4(input.position, 1.0); 
                }
                return out;
            }
            
            fragment half4 basic_fragment(
                Varyings in [[stage_in]]
            ) { // 1
              return half4(in.position.x / 100.0, in.position.y / 100.0, 1.0, 1.0);              // 2
            }
        """.trimIndent()), null, null) ?: error("Can't create MTLLibrary")

        val drawable = layer.nextDrawable() ?: error("Can't find nextDrawable")
        val renderPassDescriptor = MTLRenderPassDescriptor().also {
            it.colorAttachments.objectAtIndexedSubscript(0L).also {
                it.texture = drawable.texture
                it.loadAction = MTLLoadActionClear
                it.clearColor = MTLClearColor.make(0.0, 104.0/255.0, 55.0/255.0, 1.0)
            }
        }

        val commandBuffer = device.newCommandQueue()!!.commandBuffer() ?: error("Can't create MTLCommandBuffer")

        val renderEncoder = commandBuffer.renderCommandEncoder(renderPassDescriptor)?.also {
            val pipelineStateDescriptor = MTLRenderPipelineDescriptor().also {
                it.vertexFunction = library.newFunction(NSString("basic_vertex")) ?: error("Can't find basic_vertex")
                it.fragmentFunction = library.newFunction(NSString("basic_fragment")) ?: error("Can't find basic_fragment")
                it.colorAttachments.objectAtIndexedSubscript(0).pixelFormat = MTLPixelFormatBGRA8Unorm
            }
            it.setRenderPipelineState(device.newRenderPipelineState(pipelineStateDescriptor, error = null) ?: error("Can't create a MTLRenderPipelineState"))
            it.setVertexBuffer(vertexBuffer, offset = 0, atIndex = 0)
            it.drawPrimitives(primitiveType = MTLPrimitiveTypeTriangle, vertexStart = 0, vertexCount = 3, instanceCount = 1)
            it.endEncoding()
        } ?: error("Can't create MTLRenderCommandEncoder")

        commandBuffer.presentDrawable(drawable)
        commandBuffer.commit()
        commandBuffer.waitUntilCompleted()

        val bmp = drawable.texture!!.readBitmap()

        assertEquals(Colors["#376800"], bmp[0, 0])
        assertEquals(Colors["#ff4141"], bmp[25, 25])
        assertEquals(Colors["#ff7e04"], bmp[1, 49])
        assertEquals(Colors["#ff7e41"], bmp[25, 49])
        assertEquals(Colors["#ff7e7e"], bmp[49, 49])
        //runBlocking { bmp.showImageAndWait() }

        //generateKotlinCode("MTLCommandQueue")
        //generateKotlinCode("MTLCommandBuffer")
        //generateKotlinCode("MTLCommandEncoder")
        //generateKotlinCode("MTLRenderCommandEncoder")
        generateKotlinCode("MTLTexture")
    }

    private fun generateKotlinCode(name: String) {
        ObjcClassRef.fromName(name)?.dumpKotlin()
        ObjcProtocolRef.fromName(name)?.dumpKotlin()
    }

    private fun macTestWithAutoreleasePool(block: () -> Unit) {
        if (!Platform.isMac) return
        nsAutoreleasePool {
            block()
        }
    }
}
