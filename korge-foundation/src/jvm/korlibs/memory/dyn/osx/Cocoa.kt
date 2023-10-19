package korlibs.memory.dyn.osx

import com.sun.jna.*
import korlibs.annotations.*
import korlibs.memory.dyn.*
import java.lang.reflect.*
import java.util.*
import java.util.concurrent.*

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

inline fun <reified T : Library> NativeLoad(name: String): T = Native.load(name, T::class.java, NativeName.OPTIONS) as T

// https://developer.apple.com/documentation/objectivec/objective-c_runtime
interface ObjectiveC : Library {
    fun objc_copyProtocolList(outCount: IntArray): Pointer
    fun protocol_getName(protocol: Long): String

    fun objc_getClass(name: String): Long

    fun objc_getClassList(buffer: Pointer?, bufferCount: Int): Int

    fun objc_getProtocol(name: String): Long

    fun class_addProtocol(a: Long, b: Long): Long
    fun class_copyMethodList(clazz: Long, items: IntArray): Pointer

    // typedef struct objc_method_description {
    //     SEL name;        // The name of the method
    //     char *types;     // The types of the method arguments
    // } MethodDescription;
    fun protocol_copyMethodDescriptionList(proto: Long, isRequiredMethod: Boolean, isInstanceMethod: Boolean, outCount: IntArray): Pointer

    fun objc_registerClassPair(cls: Long)
    fun objc_lookUpClass(name: String): Long

    fun objc_msgSend(vararg args: Any?): Long
    @NativeName("objc_msgSend")
    fun objc_msgSendInt(vararg args: Any?): Int
    @NativeName("objc_msgSend")
    fun objc_msgSendVoid(vararg args: Any?): Unit
    @NativeName("objc_msgSend")
    fun objc_msgSendCGFloat(vararg args: Any?): CGFloat
    @NativeName("objc_msgSend")
    fun objc_msgSendFloat(vararg args: Any?): Float
    @NativeName("objc_msgSend")
    fun objc_msgSendDouble(vararg args: Any?): Double
    @NativeName("objc_msgSend")
    fun objc_msgSendNSPoint(vararg args: Any?): NSPointRes
    @NativeName("objc_msgSend")
    fun objc_msgSendNSRect(vararg args: Any?): NSRectRes
    @NativeName("objc_msgSend_stret")
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
    @NativeName("method_getName")
    fun method_getNameString(m: Long): String

    fun sel_registerName(name: String): Long
    fun sel_getName(sel: Long): String
    @NativeName("sel_getName")
    fun sel_getNameString(sel: String): String

    fun objc_allocateClassPair(clazz: Long, name: String, extraBytes: Int): Long
    fun object_getIvar(obj: Long, ivar: Long): Long

    fun class_getInstanceVariable(clazz: ID, name: String): ID
    fun class_getProperty(clazz: ID, name: String): ID

    fun class_addMethod(cls: Long, name: Long, imp: Callback, types: String): Long
    fun class_conformsToProtocol(cls: Long, protocol: Long): Boolean

    fun object_getClass(obj: ID): ID
    fun class_getName(clazz: ID): String

    fun object_getClassName(obj: ID): String
    fun class_getImageName(obj: ID): String

    fun property_getName(prop: ID): String
    fun property_getAttributes(prop: ID): String

    fun class_getInstanceMethod(cls: ID, id: NativeLong): NativeLong

    fun method_getReturnType(id: NativeLong, dst: Pointer, dst_length: NativeLong)
    fun method_getTypeEncoding(ptr: Pointer): String

    fun class_createInstance(cls: ID, extraBytes: NativeLong): ID
    fun class_copyPropertyList(cls: ID, outCountPtr: IntArray): Pointer
    fun class_copyIvarList(cls: ID, outCountPtr: IntArray): Pointer
    fun ivar_getName(ivar: Pointer?): String?
    fun ivar_getTypeEncoding(ivar: Pointer?): String?

    companion object : ObjectiveC by NativeLoad("objc") {
        //val NATIVE = NativeLibrary.getInstance("objc")
    }
}

data class ObjcMethodRef(val objcClass: ObjcProtocolClassBaseRef, val ptr: Pointer) {
    val name: String by lazy { ObjectiveC.method_getNameString(ptr.address) }
    val selName: String by lazy { ObjectiveC.sel_getNameString(name) }
    val types: String by lazy { ObjectiveC.method_getTypeEncoding(ptr) }
    val parsedTypes: ObjcMethodDesc by lazy { ObjcTypeParser.parse(types) }

