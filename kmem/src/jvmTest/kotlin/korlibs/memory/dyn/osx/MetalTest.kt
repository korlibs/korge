package korlibs.memory.dyn.osx

import com.sun.jna.*
import korlibs.memory.Platform
import korlibs.memory.dyn.*
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
        @get:ObjcDescriptor("name", "@16@0:8") val name: String
    }
    interface MTLDevice : ObjcDynamicInterface {
        @get:ObjcDescriptor("architecture", "@16@0:8") val architecture: MTLArchitecture
        @get:ObjcDescriptor("name", "@16@0:8") val name: String
        @get:ObjcDescriptor("maxTransferRate", "Q16@0:8") val maxTransferRate: Long
        @get:ObjcDescriptor("maxBufferLength", "Q16@0:8") val maxBufferLength: Long
        @get:ObjcDescriptor("newCommandQueue", "@16@0:8") val newCommandQueue: ID
    }
    @Test
    fun test() {
        // https://developer.apple.com/documentation/objectivec/objective-c_runtime
        if (Platform.isMac) {
            val cg = Native.load("/System/Library/Frameworks/CoreGraphics.framework/Versions/A/CoreGraphics", CoreGraphics::class.java)
            val metal = Native.load("/System/Library/Frameworks/Metal.framework/Versions/A/Metal", MetalGlobals::class.java)
            val metalDevice = metal.MTLCreateSystemDefaultDevice()?.asObjcDynamicInterface<MTLDevice>()

            if (metalDevice != null) {
                println(metalDevice.name)
                println(metalDevice.architecture.name)
                println(metalDevice.maxTransferRate)
                println(metalDevice.maxBufferLength)
            }

            ObjcProtocolRef.getByName("MTLDevice")!!.dumpKotlin()

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
