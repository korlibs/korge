package korlibs.render.osx.metal

import com.sun.jna.*
import korlibs.memory.dyn.osx.*

interface CoreGraphics : Library {
}

interface MetalGlobals : Library {
    ///System/Library/Frameworks/Metal.framework/Versions/A/Metal
    //fun _MTLCreateSystemDefaultDevice(): Pointer?
    fun MTLCreateSystemDefaultDevice(): Pointer?
    //fun MTLClearColorMake(red: Double, green: Double, blue: Double, alpha: Double): MTLClearColor.ByValue
    //fun MTLRegionMake2D(x: Double, y: Double, width: Double, height: Double): MTLRegion.ByValue
    companion object : MetalGlobals by Native.load("/System/Library/Frameworks/Metal.framework/Versions/A/Metal", MetalGlobals::class.java) {
        init {
            val cg = Native.load("/System/Library/Frameworks/CoreGraphics.framework/Versions/A/CoreGraphics", CoreGraphics::class.java)
        }
    }
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
    companion object {
        fun make(red: Double, green: Double, blue: Double, alpha: Double): MTLClearColor.ByValue {
            val it = MTLClearColor.ByValue()
            it.red = red
            it.green = green
            it.blue = blue
            it.alpha = alpha
            it.write()
            return it
        }
    }

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
    @ObjcDesc("didModifyRange:", "v32@0:8{_NSRange=QQ}16") fun didModifyRange(didModifyRange: NSRange): Unit
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

    companion object {
        fun make(x: Double, y: Double, width: Double, height: Double): CGRect.ByValue {
            val it = CGRect.ByValue()
            it.x = 0.0
            it.y = 0.0
            it.width = width.toDouble()
            it.height = height.toDouble()
            it.write()
            return it
        }
    }
}

open class NSRange : Structure {
    override fun getFieldOrder() = listOf("location", "length")
    @JvmField var location: Long = 0L
    @JvmField var length: Long = 0L

    constructor() : super() {}
    constructor(peer: Pointer?) : super(peer) {}


    class ByReference : NSRange(), Structure.ByReference
    class ByValue : NSRange(), Structure.ByValue

    companion object {
        fun make(location: Long, length: Long): NSRange.ByValue {
            val it = NSRange.ByValue()
            it.location = location
            it.length = length
            it.write()
            return it
        }
    }
}

val MTLPixelFormatBGRA8Unorm = 80L

val MTLPrimitiveTypePoint = 0L
val MTLPrimitiveTypeLine = 1L
val MTLPrimitiveTypeLineStrip = 2L
val MTLPrimitiveTypeTriangle = 3L
val MTLPrimitiveTypeTriangleStrip = 4L

val MTLLoadActionDontCare = 0L
val MTLLoadActionLoad = 1L
val MTLLoadActionClear = 2L