    override fun toString(): String = "${objcClass.name}.$name"
}

interface ObjcProtocolClassBaseRef {
    val name: String
}

annotation class ObjcDesc(val name: String, val types: String = "")

data class ObjcMethodDescription(
    val protocol: ObjcProtocolRef,
    val id: NativeLong,
    val types: String
) {
    val method by lazy { ObjectiveC.class_getInstanceMethod(protocol.ref, id) }
    val name: String by lazy { ObjectiveC.sel_getName(id.toLong()) }
    val parsedTypes: ObjcMethodDesc by lazy { ObjcTypeParser.parse(types) }

    override fun toString(): String = "ObjcMethodDescription[$name$parsedTypes]"
    fun dumpKotlin() {
        println("  " + createKotlinMethod(name, parsedTypes))
    }
}

data class ObjcMethodDesc(val desc: String, val returnType: ObjcParam, val params: List<ObjcParam>) {
    constructor(desc: String, all: List<ObjcParam>) : this(desc, all.first(), all.drop(1))
}
data class ObjcParam(val offset: Int, val type: ObjcType)

interface ObjcType {
    fun toKotlinString(): String
}
data class ConstObjcType(val base: ObjcType) : ObjcType {
    override fun toKotlinString(): String = base.toKotlinString()
}
data class PointerObjcType(val base: ObjcType) : ObjcType {
    override fun toKotlinString(): String = "Pointer"
}
data class StructObjcType(val strName: String, val types: List<ObjcType>) : ObjcType {
    override fun toKotlinString(): String = "Struct"
}
data class FixedArrayObjcType(val count: Int, val type: ObjcType) : ObjcType {
    override fun toKotlinString(): String = "FixedArray[$count]"
}

enum class PrimitiveObjcType : ObjcType {
    VOID, BOOL, ID, SEL, BYTE, INT, UINT, NINT, NUINT, FLOAT, DOUBLE, BLOCK;

    override fun toKotlinString(): String = when (this) {
        VOID -> "Unit"
        BOOL -> "Boolean"
        ID -> "ID"
        SEL -> "SEL"
        BYTE -> "Byte"
        INT -> "Int"
        UINT -> "UInt"
        NINT -> "NativeLong"
        NUINT -> "NativeLong"
        FLOAT -> "Float"
        DOUBLE -> "Double"
        BLOCK -> "(() -> Unit)"
    }
}

data class StrReader(val str: String, var pos: Int = 0, var end: Int = str.length) {
    val hasMore: Boolean get() = pos < end

    fun peekChar(): Char = str.getOrElse(pos) { '\u0000' }
    fun readChar(): Char = peekChar().also { skip() }
    fun skip(count: Int = 1) {
        pos += count
    }
    fun readUntil(cond: (Char) -> Boolean): String {
        var out = ""
        while (true) {
            val c = peekChar()
            if (!cond(c)) break
            skip()
            out += c
        }
        return out
    }
}

object ObjcTypeParser {
    fun parseInt(str: StrReader): Int {
        var out = ""
        while (str.peekChar().isDigit()) {
            out += str.readChar()
        }
        return out.toIntOrNull() ?: -1
    }

