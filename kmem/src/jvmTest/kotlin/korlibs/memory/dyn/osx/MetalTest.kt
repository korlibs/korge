package korlibs.memory.dyn.osx

import com.sun.jna.*
import korlibs.memory.Platform
import kotlin.test.*

interface CoreGraphics : Library {

}

interface MetalGlobals : Library {
    ///System/Library/Frameworks/Metal.framework/Versions/A/Metal
    //fun _MTLCreateSystemDefaultDevice(): Pointer?
    fun MTLCreateSystemDefaultDevice(): Pointer?
}

class MetalTest {
    interface MTLArchitecture : ObjcDynamicInterface {
        @get:ObjcDesc("name", "@16@0:8") val name: String
    }
    interface MTLLibrary : ObjcDynamicInterface {

    }
    interface MTLDevice : ObjcDynamicInterface {
        @get:ObjcDesc("architecture", "@16@0:8") val architecture: MTLArchitecture
        @get:ObjcDesc("name", "@16@0:8") val name: String
        @get:ObjcDesc("maxTransferRate", "Q16@0:8") val maxTransferRate: Long
        @get:ObjcDesc("maxBufferLength", "Q16@0:8") val maxBufferLength: Long
        @get:ObjcDesc("newCommandQueue", "@16@0:8") val newCommandQueue: ID

        @ObjcDesc("newBufferWithLength:options:", "@32@0:8Q16Q24") fun newBuffer(length: Long, options: Long): MTLBuffer
        @ObjcDesc("newLibraryWithSource:options:error:", "@40@0:8@16@24^@32") fun newLibrary(source: NSString, options: Pointer?, error: Pointer?): MTLLibrary?
    }

    interface MTLBuffer : ObjcDynamicInterface {
        @get:ObjcDesc("length", "Q16@0:8") val length: Long
        @get:ObjcDesc("contents", "^v16@0:8") val contents: Pointer
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
        @ObjcDesc("setPixelFormat:", "v24@0:8Q16") fun setPixelFormat(setPixelFormat: Long): Unit
        @ObjcDesc("setDevice:", "v24@0:8@16") fun setDevice(device: MTLDevice?): Unit
        @ObjcDesc("setFramebufferOnly:", "v20@0:8B16") fun setFramebufferOnly(setFramebufferOnly: Boolean): Unit
        @ObjcDesc("setFrame:", "v48@0:8{CGRect={CGPoint=dd}{CGSize=dd}}16") fun setFrame(frame: CGRect.ByValue): Unit
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

    @Test
    fun test() {
        // https://developer.apple.com/documentation/objectivec/objective-c_runtime
        // https://github.com/korlibs/korge/discussions/1155
        if (Platform.isMac) {
            val cg = Native.load("/System/Library/Frameworks/CoreGraphics.framework/Versions/A/CoreGraphics", CoreGraphics::class.java)
            val metal = Native.load("/System/Library/Frameworks/Metal.framework/Versions/A/Metal", MetalGlobals::class.java)
            val device = metal.MTLCreateSystemDefaultDevice()?.asObjcDynamicInterface<MTLDevice>()

            val MTLPixelFormatBGRA8Unorm = 80L

            if (device != null) {
                println(device.name)
                println(device.architecture.name)
                println(device.maxTransferRate)
                println(device.maxBufferLength)

                //NSClass("CAMetalLayer").alloc().msgSend("init").msgSend("setPixelFormat", 1L)
                val pool = NSClass("NSAutoreleasePool").alloc().msgSend("init")

                val layer = CAMetalLayer()
                layer.setPixelFormat(MTLPixelFormatBGRA8Unorm)
                layer.setDevice(device)
                layer.setFramebufferOnly(false)
                layer.setFrame(CGRect.ByValue().also {
                    it.x = 0.0
                    it.y = 0.0
                    it.width = 512.0
                    it.height = 512.0
                })
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
                        it.setFloat(n * 8L, v)
                    }
                }
                println(vertexBuffer.contents)
                println(vertexBuffer.length)
                val library = device.newLibrary(NSString("""
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

                //pool.autorelease()
                pool.msgSend("release")
            }


            //ObjcProtocolRef.fromName("MTLBuffer")!!.dumpKotlin()
            ObjcProtocolRef.fromName("MTLDevice")!!.dumpKotlin()
            //ObjcClassRef.fromName("CAMetalLayer")!!.dumpKotlin()
            //ObjcClassRef.fromName("CALayer")!!.dumpKotlin()

            //ObjcClassRef.fromName("CAMetalLayer")!!.createInstance().asObjcDynamicInterface<CAMetal>()


            /*
            ObjcDynamicInterface.proxy(metalDevice, MTLDevice::class)
            println(metalDevice?.address?.msgSend("hasUnifiedMemory"))

            println(NSString(metalDevice?.address?.msgSend("name")).cString)
            println(NSString(metalDevice?.address?.msgSend("architecture")?.msgSend("name")).cString)
            //println()
            val protocol = ObjcProtocolRef.getByName("MTLDevice")!!
            protocol.dumpKotlin()
            println("protocol=$protocol")
            //println(protocol.ref.msgSend("name").toPointer().getString(0L))
            //println(protocol.ref.msgSend("hasUnifiedMemory"))
            //for (method in protocol.listMethods()) {
            //    println(" - ${method}")
            //}
            println(ObjcProtocolRef.listAll())
            println(ObjectiveC.getClassByName("_MTLDevice")!!.imageName)
            /*
            val MTLDevice = ObjectiveC.getClassByName("_MTLDevice")!!
            //println(ObjectiveC.getAllClassIDs())
            for (method in MTLDevice.listMethods()) {
                println("$method")
            }

             */
            //ObjectiveC.objc_getClass("")

             */
        }
    }
}
