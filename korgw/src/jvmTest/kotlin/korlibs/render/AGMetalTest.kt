package korlibs.render

import com.sun.jna.*
import korlibs.datastructure.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.memory.Platform
import korlibs.memory.dyn.osx.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Test
import kotlin.test.*


interface CoreGraphics : Library {

}

interface MetalGlobals : Library {
    ///System/Library/Frameworks/Metal.framework/Versions/A/Metal
    //fun _MTLCreateSystemDefaultDevice(): Pointer?
    fun MTLCreateSystemDefaultDevice(): Pointer?
    //fun MTLClearColorMake(red: Double, green: Double, blue: Double, alpha: Double): MTLClearColor.ByValue
    //fun MTLRegionMake2D(x: Double, y: Double, width: Double, height: Double): MTLRegion.ByValue
}

open class MTLRegion : Structure {
    override fun getFieldOrder() = listOf("x", "y", "z", "width", "height", "depth")
    companion object {
        fun make2D(x: Long, y: Long, width: Long, height: Long): MTLRegion.ByValue {
            return MTLRegion.ByValue().also {
                it.x = x
                it.y = y
                it.width = width
                it.height = height
                it.autoWrite()
                it.write()
            }
        }
    }
    @JvmField var x: Long = 0L
    @JvmField var y: Long = 0L
    @JvmField var z: Long = 0L
    @JvmField var width: Long = 0L
    @JvmField var height: Long = 0L
    @JvmField var depth: Long = 1L
    //override fun getFieldOrder() = listOf("origin", "size")
    //@JvmField var origin: MTLOrigin = MTLOrigin()
    //@JvmField var size: MTLSize = MTLSize()

    constructor() : super() {}
    constructor(peer: Pointer?) : super(peer) {}


    class ByReference : MTLRegion(), Structure.ByReference
    class ByValue : MTLRegion(), Structure.ByValue
}

open class MTLOrigin : Structure {
    override fun getFieldOrder() = listOf("x", "y", "z")
    @JvmField var x: Double = 0.0
    @JvmField var y: Double = 0.0
    @JvmField var z: Double = 0.0

    constructor() : super() {}
    constructor(peer: Pointer?) : super(peer) {}

    class ByReference : MTLOrigin(), Structure.ByReference
    class ByValue : MTLOrigin(), Structure.ByValue
}

open class MTLSize : Structure {
    override fun getFieldOrder() = listOf("width", "height", "depth")
    @JvmField var width: Double = 0.0
    @JvmField var height: Double = 0.0
    @JvmField var depth: Double = 0.0

    constructor() : super() {}
    constructor(peer: Pointer?) : super(peer) {}

    class ByReference : MTLSize(), Structure.ByReference
    class ByValue : MTLSize(), Structure.ByValue
}

open class MTLClearColor : Structure {
    @JvmField var red: Double = 0.0
    @JvmField var green: Double = 0.0
    @JvmField var blue: Double = 0.0
    @JvmField var alpha: Double = 0.0

    constructor() : super() {}
    constructor(peer: Pointer?) : super(peer) {}

    override fun getFieldOrder() = listOf("red", "green", "blue", "alpha")

    class ByReference : MTLClearColor(), Structure.ByReference
    class ByValue : MTLClearColor(), Structure.ByValue
}

// https://developer.apple.com/documentation/objectivec/objective-c_runtime
// https://github.com/korlibs/korge/discussions/1155