    fun parseType(str: StrReader): ObjcType {
        val c = str.readChar()
        return when (c) {
            'V' -> PrimitiveObjcType.VOID // Class initializer
            'v' -> PrimitiveObjcType.VOID
            'B' -> PrimitiveObjcType.BOOL
            '@' -> {
                if (str.peekChar() == '?') {
                    str.skip()
                    PrimitiveObjcType.BLOCK
                } else {
                    PrimitiveObjcType.ID
                }
            }
            '{' -> {
                val name = str.readUntil { it != '=' }
                if (str.readChar() != '=') error("Invalid $str")
                val out = arrayListOf<ObjcType>()
                while (str.peekChar() != '}') {
                    out += parseType(str)
                }
                str.skip()
                StructObjcType(name, out)
            }
            '[' -> {
                val count = parseInt(str)
                val type = parseType(str)
                if (str.readChar() != ']') error("Invalid $str")
                FixedArrayObjcType(count, type)
            }
            ':' -> PrimitiveObjcType.SEL
            'C' -> PrimitiveObjcType.BYTE
            'i' -> PrimitiveObjcType.INT
            'I' -> PrimitiveObjcType.UINT
            'q' -> PrimitiveObjcType.NINT
            'Q' -> PrimitiveObjcType.NUINT
            '^' -> PointerObjcType(parseType(str))
            'r' -> ConstObjcType(parseType(str))
            'f' -> PrimitiveObjcType.FLOAT
            'd' -> PrimitiveObjcType.DOUBLE
            else -> TODO("Not implemented '$c' in $str")
        }
    }
    fun parseParam(str: StrReader): ObjcParam {
        val type = parseType(str)
        val offset = parseInt(str)
        return ObjcParam(offset, type)
    }
    fun parse(str: StrReader): ObjcMethodDesc {
        val out = arrayListOf<ObjcParam>()
        while (str.hasMore) {
            out += parseParam(str)
        }
        return ObjcMethodDesc(str.str, out)
    }
    fun parse(str: String): ObjcMethodDesc = parse(StrReader(str))
}

// @TODO: Optimize this to be as fast as possible
@Suppress("NewApi")
interface ObjcDynamicInterface {
    val __id: Long get() = TODO()

    @ObjcDesc("dealloc", "v16@0:8") fun dealloc(): Unit

    companion object {
        inline fun <reified T : ObjcDynamicInterface> createNew(init: String = "init", vararg args: Any?): T = createNew(T::class.java, init, *args)
        fun <T : ObjcDynamicInterface> createNew(clazz: Class<T>, init: String = "init", vararg args: Any?): T {
            val name = clazz.getDeclaredAnnotation(ObjcDesc::class.java)?.name ?: clazz.simpleName
            return proxy(NSClass(name).alloc().msgSend(init, *args), clazz)
            //return proxy(NSClass(name).alloc(), clazz)
            //return proxy(ObjcClassRef.fromName(name)!!.createInstance(), clazz)
        }

        fun <T : ObjcDynamicInterface> proxy(instance: Pointer?, clazz: Class<T>): T {
            return proxy(instance?.address ?: 0L, clazz)
        }
        fun <T : ObjcDynamicInterface> proxy(instance: Long, clazz: Class<T>): T {
            return Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz)
            ) { proxy, method, args ->
                if (method.name == "get__id") {
                    return@newProxyInstance instance
                }
                if (method.name == "toString") {
                    val classInstance = ObjectiveC.object_getClass(instance)
                    val className = ObjectiveC.class_getName(classInstance)
                    //val className = NSString(ObjectiveC.object_getClass(instance).msgSend("name")).cString
                    //val className = "$classInstance"
                    return@newProxyInstance "ObjcDynamicInterface[$className]($instance)"
                }
                val nargs = (args ?: emptyArray()).map {
                    when (it) {
                        null -> 0L
                        is ObjcDynamicInterface -> it.__id
                        else -> it
                    }
                }.toTypedArray()
                val name = method.getDeclaredAnnotation(ObjcDesc::class.java)?.name ?: method.name
                val returnType = method.returnType
                //println(":: $clazz[$instance].$name : ${nargs.toList()}")
                //if (returnType == Void::class.javaPrimitiveType) {
                //    val res = instance.msgSendVoid(name, *nargs)
                //    Unit
                //}
                val res = instance.msgSend(name, *nargs)
                if (ObjcDynamicInterface::class.java.isAssignableFrom(returnType)) {
                    ObjcDynamicInterface.proxy(res, returnType as Class<ObjcDynamicInterface>)
                } else {
                    when (returnType) {
                        String::class.java -> NSString(res).cString
                        Boolean::class.java -> res != 0L
                        Long::class.java -> res
                        NativeLong::class.java -> NativeLong(res)
                        Pointer::class.java -> Pointer(res)
                        Void::class.javaPrimitiveType -> Unit
                        Unit::class.java -> Unit
                        else -> TODO()
                    }
                }
            } as T
        }
    }
}

inline fun <reified T : ObjcDynamicInterface> Long.asObjcDynamicInterface(): T = ObjcDynamicInterface.proxy(this, T::class.java)
inline fun <reified T : ObjcDynamicInterface> Pointer.asObjcDynamicInterface(): T = ObjcDynamicInterface.proxy(this, T::class.java)

