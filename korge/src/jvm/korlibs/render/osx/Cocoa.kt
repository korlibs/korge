package korlibs.render.osx

import com.sun.jna.*
import korlibs.memory.dyn.osx.*
import korlibs.memory.dyn.osx.NativeName
import korlibs.render.platform.*
import korlibs.render.platform.NativeLoad

class NSApplication(id: Long) : NSObject(id) {
    fun setActivationPolicy(value: Int) = id.msgSend("setActivationPolicy:", value.toLong())

    companion object : NSClass("NSApplication") {
        fun sharedApplication(): NSApplication = NSApplication(OBJ_CLASS.msgSend("sharedApplication"))
    }
}

class NSWindow(id: Long) : NSObject(id) {
    companion object : NSClass("NSWindow") {
        operator fun invoke() {
            val res = OBJ_CLASS.msgSend("alloc").msgSend("init(contentRect:styleMask:backing:defer:)")
        }

        fun sharedApplication(): NSApplication = NSApplication(OBJ_CLASS.msgSend("sharedApplication"))
    }
}

interface ApplicationShouldTerminateCallback : Callback {
    operator fun invoke(self: Long, _sel: Long, sender: Long): Long
}

var running = true

val applicationShouldTerminateCallback = object : ApplicationShouldTerminateCallback {
    override fun invoke(self: Long, _sel: Long, sender: Long): Long {
        println("applicationShouldTerminateCallback")
        running = false
        System.exit(0)
        return 0L
    }
}

interface WindowWillCloseCallback : Callback {
    operator fun invoke(self: Long, _sel: Long, sender: Long): Long
}

val windowWillClose = object : WindowWillCloseCallback {
    override fun invoke(self: Long, _sel: Long, sender: Long): Long {
        running = false
        System.exit(0)
        return 0L
    }
}

fun Long.alloc(): Long = this.msgSend("alloc")
fun Long.autorelease(): Long = this.apply { this.msgSend("autorelease") }
fun <T : NSObject> T.autorelease(): T = this.apply { this.msgSend("autorelease") }

@Structure.FieldOrder("value")
class CGFloat(val value: Double) : Number(), NativeMapped {
    constructor() : this(0.0)
    constructor(value: Float) : this(value.toDouble())
    constructor(value: Number) : this(value.toDouble())

    companion object {
        @JvmStatic
        val SIZE = Native.LONG_SIZE
    }

    override fun toByte(): Byte = value.toInt().toByte()
    override fun toChar(): Char = value.toChar()
    override fun toDouble(): Double = value.toDouble()
    override fun toFloat(): Float = value.toFloat()
    override fun toInt(): Int = value.toInt()
    override fun toLong(): Long = value.toLong()
    override fun toShort(): Short = value.toInt().toShort()
    override fun nativeType(): Class<*> = when (SIZE) {
        4 -> Float::class.java
        8 -> Double::class.java
        else -> TODO()
    }

    override fun toNative(): Any = when (SIZE) {
        4 -> this.toFloat()
        8 -> this.toDouble()
        else -> TODO()
    }

    override fun fromNative(nativeValue: Any, context: FromNativeContext?): Any = CGFloat((nativeValue as Number).toDouble())

    override fun toString(): String = "$value"
}

@Structure.FieldOrder("x", "y")
public class NSPoint(@JvmField var x: CGFloat, @JvmField var y: CGFloat) : Structure(), Structure.ByValue {
    constructor(x: Double, y: Double) : this(CGFloat(x), CGFloat(y))
    constructor() : this(0.0, 0.0)

    override fun toString(): String = "($x, $y)"

    companion object {
        inline operator fun invoke(x: Number, y: Number) = NSPoint(x.toDouble(), y.toDouble())
    }
}

@Structure.FieldOrder("width", "height")
public class NSSize(@JvmField var width: CGFloat, @JvmField var height: CGFloat) : Structure(), Structure.ByValue {
    constructor(width: Double, height: Double) : this(CGFloat(width), CGFloat(height))
    constructor(width: Number, height: Number) : this(CGFloat(width), CGFloat(height))
    constructor() : this(0.0, 0.0)

    override fun toString(): String = "($width, $height)"

    companion object {
        inline operator fun invoke(width: Number, height: Number) = NSSize(width.toDouble(), height.toDouble())
    }
}

@Structure.FieldOrder("origin", "size")
public class NSRect(
    @JvmField var origin: NSPoint,
    @JvmField var size: NSSize
) : Structure(), Structure.ByValue {
    constructor() : this(NSPoint(), NSSize()) {
        allocateMemory()
        autoWrite()
    }
    constructor(x: Number, y: Number, width: Number, height: Number) : this(NSPoint(x, y), NSSize(width, height))

    override fun toString(): String = "NSRect($origin, $size)"
}

public class NativeNSRect {
    private var pointer: Pointer

