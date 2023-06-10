package korlibs.render.osx.metal

import com.sun.jna.*
import korlibs.datastructure.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.memory.Platform
import korlibs.memory.dyn.osx.*
import org.junit.Test
import kotlin.test.*

class AGMetalTest {
    @Test
    fun test() {
        if (!Platform.isMac) return

        // https://developer.apple.com/documentation/objectivec/objective-c_runtime
        // https://github.com/korlibs/korge/discussions/1155

        nsAutoreleasePool {
            val device = MetalGlobals.MTLCreateSystemDefaultDevice()?.asObjcDynamicInterface<MTLDevice>()

            if (device != null) {
                val width = 50
                val height = 50
                //println(device.name)
                //println(device.architecture.name)
                //println(device.maxTransferRate)
                //println(device.maxBufferLength)

                //NSClass("CAMetalLayer").alloc().msgSend("init").msgSend("setPixelFormat", 1L)

                val layer = CAMetalLayer()
                layer.setPixelFormat(MTLPixelFormatBGRA8Unorm)
                layer.setDevice(device)
                layer.setFramebufferOnly(false)
                //layer.setFrame(CGRect.ByValue().also {
                layer.setFrame(CGRect.ByValue().also {
                    it.x = 0.0
                    it.y = 0.0
                    it.width = width.toDouble()
                    it.height = height.toDouble()
                }.also { it.write() })
                //layer.pixelFormat = 1
                //layer.setPixelFormat(1L)
                println(layer.pixelFormat)
                //println(layer.device)
                println(layer.colorspace)
                //println(layer.colorspace)

                val data = floatArrayOf(0.0f,  1.0f, 0.0f, -1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f)
                val vertexBuffer = device.newBuffer(length = (data.size * Float.SIZE_BYTES).toLong(), options = 0L)
                vertexBuffer.contents.let {
                    for (n in 0 until data.size) {
                        val v = data[n]
                        it.setFloat(n * 4L, v)
                    }
                }
                println("vertexBuffer.contents=${vertexBuffer.contents}")
                println("vertexBuffer.length=${vertexBuffer.length}")
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
                """.trimIndent()), null, null)

                println("library=$library")
                val vertexFunction = library!!.newFunction(NSString("basic_vertex")) !!
                val fragmentFunction = library!!.newFunction(NSString("basic_fragment")) !!

                val pipelineStateDescriptor = MTLRenderPipelineDescriptor()
                println("pipelineStateDescriptor=${pipelineStateDescriptor}")
                println("pipelineStateDescriptor.vertexFunction=${pipelineStateDescriptor.vertexFunction}")
                pipelineStateDescriptor.vertexFunction = vertexFunction
                pipelineStateDescriptor.fragmentFunction = fragmentFunction
                pipelineStateDescriptor.colorAttachments.objectAtIndexedSubscript(0).pixelFormat = MTLPixelFormatBGRA8Unorm
                println("pipelineStateDescriptor.vertexFunction=${pipelineStateDescriptor.vertexFunction}")
                println("pipelineStateDescriptor.fragmentFunction=${pipelineStateDescriptor.fragmentFunction}")
                println("pipelineStateDescriptor.colorAttachments.objectAtIndexedSubscript(0).pixelFormat=${pipelineStateDescriptor.colorAttachments.objectAtIndexedSubscript(0).pixelFormat}")

                val pipelineState = device.newRenderPipelineState(pipelineStateDescriptor, error = null)!!
                val commandQueue = device.newCommandQueue()!!

                val drawable = layer.nextDrawable()
                val renderPassDescriptor = MTLRenderPassDescriptor()
                val colorAttachment = renderPassDescriptor.colorAttachments.objectAtIndexedSubscript(0L)
                //println("drawable?.texture=${drawable?.texture}")
                colorAttachment.texture = drawable!!.texture
                colorAttachment.loadAction = MTLLoadActionClear
                colorAttachment.clearColor = MTLClearColor.ByValue().also {
                    it.autoWrite()
                    it.red = 0.0
                    it.green = 104.0/255.0
                    it.blue = 55.0/255.0
                    it.alpha = 1.0
                }.also { it.write() }

                //println("colorAttachment=$colorAttachment")
                println("renderPassDescriptor=$renderPassDescriptor")
                println("drawable=$drawable")
                println("commandQueue=$commandQueue")
                println("pipelineState=$pipelineState")
                println("vertexFunction=$vertexFunction")
                println("fragmentFunction=$fragmentFunction")

                val commandBuffer = commandQueue.commandBuffer() ?: TODO()
                println("commandBuffer=$commandBuffer")

                val renderEncoder = commandBuffer.renderCommandEncoder(renderPassDescriptor) ?: TODO()
                renderEncoder.setRenderPipelineState(pipelineState)
                renderEncoder.setVertexBuffer(vertexBuffer, offset = 0, atIndex = 0)
                renderEncoder.drawPrimitives(primitiveType = MTLPrimitiveTypeTriangle, vertexStart = 0, vertexCount = 3, instanceCount = 1)
                renderEncoder.endEncoding()

                commandBuffer.presentDrawable(drawable)
                commandBuffer.commit()
                commandBuffer.waitUntilCompleted()

                val dataOut = Memory((width * height * 4).toLong()).also { it.clear() }

                println("drawable!!.texture!!=${drawable.texture!!.width}x${drawable.texture!!.height}")
                println("MTLRegion.make2D(0L, 0L, width.toLong(), height.toLong())=${MTLRegion.make2D(0L, 0L, width.toLong(), height.toLong())}")

                //println("drawable!!.texture!!.buffer=${drawable!!.texture!!.buffer}")
                //println("drawable!!.texture!!.bufferOffset=${drawable!!.texture!!.bufferOffset}")
                //println("drawable!!.texture!!.bufferBytesPerRow=${drawable!!.texture!!.bufferBytesPerRow}")

                drawable.texture!!.getBytes(
                    dataOut,
                    (width * 4).toLong(),
                    (width * height * 4).toLong(),
                    MTLRegion.make2D(0L, 0L, width.toLong(), height.toLong()),
                    0L,
                    0L
                )
                val dataPixels = IntArray2(width, height) { dataOut.getInt((4 * it).toLong()) }
                val bmp = Bitmap32(width, height, RgbaArray(dataPixels.data))

                assertEquals(Colors["#376800"], bmp[0, 0])
                assertEquals(Colors["#ff4141"], bmp[25, 25])
                //runBlocking { bmp.showImageAndWait() }
            }

            //dumpKotlin("MTLCommandQueue")
            //dumpKotlin("MTLCommandBuffer")
            //dumpKotlin("MTLCommandEncoder")
            //dumpKotlin("MTLRenderCommandEncoder")
            dumpKotlin("MTLTexture")
        }
    }

    private fun dumpKotlin(name: String) {
        ObjcClassRef.fromName(name)?.dumpKotlin()
        ObjcProtocolRef.fromName(name)?.dumpKotlin()
    }
}