data class ObjcProtocolRef(val ref: ID) : ObjcProtocolClassBaseRef {
    override val name: String by lazy { ObjectiveC.protocol_getName(ref) }

    fun dumpKotlin() {
        println("interface $name : ObjcDynamicInterface {")
        for (method in listMethods()) {
            method.dumpKotlin()
        }
        println("}")
    }

    fun listMethods(): List<ObjcMethodDescription> {
        val nitemsPtr = IntArray(1)
        val items2 = ObjectiveC.protocol_copyMethodDescriptionList(ref, true, true, nitemsPtr)
        val nitems = nitemsPtr[0]
        val out = ArrayList<String>(nitems)
        //println("nitems=$nitems")
        return (0 until nitems).map { n ->
            val namePtr = items2.getNativeLong((Native.LONG_SIZE * n * 2 + 0).toLong())
            val typesPtr = items2.getNativeLong((Native.LONG_SIZE * n * 2 + Native.LONG_SIZE).toLong())
            val typesStr = typesPtr.toLong().toPointer().getString(0L)
            ObjcMethodDescription(this, namePtr, typesStr)
            //println("$selName: $typesStr")
            //val selName = ObjectiveC.sel_getName(mname)
        }
    }

    companion object {
        fun fromName(name: String): ObjcProtocolRef? =
            ObjectiveC.objc_getProtocol(name).takeIf { it != 0L }?.let { ObjcProtocolRef(it) }

        fun listAll(): List<ObjcProtocolRef> {
            val countPtr = IntArray(1)
            val ptr = ObjectiveC.objc_copyProtocolList(countPtr)
            val count = countPtr[0]
            return (0 until count).map {
                ObjcProtocolRef(ptr.getPointer((Native.LONG_SIZE * it).toLong()).address)
            }
        }
    }

    override fun toString(): String = "ObjcProtocol[$name]"
}

private fun createKotlinMethod(
    name: String, parsedTypes: ObjcMethodDesc,
    setName: String? = null, setParsedTypes: ObjcMethodDesc? = null,
): String {
    val types = parsedTypes.desc
    val parts = name.split(":")
    val fullName = parts[0]
    val firstArg = fullName.substringAfterLast("With").decapitalize(Locale.ENGLISH)
    val baseName = fullName.substringBeforeLast("With")
    val argNames = listOf(firstArg) + parts.drop(1)
    val paramsWithNames = argNames.zip(parsedTypes.params.drop(2))
    val paramsStr = paramsWithNames.joinToString(", ") { "${it.first}: ${it.second.type.toKotlinString()}" }
    val returnTypeStr = parsedTypes.returnType.type.toKotlinString()
    if (paramsWithNames.isEmpty() && parsedTypes.returnType.type != PrimitiveObjcType.VOID) {
        return "@get:ObjcDesc(\"$name\", \"$types\") val $baseName: $returnTypeStr"
    } else {
        return "@ObjcDesc(\"$name\", \"$types\") fun $baseName($paramsStr): $returnTypeStr"
    }
}

data class ObjcClassRef(val ref: ID) : ObjcProtocolClassBaseRef {
    companion object {
        fun listAll(): List<ObjcClassRef> = ObjectiveC.getAllClassIDs()
        fun fromName(name: String): ObjcClassRef? = ObjectiveC.getClassByName(name)
    }

    fun createInstance(extraBytes: Int = 0): ID {
        return ObjectiveC.class_createInstance(ref, NativeLong(extraBytes.toLong()))
    }

    fun dumpKotlin() {
        println("class $name : ObjcDynamicInterface {")
        listIVars()
        listProperties()
        for (method in listMethods()) {
            println("  " + createKotlinMethod(method.name, method.parsedTypes))
        }
        println("}")
    }

    override val name: String by lazy { ObjectiveC.object_getClassName(ref) }
    val imageName: String by lazy { ObjectiveC.class_getImageName(ref) }

    fun listMethods(): List<ObjcMethodRef> {
        val nitemsPtr = IntArray(1)
        val items2 = ObjectiveC.class_copyMethodList(ref, nitemsPtr)
        val nitems = nitemsPtr[0]
        return (0 until nitems).map {
            ObjcMethodRef(this, items2.getPointer((Native.LONG_SIZE * it).toLong()))
        }
    }