    constructor() {
        val memory = Native.malloc(32);
        pointer = Pointer(memory);
    }

    fun free() {
        Native.free(Pointer.nativeValue(pointer));
    }

    fun getPointer(): Pointer {
        return pointer;
    }

    var a: Int get() = pointer.getInt(0L); set(value) { pointer.setInt(0L, value) }
    var b: Int get() = pointer.getInt(4L); set(value) { pointer.setInt(4L, value) }
    var c: Int get() = pointer.getInt(8L); set(value) { pointer.setInt(8L, value) }
    var d: Int get() = pointer.getInt(12L); set(value) { pointer.setInt(12L, value) }
    var e: Int get() = pointer.getInt(16L); set(value) { pointer.setInt(16L, value) }
    var f: Int get() = pointer.getInt(20L); set(value) { pointer.setInt(20L, value) }
    var g: Int get() = pointer.getInt(24L); set(value) { pointer.setInt(24L, value) }
    var h: Int get() = pointer.getInt(28L); set(value) { pointer.setInt(28L, value) }

    override fun toString(): String = "NativeNSRect($a, $b, $c, $d, $e, $f, $g, $h)"
}

@Structure.FieldOrder("x", "y", "width", "height")
open class MyNativeNSRect : Structure {
    @JvmField var x: Double = 0.0
    @JvmField var y: Double = 0.0
    @JvmField var width: Double = 0.0
    @JvmField var height: Double = 0.0

    constructor() {
        allocateMemory()
        autoWrite()
    }
    constructor(x: Number, y: Number, width: Number, height: Number) : this() {
        this.x = x.toDouble()
        this.y = y.toDouble()
        this.width = width.toDouble()
        this.height = height.toDouble()
    }

    class ByReference() : MyNativeNSRect(), Structure.ByReference {
        constructor(x: Number, y: Number, width: Number, height: Number) : this() {
            this.x = x.toDouble()
            this.y = y.toDouble()
            this.width = width.toDouble()
            this.height = height.toDouble()
        }
    }
    class ByValue() : MyNativeNSRect(), Structure.ByValue {
        constructor(x: Number, y: Number, width: Number, height: Number) : this() {
            this.x = x.toDouble()
            this.y = y.toDouble()
            this.width = width.toDouble()
            this.height = height.toDouble()
        }
    }

    override fun toString(): String = "NSRect($x, $y, $width, $height)"
}

@Structure.FieldOrder("x", "y")
open class MyNativeNSPoint() : Structure() {
    @JvmField var x: Double = 0.0
    @JvmField var y: Double = 0.0

    constructor(x: Number, y: Number) : this() {
        this.x = x.toDouble()
        this.y = y.toDouble()
    }

    init {
        allocateMemory()
        autoWrite()
    }

    class ByReference() : MyNativeNSPoint(), Structure.ByReference {
        constructor(x: Number, y: Number) : this() {
            this.x = x.toDouble()
            this.y = y.toDouble()
        }
    }
    class ByValue() : MyNativeNSPoint(), Structure.ByValue {
        constructor(x: Number, y: Number) : this() {
            this.x = x.toDouble()
            this.y = y.toDouble()
        }
    }

    override fun toString(): String = "NSPoint($x, $y)"
}

@Structure.FieldOrder("x", "y")
open class MyNativeNSPointLong() : Structure() {
    @JvmField var x: Long = 0L
    @JvmField var y: Long = 0L

    init {
        allocateMemory()
        autoWrite()
    }

    class ByReference : MyNativeNSPoint(), Structure.ByReference
    class ByValue : MyNativeNSPoint(), Structure.ByValue

    override fun toString(): String = "NSPoint($x, $y)"
}

inline class NSMenuItem(val id: Long) {
    constructor() : this(NSClass("NSMenuItem").alloc().msgSend("init"))
    constructor(text: String, sel: String, keyEquivalent: String) : this(
        NSClass("NSMenuItem").alloc().msgSend(
            "initWithTitle:action:keyEquivalent:",
            NSString(text).id,
            sel(sel),
            NSString(keyEquivalent).id
        ).autorelease()
    )

    companion object {
        operator fun invoke(callback: NSMenuItem.() -> Unit) = NSMenuItem().apply(callback)
    }

    fun setSubmenu(menu: NSMenu) {
        id.msgSend("setSubmenu:", menu.id)
    }
}

//interface NSObject {
//    val id: Long
//}

inline class NSMenu(val id: Long) {
    constructor() : this(NSClass("NSMenu").alloc().msgSend("init"))

    companion object {
        operator fun invoke(callback: NSMenu.() -> Unit) = NSMenu().apply(callback)
    }

    fun addItem(menuItem: NSMenuItem) {
        id.msgSend("addItem:", menuItem.id)
    }
}

@Deprecated("", ReplaceWith("korlibs.memory.dyn.osx.autoreleasePool(body)", "korlibs"))
inline fun autoreleasePool(body: () -> Unit) = korlibs.memory.dyn.osx.autoreleasePool(body)

