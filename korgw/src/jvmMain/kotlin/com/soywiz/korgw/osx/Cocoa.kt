package com.soywiz.korgw.osx
import com.sun.jna.*

//inline class ID(val id: Long)
typealias ID = Long

annotation class NativeName(val name: String) {
    companion object {
        val OPTIONS = mapOf(
            Library.OPTION_FUNCTION_MAPPER to FunctionMapper { _, method ->
                method.getAnnotation(NativeName::class.java)?.name ?: method.name
            }
        )
    }
}

typealias NSRectPtr = Pointer

inline fun <reified T : Library> NativeLoad(name: String) = Native.load(name, T::class.java, NativeName.OPTIONS) as T

internal interface GL : Library {
    fun glViewport(x: Int, y: Int, width: Int, height: Int)
    fun glClearColor(r: Float, g: Float, b: Float, a: Float)
    fun glClear(flags: Int)
    fun glFinish()
    fun glFlush()
    fun CGLSetParameter(vararg args: Any?)
    fun CGLEnable(vararg args: Any?)
    companion object : GL by NativeLoad("OpenGL") {
        const val GL_COLOR_BUFFER_BIT = 0x00004000
        val NATIVE = NativeLibrary.getInstance("OpenGL")
    }
}

interface ObjectiveC : Library {
    fun objc_getClass(name: String): Long
    fun objc_getProtocol(name: String): Long

    fun class_addProtocol(a: Long, b: Long): Long
    fun class_copyMethodList(clazz: Long, items: IntArray): Pointer

    fun objc_registerClassPair(cls: Long)
    fun objc_lookUpClass(name: String): Long

    fun objc_msgSend(vararg args: Any?): Long
    @NativeName("objc_msgSend")
    fun objc_msgSendInt(vararg args: Any?): Int
    @NativeName("objc_msgSend")
    fun objc_msgSendCGFloat(vararg args: Any?): CGFloat
    @NativeName("objc_msgSend")
    fun objc_msgSendNSPoint(vararg args: Any?): NSPointRes
    fun objc_msgSend_stret(structPtr: Any?, vararg args: Any?): Unit
    /*
    fun objc_msgSend(a: Long, b: Long): Long
    fun objc_msgSend(a: Long, b: Long, c: Long): Long
    fun objc_msgSend(a: Long, b: Long, c: String): Long
    fun objc_msgSend(a: Long, b: Long, c: ByteArray, d: Int, e: Int): Long
    fun objc_msgSend(a: Long, b: Long, c: ByteArray, len: Int): Long
    fun objc_msgSend(a: Long, b: Long, c: CharArray, len: Int): Long
     */
    fun method_getName(m: Long): Long

    fun sel_registerName(name: String): Long

    fun sel_getName(sel: Long): String
    fun objc_allocateClassPair(clazz: Long, name: String, extraBytes: Int): Long
    fun object_getIvar(obj: Long, ivar: Long): Long

    fun class_getInstanceVariable(clazz: ID, name: String): ID
    fun class_getProperty(clazz: ID, name: String): ID

    fun class_addMethod(cls: Long, name: Long, imp: Callback, types: String): Long
    fun class_conformsToProtocol(cls: Long, protocol: Long): Boolean

    fun object_getClass(obj: ID): ID
    fun class_getName(clazz: ID): String

    fun object_getClassName(obj: ID): String

    fun property_getName(prop: ID): String
    fun property_getAttributes(prop: ID): String

    companion object : ObjectiveC by NativeLoad("objc") {
        val NATIVE = NativeLibrary.getInstance("objc")
    }
}

@PublishedApi
internal fun __AllocateClass(name: String, base: String, vararg protocols: String): Long {
    val clazz = ObjectiveC.objc_allocateClassPair(ObjectiveC.objc_getClass(base), name, 0)
    for (protocol in protocols) {
        val protocolId = ObjectiveC.objc_getProtocol(protocol)
        if (protocolId != 0L) {
            ObjectiveC.class_addProtocol(clazz, protocolId)
        }
    }
    return clazz
}