    fun listProperties() {
        val outCountPtr = IntArray(1)
        val properties = ObjectiveC.class_copyPropertyList(ref, outCountPtr)
        val outCount = outCountPtr[0]
        for (n in 0 until outCount) {
            val prop = properties.getPointer(n * 8L)
            val propName = ObjectiveC.property_getName(prop.address)
            val attributes = ObjectiveC.property_getAttributes(prop.address)
            println("* $propName : $attributes")
        }
        //TODO("listProperties. outCount=$outCount")
    }

    fun listIVars() {
        val outCountPtr = IntArray(1)
        val ivars = ObjectiveC.class_copyIvarList(ref, outCountPtr)
        val outCount = outCountPtr[0]
        for (n in 0 until outCount) {
            val ivar = ivars.getPointer(n * 8L)
            val ivarName = ObjectiveC.ivar_getName(ivar)
            val encoding = ObjectiveC.ivar_getTypeEncoding(ivar)
            println("* ivar=$ivarName : $encoding")
        }
        //TODO("listIVars. outCount=$outCount")
    }

    override fun toString(): String = "ObjcClass[$name]"
}

fun ObjectiveC.getClassByName(name: String): ObjcClassRef? {
    val id = ObjectiveC.objc_lookUpClass(name)
    return if (id != 0L) ObjcClassRef(id) else null
}