class AGMetalTest {
    interface MTLArchitecture : ObjcDynamicInterface {
        @get:ObjcDesc("name", "@16@0:8") val name: String
    }
    interface MTLFunction : ObjcDynamicInterface {

    }
    interface MTLLibrary : ObjcDynamicInterface {
        @ObjcDesc("newFunctionWithName:", "@24@0:8@16") fun newFunction(name: NSString): MTLFunction?
    }
    interface MTLRenderPipelineState : ObjcDynamicInterface {

    }
    interface MTLCommandEncoder : ObjcDynamicInterface {
        @ObjcDesc("endEncoding", "v16@0:8") fun endEncoding(): Unit
    }
    interface MTLRenderCommandEncoder : MTLCommandEncoder {
        @ObjcDesc("setRenderPipelineState:", "v24@0:8@16") fun setRenderPipelineState(setRenderPipelineState: MTLRenderPipelineState?): Unit
        @ObjcDesc("setVertexBuffer:offset:atIndex:", "v40@0:8@16Q24Q32") fun setVertexBuffer(setVertexBuffer: MTLBuffer?, offset: Long, atIndex: Long): Unit
        @ObjcDesc("drawPrimitives:vertexStart:vertexCount:instanceCount:", "v48@0:8Q16Q24Q32Q40") fun drawPrimitives(primitiveType: Long, vertexStart: Long, vertexCount: Long, instanceCount: Long): Unit

    }
    interface MTLCommandBuffer : ObjcDynamicInterface {
        @ObjcDesc("renderCommandEncoderWithDescriptor:", "@24@0:8@16") fun renderCommandEncoder(descriptor: MTLRenderPassDescriptor?): MTLRenderCommandEncoder?
        @ObjcDesc("presentDrawable:", "v24@0:8@16") fun presentDrawable(presentDrawable: CAMetalDrawable?): Unit
        @ObjcDesc("commit", "v16@0:8") fun commit(): Unit
        @ObjcDesc("waitUntilCompleted", "v16@0:8") fun waitUntilCompleted(): Unit
    }
    interface MTLCommandQueue : ObjcDynamicInterface {
        @ObjcDesc("commandBuffer", "@16@0:8") fun commandBuffer(): MTLCommandBuffer?
    }
    interface MTLDevice : ObjcDynamicInterface {
        @get:ObjcDesc("architecture", "@16@0:8") val architecture: MTLArchitecture
        @get:ObjcDesc("name", "@16@0:8") val name: String
        @get:ObjcDesc("maxTransferRate", "Q16@0:8") val maxTransferRate: Long
        @get:ObjcDesc("maxBufferLength", "Q16@0:8") val maxBufferLength: Long
        @ObjcDesc("newCommandQueue", "@16@0:8") fun newCommandQueue(): MTLCommandQueue?

        @ObjcDesc("newBufferWithLength:options:", "@32@0:8Q16Q24") fun newBuffer(length: Long, options: Long): MTLBuffer
        @ObjcDesc("newLibraryWithSource:options:error:", "@40@0:8@16@24^@32") fun newLibrary(source: NSString, options: Pointer?, error: Pointer?): MTLLibrary?
        @ObjcDesc("newRenderPipelineStateWithDescriptor:error:", "@32@0:8@16^@24") fun newRenderPipelineState(descriptor: MTLRenderPipelineDescriptor, error: Pointer?): MTLRenderPipelineState?
    }

    interface MTLBuffer : ObjcDynamicInterface {
        @get:ObjcDesc("length", "Q16@0:8") val length: Long
        @get:ObjcDesc("contents", "^v16@0:8") val contents: Pointer
    }

    interface MTLRenderPassColorAttachmentDescriptor : ObjcDynamicInterface {
        @get:ObjcDesc("texture", "Q16@0:8") @set:ObjcDesc("setTexture:", "Q16@0:8") var texture: MTLTexture?
        @get:ObjcDesc("loadAction", "Q16@0:8") @set:ObjcDesc("setLoadAction:", "Q16@0:8") var loadAction: Long
        @get:ObjcDesc("clearColor", "Q16@0:8") @set:ObjcDesc("setClearColor:", "Q16@0:8") var clearColor: MTLClearColor.ByValue
    }