inline fun AllocateClassAndRegister(name: String, base: String, vararg protocols: String, configure: AllocateClassMethodRegister.() -> Unit = {}): Long {
    val clazz = __AllocateClass(name, base, *protocols)
    try {
        configure(AllocateClassMethodRegister(clazz))
    } finally {
        ObjectiveC.objc_registerClassPair(clazz)
    }
    return clazz
}

inline class AllocateClassMethodRegister(val clazz: Long) {
    fun addMethod(sel: String, callback: Callback, types: String) {
        ObjectiveC.class_addMethod(clazz, sel(sel), callback, types)
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

//typealias NSPointRes = Long
typealias NSPointRes = MyNativeNSPoint.ByValue

fun sel(name: String) = ObjectiveC.sel_registerName(name)
fun Long.msgSend(sel: String, vararg args: Any?): Long = ObjectiveC.objc_msgSend(this, sel(sel), *args)
fun Long.msgSendInt(sel: String, vararg args: Any?): Int = ObjectiveC.objc_msgSendInt(this, sel(sel), *args)
fun Long.msgSendCGFloat(sel: String, vararg args: Any?): CGFloat = ObjectiveC.objc_msgSendCGFloat(this, sel(sel), *args)
fun Long.msgSendNSPoint(sel: String, vararg args: Any?): NSPointRes = ObjectiveC.objc_msgSendNSPoint(this, sel(sel), *args)
fun Long.msgSend_stret(output: Any?, sel: String, vararg args: Any?): Unit = ObjectiveC.objc_msgSend_stret(output, this, sel(sel), *args)
operator fun Long.invoke(sel: String, vararg args: Any?): Long = ObjectiveC.objc_msgSend(this, sel(sel), *args)

open class NSObject(val id: Long) : IntegerType(8, id, false), NativeMapped {
    fun msgSend(sel: String, vararg args: Any?): Long = ObjectiveC.objc_msgSend(id, sel(sel), *args)
    fun alloc(): Long = msgSend("alloc")

    companion object : NSClass("NSObject") {
    }

    override fun toByte(): Byte = id.toByte()
    override fun toChar(): Char = id.toChar()
    override fun toShort(): Short = id.toShort()
    override fun toInt(): Int = id.toInt()
    override fun toLong(): Long = id

    override fun toNative(): Any = this.id

    override fun fromNative(nativeValue: Any, context: FromNativeContext?): Any = NSObject((nativeValue as Number).toLong())
    override fun nativeType(): Class<*> = Long::class.javaPrimitiveType!!
}

open class NSString(id: Long) : NSObject(id) {
    constructor() : this("")
    constructor(str: String) : this(OBJ_CLASS.msgSend("alloc").msgSend("initWithCharacters:length:", str.toCharArray(), str.length))

    //val length: Int get() = ObjectiveC.object_getIvar(this.id, LENGTH_ivar).toInt()
    val length: Int get() = this.msgSend("length").toInt()

    val cString: String
        get() {
            val length = this.length
            val ba = ByteArray(length + 1)
            msgSend("getCString:maxLength:encoding:", ba, length + 1, 4)
            val str = ba.toString(Charsets.UTF_8)
            return str.substring(0, str.length - 1)
        }

    override fun toString(): String = cString

    companion object : NSClass("NSString") {
        val LENGTH_ivar = ObjectiveC.class_getProperty(OBJ_CLASS, "length")
    }
}

open class NSClass(val name: String) : NSObject(ObjectiveC.objc_getClass(name)) {
    val OBJ_CLASS = id
}

fun NSClass.listClassMethods(): List<String> = ObjC_listMethods(ObjectiveC.object_getClass(this.id))
fun NSClass.listInstanceMethods(): List<String> = ObjC_listMethods(this.id)

fun ObjC_listMethods(clazz: Long): List<String> {
    val nitemsPtr = IntArray(1)
    val items2 = ObjectiveC.class_copyMethodList(clazz, nitemsPtr)
    val nitems = nitemsPtr[0]
    val out = ArrayList<String>(nitems)
    for (n in 0 until nitems) {
        val ptr = items2.getNativeLong((Native.LONG_SIZE * n).toLong())
        val mname = ObjectiveC.method_getName(ptr.toLong())
        val selName = ObjectiveC.sel_getName(mname)
        out.add(selName)
    }
    return out
}

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

interface ObjcCallback : Callback {
    operator fun invoke(self: Long, _sel: Long, sender: Long): Long
}

interface ObjcCallbackVoid : Callback {
    operator fun invoke(self: Long, _sel: Long, sender: Long): Unit
}

fun ObjcCallback(callback: (self: Long, _sel: Long, sender: Long) -> Long): ObjcCallback {
    return object : ObjcCallback {
        override fun invoke(self: Long, _sel: Long, sender: Long): Long = callback(self, _sel, sender)
    }
}

fun ObjcCallbackVoid(callback: (self: Long, _sel: Long, sender: Long) -> Unit): ObjcCallbackVoid {
    return object : ObjcCallbackVoid {
        override fun invoke(self: Long, _sel: Long, sender: Long): Unit = callback(self, _sel, sender)
    }
}

fun ObjcCallbackVoidEmpty(callback: () -> Unit): ObjcCallbackVoid {
    return object : ObjcCallbackVoid {
        override fun invoke(self: Long, _sel: Long, sender: Long): Unit = callback()
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

    var a: Int get() = pointer.getInt(0L); set(value) = run { pointer.setInt(0L, value) }
    var b: Int get() = pointer.getInt(4L); set(value) = run { pointer.setInt(4L, value) }
    var c: Int get() = pointer.getInt(8L); set(value) = run { pointer.setInt(8L, value) }
    var d: Int get() = pointer.getInt(12L); set(value) = run { pointer.setInt(12L, value) }
    var e: Int get() = pointer.getInt(16L); set(value) = run { pointer.setInt(16L, value) }
    var f: Int get() = pointer.getInt(20L); set(value) = run { pointer.setInt(20L, value) }
    var g: Int get() = pointer.getInt(24L); set(value) = run { pointer.setInt(24L, value) }
    var h: Int get() = pointer.getInt(28L); set(value) = run { pointer.setInt(28L, value) }

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

inline class NSMenu(val id: Long) {
    constructor() : this(NSClass("NSMenu").alloc().msgSend("init"))

    companion object {
        operator fun invoke(callback: NSMenu.() -> Unit) = NSMenu().apply(callback)
    }

    fun addItem(menuItem: NSMenuItem) {
        id.msgSend("addItem:", menuItem.id)
    }
}

inline fun autoreleasePool(body: () -> Unit) {
    val autoreleasePool = NSClass("NSAutoreleasePool").alloc().msgSend("init")
    try {
        body()
    } finally {
        autoreleasePool.msgSend("drain")
    }
}

interface DisplayLinkCallback : Callback {
    fun callback(displayLink: Pointer?, inNow: Pointer?, inOutputTime: Pointer?, flagsIn: Pointer?, flagsOut: Pointer?, userInfo: Pointer?): Int
}

interface CoreGraphics : Library {
    fun CGMainDisplayID(): Int
    fun CVDisplayLinkCreateWithCGDisplay(displayId: Int, ptr: Pointer?): Int
    fun CVDisplayLinkSetOutputCallback(displayLinkValue: Pointer?, callback: Callback?, userInfo: Pointer?): Int
    fun CVDisplayLinkStart(displayLinkValue: Pointer?): Int
    fun CVDisplayLinkStop(displayLinkValue: Pointer?): Int
    companion object : CoreGraphics by NativeLoad("/System/Library/Frameworks/CoreGraphics.framework/Versions/A/CoreGraphics")
}

fun JnaMemory(array: IntArray): Memory {
    val mem = Memory((array.size * 4).toLong())
    for (n in 0 until array.size) {
        mem.setInt((n * 4).toLong(), array[n])
    }
    return mem
}