internal interface GL : Library {
    fun glViewport(x: Int, y: Int, width: Int, height: Int)
    fun glClearColor(r: Float, g: Float, b: Float, a: Float)
    fun glClear(flags: Int)
    fun glFinish()
    fun glFlush()
    fun CGLSetParameter(vararg args: Any?)
    fun CGLEnable(vararg args: Any?)
    companion object : GL by NativeLoad(nativeOpenGLLibraryPath) {
        const val GL_COLOR_BUFFER_BIT = 0x00004000
    }
}


interface Foundation : Library {
    fun NSLog(msg: Long): Unit
    fun NSMakeRect(x: CGFloat, y: CGFloat, w: CGFloat, h: CGFloat): NSRect

    //companion object : Foundation by Native.load("/System/Library/Frameworks/Foundation.framework/Versions/C/Foundation", Foundation::class.java) as Foundation
    companion object : Foundation by Native.load("Foundation", Foundation::class.java, NativeName.OPTIONS) as Foundation {
        val NATIVE = NativeLibrary.getInstance("Foundation")
    }
}

interface Cocoa : Library {
    companion object : Cocoa by Native.load("Cocoa", Cocoa::class.java, NativeName.OPTIONS) as Cocoa {
        val NATIVE = NativeLibrary.getInstance("Cocoa")
    }
}

interface AppKit : Library {
    companion object : AppKit by Native.load("AppKit", AppKit::class.java, NativeName.OPTIONS) as AppKit {
        val NATIVE = NativeLibrary.getInstance("AppKit")
        val NSApp = NATIVE.getGlobalVariableAddress("NSApp").getLong(0L)
    }
}

fun Foundation.NSLog(msg: NSString) = NSLog(msg.id)
fun Foundation.NSLog(msg: String) = NSLog(NSString(msg))

fun interface DisplayLinkCallback : Callback {
    fun callback(displayLink: Pointer?, inNow: Pointer?, inOutputTime: Pointer?, flagsIn: Pointer?, flagsOut: Pointer?, userInfo: Pointer?): Int
}

interface CoreGraphics : Library {
    fun CGMainDisplayID(): Int
    companion object : CoreGraphics by NativeLoad("/System/Library/Frameworks/CoreGraphics.framework/Versions/A/CoreGraphics")
}

interface CoreVideo : Library {
    fun CVDisplayLinkCreateWithCGDisplay(displayId: Int, ptr: Pointer?): Int
    fun CVDisplayLinkSetOutputCallback(displayLinkValue: Pointer?, callback: Callback?, userInfo: Pointer?): Int
    fun CVDisplayLinkStart(displayLinkValue: Pointer?): Int
    fun CVDisplayLinkStop(displayLinkValue: Pointer?): Int

    // https://developer.apple.com/documentation/metal/metal_sample_code_library/mixing_metal_and_opengl_rendering_in_a_view
    fun CVPixelBufferCreate(
        allocator: Pointer?,
        width: Int,
        height: Int,
        pixelFormatType: Int,
        pixelBufferAttributes: Pointer?, // Dictionary
        pixelBufferOut: Pointer?
    ): Int

    // OpenGL

    fun CVOpenGLTextureCacheCreate(
        allocator: Pointer?,
        cacheAttributes: Pointer?, // Dictionary
        cglContext: Pointer?,
        cglPixelFormat: Pointer?,
        textureAttributes: Pointer?,
        cacheOut: Pointer?
    ): Int

    fun CVOpenGLTextureCacheCreateTextureFromImage(
        allocator: Pointer?,
        textureCache: Pointer?,
        sourceImage: Pointer?,
        attributes: Pointer?,
        textureOut: Pointer?
    ): Int

    fun CVOpenGLTextureGetName(image: Pointer?): Int

    // Metal

    fun CVMetalTextureCacheCreate(
        allocator: Pointer?,
        cacheAttributes: Pointer?,
        metalDevice: Pointer?,
        textureAttributes: Pointer?,
        cacheOut: Pointer?
    ): Int

    fun CVMetalTextureCacheCreateTextureFromImage(
        allocator: Pointer?,
        textureCache: Pointer?,
        sourceImage: Pointer?,
        textureAttributes: Pointer?,
        pixelFormat: Int,
        width: Int,
        height: Int,
        planeIndex: Int,
        textureOut: Pointer?
    ): Int
    fun CVMetalTextureGetTexture(image: Pointer?): Pointer?

    companion object : CoreVideo by NativeLoad("/System/Library/Frameworks/CoreVideo.framework/Versions/A/CoreVideo",)
}

fun JnaMemory(array: IntArray): Memory {
    val mem = Memory((array.size * 4).toLong())
    for (n in 0 until array.size) {
        mem.setInt((n * 4).toLong(), array[n])
    }
    return mem
}