    interface MTLRenderPassColorAttachmentDescriptorArray : ObjcDynamicInterface {
        @ObjcDesc("objectAtIndexedSubscript:", "@24@0:8Q16") fun objectAtIndexedSubscript(objectAtIndexedSubscript: Long): MTLRenderPassColorAttachmentDescriptor
        @ObjcDesc("setObject:atIndexedSubscript:", "v32@0:8@16Q24") fun setObject(setObject: MTLRenderPassColorAttachmentDescriptor, atIndexedSubscript: Long): Unit
        @ObjcDesc("_descriptorAtIndex:", "@24@0:8Q16") fun _descriptorAtIndex(_descriptorAtIndex: Long): MTLRenderPassColorAttachmentDescriptor
    }

    interface MTLRenderPassDescriptor : ObjcDynamicInterface {
        companion object {
            operator fun invoke(): MTLRenderPassDescriptor = ObjcDynamicInterface.createNew<MTLRenderPassDescriptor>()
        }
        @get:ObjcDesc("colorAttachments") val colorAttachments: MTLRenderPassColorAttachmentDescriptorArray
    }

    interface MTLRenderPipelineColorAttachmentDescriptor : ObjcDynamicInterface {
        @get:ObjcDesc("pixelFormat", "Q16@0:8") @set:ObjcDesc("setPixelFormat:", "Q16@0:8") var pixelFormat: Long
        @get:ObjcDesc("texture", "Q16@0:8") @set:ObjcDesc("setTexture:", "Q16@0:8") var texture: MTLTexture?
    }

    interface MTLRenderPipelineColorAttachmentDescriptorArray : ObjcDynamicInterface {
        @ObjcDesc("objectAtIndexedSubscript:", "@24@0:8Q16") fun objectAtIndexedSubscript(objectAtIndexedSubscript: Long): MTLRenderPipelineColorAttachmentDescriptor
        @ObjcDesc("setObject:atIndexedSubscript:", "v32@0:8@16Q24") fun setObject(setObject: MTLRenderPipelineColorAttachmentDescriptor, atIndexedSubscript: Long): Unit
    }

    @ObjcDesc("MTLRenderPipelineDescriptor")
    interface MTLRenderPipelineDescriptor : ObjcDynamicInterface {
        companion object {
            operator fun invoke(): MTLRenderPipelineDescriptor = ObjcDynamicInterface.createNew<MTLRenderPipelineDescriptor>()
        }

        @get:ObjcDesc("colorAttachments", "Q16@0:8") val colorAttachments: MTLRenderPipelineColorAttachmentDescriptorArray
        @get:ObjcDesc("vertexFunction", "Q16@0:8") @set:ObjcDesc("setVertexFunction:", "Q16@0:8") var vertexFunction: MTLFunction?
        @get:ObjcDesc("fragmentFunction", "Q16@0:8") @set:ObjcDesc("setFragmentFunction:", "Q16@0:8") var fragmentFunction: MTLFunction?

        @ObjcDesc("setVertexPreloadedLibraries:", "v24@0:8@16") fun setVertexPreloadedLibraries(setVertexPreloadedLibraries: MTLFunction?): Unit
        @ObjcDesc("setFragmentPreloadedLibraries:", "v24@0:8@16") fun setFragmentPreloadedLibraries(setFragmentPreloadedLibraries: MTLFunction?): Unit
    }

    interface CAMetalDrawable : ObjcDynamicInterface {
        @get:ObjcDesc("texture", "Q16@0:8") val texture: MTLTexture?
    }

    interface MTLTexture : ObjcDynamicInterface {
        @ObjcDesc("getBytes:bytesPerRow:bytesPerImage:fromRegion:mipmapLevel:slice:", "v104@0:8^v16Q24Q32{?={?=QQQ}{?=QQQ}}40Q88Q96") fun getBytes(
            getBytes: Pointer, bytesPerRow: Long, bytesPerImage: Long, fromRegion: MTLRegion.ByValue, mipmapLevel: Long, slice: Long): Unit
        @get:ObjcDesc("width", "Q16@0:8") val width: Long
        @get:ObjcDesc("height", "Q16@0:8") val height: Long
        @get:ObjcDesc("textureType", "Q16@0:8") val textureType: Long
        @get:ObjcDesc("buffer", "@16@0:8") val buffer: MTLBuffer?
        @get:ObjcDesc("bufferBytesPerRow", "Q16@0:8") val bufferBytesPerRow: Long
        @get:ObjcDesc("bufferOffset", "Q16@0:8") val bufferOffset: Long
        @get:ObjcDesc("depth", "Q16@0:8") val depth: Long
    }