fun ObjectiveC.getAllClassIDs(): List<ObjcClassRef> {
    val total = objc_getClassList(null, 0)
    val data = Memory((total * 8).toLong()).also { it.clear() }
    //println(data.getLong(0L))
    val total2 = objc_getClassList(data, total)
    return (0 until total2).map { ObjcClassRef(data.getLong((it * 8).toLong())) }
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
typealias NSRectRes = MyNativeNSRect.ByValue

private val isArm64 = System.getProperty("os.arch") == "aarch64"

// @TODO: Move Long to ObjcRef to not pollute Long scope
open class ObjcRef(val id: Long) {
}

inline class ObjcSel(val id: Long) {
    companion object {
        private val selectors = ConcurrentHashMap<String, ObjcSel>()

        operator fun invoke(name: String): ObjcSel =
            selectors.getOrPut(name) { ObjcSel(ObjectiveC.sel_registerName(name)) }
    }
}

fun sel(name: String): Long {
    val value = ObjectiveC.sel_registerName(name)
    if (value == 0L) error("Invalid selector '$name'")
    return value
}
fun sel(name: ObjcSel): Long = name.id
fun Long.msgSend(sel: ObjcSel, vararg args: Any?): Long = ObjectiveC.objc_msgSend(this, sel(sel), *args)
fun Long.msgSend(sel: String, vararg args: Any?): Long = ObjectiveC.objc_msgSend(this, sel(sel), *args)
fun Long.msgSendInt(sel: ObjcSel, vararg args: Any?): Int = ObjectiveC.objc_msgSendInt(this, sel(sel), *args)
fun Long.msgSendInt(sel: String, vararg args: Any?): Int = ObjectiveC.objc_msgSendInt(this, sel(sel), *args)
fun Long.msgSendVoid(sel: String, vararg args: Any?): Unit = ObjectiveC.objc_msgSendVoid(this, sel(sel), *args)

fun Long.msgSendFloat(sel: ObjcSel, vararg args: Any?): Float = ObjectiveC.objc_msgSendFloat(this, sel(sel), *args)
fun Long.msgSendFloat(sel: String, vararg args: Any?): Float = ObjectiveC.objc_msgSendFloat(this, sel(sel), *args)
fun Long.msgSendDouble(sel: ObjcSel, vararg args: Any?): Double = ObjectiveC.objc_msgSendDouble(this, sel(sel), *args)
fun Long.msgSendDouble(sel: String, vararg args: Any?): Double = ObjectiveC.objc_msgSendDouble(this, sel(sel), *args)

fun Long.msgSendCGFloat(sel: ObjcSel, vararg args: Any?): CGFloat = ObjectiveC.objc_msgSendCGFloat(this, sel(sel), *args)
fun Long.msgSendCGFloat(sel: String, vararg args: Any?): CGFloat = ObjectiveC.objc_msgSendCGFloat(this, sel(sel), *args)

fun Long.msgSendNSPoint(sel: String, vararg args: Any?): NSPointRes = ObjectiveC.objc_msgSendNSPoint(this, sel(sel), *args)
fun Long.msgSendNSRect(sel: String, vararg args: Any?): NSRectRes {
    if (isArm64) {
        return ObjectiveC.objc_msgSendNSRect(this, sel(sel), *args)
    } else {
        val rect = Memory(32)
        val out = NSRectRes()
        this.msgSend_stret(rect, sel, *args)
        out.x = rect.getDouble(0L)
        out.y = rect.getDouble(8L)
        out.width = rect.getDouble(16L)
        out.height = rect.getDouble(24L)
        return out
    }
}

fun Long.msgSend_stret(output: Any?, sel: String, vararg args: Any?) {
    if (isArm64) error("Not available on arm64")
    ObjectiveC.objc_msgSend_stret(output, this, sel(sel), *args)
}
fun Long.toPointer(): Pointer = Pointer(this)

/*
open class NSRECT : Structure {
    var x: Double = 0.0
    var y: Double = 0.0
    var width: Double = 0.0
    var height: Double = 0.0

    constructor() : super() {}
    constructor(peer: Pointer?) : super(peer) {}

    override fun getFieldOrder() = listOf("x", "y", "width", "height")

    class ByReference : NSRECT(), Structure.ByReference
    class ByValue : NSRECT(), Structure.ByValue
}
 */

operator fun Long.invoke(sel: String, vararg args: Any?): Long = ObjectiveC.objc_msgSend(this, sel(sel), *args)

open class NSObject(val id: Long) : IntegerType(8, id, false), NativeMapped {
    val ptr get() = id.toPointer()

    fun msgSend(sel: String, vararg args: Any?): Long = ObjectiveC.objc_msgSend(id, sel(sel), *args)
    fun msgSendInt(sel: String, vararg args: Any?): Int = ObjectiveC.objc_msgSendInt(id, sel(sel), *args)
    fun msgSendFloat(sel: String, vararg args: Any?): Float = ObjectiveC.objc_msgSendFloat(id, sel(sel), *args)
    fun msgSendDouble(sel: String, vararg args: Any?): Double = ObjectiveC.objc_msgSendDouble(id, sel(sel), *args)
    fun msgSendCGFloat(sel: String, vararg args: Any?): CGFloat = ObjectiveC.objc_msgSendCGFloat(id, sel(sel), *args)
    fun msgSendNSPoint(sel: String, vararg args: Any?): NSPointRes = ObjectiveC.objc_msgSendNSPoint(id, sel(sel), *args)
    fun msgSend_stret(sel: String, vararg args: Any?): Unit = ObjectiveC.objc_msgSend_stret(id, sel(sel), *args)

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

    val objcClass: NSClass get() = NSClass(msgSend("class"))

    override fun toString(): String = "NSObject($id)"
}

open class NSString(id: Long) : NSObject(id) {
    constructor() : this("")
    constructor(id: Long?) : this(id ?: 0L)
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

open class NSClass(id: Long) : NSObject(id) {
    constructor(name: String) : this(ObjectiveC.objc_getClass(name))

    val OBJ_CLASS = id

    val name by lazy { ObjectiveC.class_getName(id) }

    override fun toString(): String = "NSClass[$id]($name)"
}

fun <T> nsAutoreleasePool(block: () -> T): T {
    val pool = NSClass("NSAutoreleasePool").alloc().msgSend("init")
    try {
        return block()
    } finally {
        pool.msgSend("release")
    }
}

open class ObjcProtocol(val name: String) : NSObject(ObjectiveC.objc_getProtocol(name)) {
    val OBJ_PROTOCOL = id
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

fun ObjcProtocol.listMethods(): List<String> = ObjC_listProtocolMethods(this.id)

fun ObjC_listProtocolMethods(protocol: Long): List<String> {
    val nitemsPtr = IntArray(1)
    val items2 = ObjectiveC.protocol_copyMethodDescriptionList(protocol, true, true, nitemsPtr)
    val nitems = nitemsPtr[0]
    val out = ArrayList<String>(nitems)
    //println("nitems=$nitems")
    for (n in 0 until nitems) {
        val namePtr = items2.getNativeLong((Native.LONG_SIZE * n * 2 + 0).toLong())
        val typesPtr = items2.getNativeLong((Native.LONG_SIZE * n * 2 + 1).toLong())
        val selName = ObjectiveC.sel_getName(namePtr.toLong())
        //val typesStr = Pointer(typesPtr.toLong()).getString(0L)
        //println("$selName: $typesStr")
        //val selName = ObjectiveC.sel_getName(mname)
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
    val autoreleasePool = if (Platform.isMac()) NSClass("NSAutoreleasePool").alloc().msgSend("init") else null
    try {
        body()
    } finally {
        autoreleasePool?.msgSend("drain")
    }
}

@Keep
object CoreFoundation {
    val library = NativeLibrary.getInstance("CoreFoundation")
    @JvmStatic val kCFRunLoopCommonModes: Pointer? = library.getGlobalVariableAddress("kCFRunLoopCommonModes").getPointer(0L)
    @JvmStatic val kCFBooleanTrue: Pointer? = library.getGlobalVariableAddress("kCFBooleanTrue").getPointer(0L)
    @JvmStatic val kCFBooleanFalse: Pointer? = library.getGlobalVariableAddress("kCFBooleanFalse").getPointer(0L)
    @JvmStatic external fun CFRunLoopGetCurrent(): Pointer?
    @JvmStatic external fun CFRunLoopGetMain(): Pointer?
    @JvmStatic external fun CFRunLoopRun(): Void

    init {
        Native.register("CoreFoundation")
    }
}

class CFBoolean private constructor(id: Long) : NSObject(id) {
    val value: Boolean get() = this.id == TRUE.id

    companion object {
        val TRUE = CFBoolean(CoreFoundation.kCFBooleanTrue.addressNotNull)
        val FALSE = CFBoolean(CoreFoundation.kCFBooleanFalse.addressNotNull)

        operator fun invoke(value: Boolean): CFBoolean = if (value) TRUE else FALSE
    }
}

class NSNumber private constructor(id: Long) : NSObject(id) {
    constructor(value: Int) : this(NSClass("NSNumber").alloc().msgSend("initWithInt:", value))
    constructor(value: Double) : this(NSClass("NSNumber").alloc().msgSend("initWithDouble:", value))
    constructor(value: Long, unit: Unit = Unit) : this(NSClass("NSNumber").alloc().msgSend("initWithLong:", value))
    val boolValue: Boolean get() = id.msgSendInt("boolValue") != 0
    val intValue: Int get() = id.msgSendInt("intValue")
    val longValue: Long get() = id.msgSend("longValue")
    val doubleValue: Double get() = id.msgSendDouble("doubleValue")
}

fun NSObject.Companion.cast(value: Any): NSObject {
    return when (value) {
        is NSObject -> value
        is String -> NSString(value)
        is Int -> NSNumber(value)
        is Long -> NSNumber(value)
        is Double -> NSNumber(value)
        is Boolean -> CFBoolean(value)
        else -> TODO("Unsupported value '$value' to be cast to NSObject")
    }
}

class NSMutableDictionary(id: Long) : NSObject(id) {
    constructor() : this(NSClass("NSMutableDictionary").alloc().msgSend("init"))
    val count: Int get() = id.msgSendInt("count")
    fun setValue(value: NSObject, forKey: NSObject) {
        id.msgSend("setValue:forKey:", value.id, forKey.id)
    }
    fun getValue(key: NSObject): NSObject {
        return NSObject(id.msgSend("valueForKey:", key.id))
    }
    operator fun set(key: NSObject, value: NSObject) {
        setValue(value, key)
    }
    operator fun set(key: Any, value: Any) {
        this[NSObject.cast(key)] = NSObject.cast(value)
    }

    operator fun get(key: Any): NSObject {
        return getValue(NSObject.cast(key))
    }
}

class NSDictionary(id: Long) : NSObject(id) {
    constructor() : this(NSClass("NSDictionary").alloc().msgSend("init"))
    val count: Int get() = id.msgSendInt("count")
}

/*
interface DisplayLinkCallback : Callback {
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
    companion object : CoreVideo by NativeLoad("/System/Library/Frameworks/CoreVideo.framework/Versions/A/CoreVideo",)
}
*/

fun JnaMemory(array: IntArray): Memory {
    val mem = Memory((array.size * 4).toLong())
    for (n in 0 until array.size) {
        mem.setInt((n * 4).toLong(), array[n])
    }
    return mem
}