    @ObjcDesc("CAMetalLayer")
    interface CAMetalLayer : ObjcDynamicInterface {
        companion object {
            operator fun invoke(): CAMetalLayer = ObjcDynamicInterface.createNew<CAMetalLayer>()
        }
        //@get:ObjcDesc("pixelFormat") @set:ObjcDesc("setPixelFormat:") var pixelFormat: Long
        @get:ObjcDesc("pixelFormat") val pixelFormat: Long
        @get:ObjcDesc("device") val device: MTLDevice?
        @get:ObjcDesc("colorspace", "^{CGColorSpace=}16@0:8") val colorspace: Pointer
        @ObjcDesc("nextDrawable", "@16@0:8") fun nextDrawable(): CAMetalDrawable?
        @ObjcDesc("setPixelFormat:", "v24@0:8Q16") fun setPixelFormat(setPixelFormat: Long): Unit
        @ObjcDesc("setDevice:", "v24@0:8@16") fun setDevice(device: MTLDevice?): Unit
        @ObjcDesc("setFramebufferOnly:", "v20@0:8B16") fun setFramebufferOnly(setFramebufferOnly: Boolean): Unit
        @ObjcDesc("setFrame:", "v48@0:8{CGRect={CGPoint=dd}{CGSize=dd}}16") fun setFrame(frame: CGRect): Unit
    }

    open class CGRect : Structure {
        @JvmField var x: Double = 0.0
        @JvmField var y: Double = 0.0
        @JvmField var width: Double = 0.0
        @JvmField var height: Double = 0.0

        constructor() : super() {}
        constructor(peer: Pointer?) : super(peer) {}

        override fun getFieldOrder() = listOf("x", "y", "width", "height")

        class ByReference : CGRect(), Structure.ByReference
        class ByValue : CGRect(), Structure.ByValue
    }

    fun <T> nsAutoreleasePool(block: () -> T): T {
        val pool = NSClass("NSAutoreleasePool").alloc().msgSend("init")
        try {
            return block()
        } finally {
            pool.msgSend("release")
        }
    }

    @Test
    fun test() {
        // https://developer.apple.com/documentation/objectivec/objective-c_runtime
        // https://github.com/korlibs/korge/discussions/1155
        if (Platform.isMac) {
            nsAutoreleasePool {
                val cg = Native.load("/System/Library/Frameworks/CoreGraphics.framework/Versions/A/CoreGraphics", CoreGraphics::class.java)
                val metal = Native.load("/System/Library/Frameworks/Metal.framework/Versions/A/Metal", MetalGlobals::class.java)
                val device = metal.MTLCreateSystemDefaultDevice()?.asObjcDynamicInterface<MTLDevice>()

                val MTLPixelFormatBGRA8Unorm = 80L

                val MTLPrimitiveTypePoint = 0L
                val MTLPrimitiveTypeLine = 1L
                val MTLPrimitiveTypeLineStrip = 2L
                val MTLPrimitiveTypeTriangle = 3L
                val MTLPrimitiveTypeTriangleStrip = 4L

                val MTLLoadActionDontCare = 0L
                val MTLLoadActionLoad = 1L
                val MTLLoadActionClear = 2L

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
    }

    private fun dumpKotlin(name: String) {
        ObjcClassRef.fromName(name)?.dumpKotlin()
        ObjcProtocolRef.fromName(name)?.dumpKotlin()
    }
}
