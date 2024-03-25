package korlibs.ffi.osx

import korlibs.annotations.*
import korlibs.datastructure.lock.*
import korlibs.ffi.*
import kotlin.properties.*
import kotlin.reflect.*

typealias ID = Long

@KeepNames
object FFIObjc : FFILib("objc") {
    val objc_copyProtocolList: (outCount: IntArray) -> FFIPointer? by func()
    val protocol_getName: (protocol: Long) -> String by func()

    val objc_getClass: (name: String) -> Long by func()

    val objc_getClassList: (buffer: FFIPointer?, bufferCount: Int) -> Int by func()

    val objc_getProtocol: (name: String) -> Long by func()

    val class_addProtocol: (a: Long, b: Long) -> Long by func()
    val class_copyMethodList: (clazz: Long, items: IntArray) -> FFIPointer? by func()

    // typedef struct objc_method_description {
    //     SEL name;        // The name of the method
    //     char *types;     // The types of the method arguments
    // } MethodDescription;
    val protocol_copyMethodDescriptionList: (proto: Long, isRequiredMethod: Boolean, isInstanceMethod: Boolean, outCount: IntArray) -> FFIPointer? by func()

    val objc_registerClassPair: (cls: Long) -> Unit by func()
    val objc_lookUpClass: (name: String) -> Long by func()

    val objc_msgSend: (args: FFIVarargs) -> Long by func("objc_msgSend")
    val objc_msgSendInt: (args: FFIVarargs) -> Int by func("objc_msgSend")
    val objc_msgSendVoid: (args: FFIVarargs) -> Unit by func("objc_msgSend")
    //val objc_msgSendCGFloat(args: FFIVarargs) -> CGFloat by func("objc_msgSend")
    val objc_msgSendFloat: (args: FFIVarargs) -> Float by func("objc_msgSend")
    val objc_msgSendDouble: (args: FFIVarargs) -> Double by func("objc_msgSend")
    //val objc_msgSendNSPoint(args: FFIVarargs): NSPointRes by func("objc_msgSend")
    //val objc_msgSendNSRect(args: FFIVarargs): NSRectRes by func("objc_msgSend")

    // @TODO: Check: Error looking up function 'objc_msgSend_stret': dlsym(RTLD_DEFAULT, objc_msgSend_stret): symbol not found
    // Was only available on Intel macs?
    val objc_msgSend_stret: (structPtr: Any?, args: FFIVarargs) -> Unit by func("objc_msgSend_stret", required = false)

    /*
    fun objc_msgSend(a: Long, b: Long): Long
    fun objc_msgSend(a: Long, b: Long, c: Long): Long
    fun objc_msgSend(a: Long, b: Long, c: String): Long
    fun objc_msgSend(a: Long, b: Long, c: ByteArray, d: Int, e: Int): Long
    fun objc_msgSend(a: Long, b: Long, c: ByteArray, len: Int): Long
    fun objc_msgSend(a: Long, b: Long, c: CharArray, len: Int): Long
     */
    val method_getName: (m: Long) -> Long by func()
    val method_getNameString: (m: Long) -> String by func("method_getName")

    val sel_registerName: (name: String) -> Long by func()
    val sel_getName: (sel: Long) -> String by func()
    val sel_getNameString: (sel: String) -> String by func("sel_getName")

    val objc_allocateClassPair: (clazz: Long, name: String, extraBytes: Int) -> Long by func()
    val object_getIvar: (obj: Long, ivar: Long) -> Long by func()

    val class_getInstanceVariable: (clazz: Long, name: String) -> Long by func()
    val class_getProperty: (clazz: Long, name: String) -> Long by func()

    @OptIn(ExperimentalStdlibApi::class)
    val class_addMethod: (cls: Long, name: Long, imp: FFICallback, types: String) -> Long by func()
    val class_conformsToProtocol: (cls: Long, protocol: Long) -> Boolean by func()

    val object_getClass: (obj: Long) -> Long by func()
    val class_getName: (clazz: Long) -> String by func()

    val object_getClassName: (obj: Long) -> String by func()
    val class_getImageName: (obj: Long) -> String by func()

    val property_getName: (prop: Long) -> String by func()
    val property_getAttributes: (prop: Long) -> String by func()

    val class_getInstanceMethod: (cls: Long, id: Long) -> Long by func()

    val method_getReturnType: (id: Long, dst: FFIPointer?, dst_length: Long) -> Unit by func()
    val method_getTypeEncoding: (ptr: FFIPointer?) -> String by func()

    val class_createInstance: (cls: Long, extraBytes: Long) -> Long by func()
    val class_copyPropertyList: (cls: Long, outCountPtr: IntArray) -> FFIPointer? by func()
    val class_copyIvarList: (cls: Long, outCountPtr: IntArray) -> FFIPointer? by func()
    val ivar_getName: (ivar: FFIPointer?) -> String? by func()
    val ivar_getTypeEncoding: (ivar: FFIPointer?) -> String? by func()
}

fun FFIObjc.getClassByName(name: String): ObjcClassRef? {
    val id = FFIObjc.objc_lookUpClass(name)
    return if (id != 0L) ObjcClassRef(id) else null
}

fun FFIObjc.getAllClassIDs(): List<ObjcClassRef> = ffiScoped {
    val total = objc_getClassList(null, 0)
    val data = allocBytes((total * 8))
    //println(data.getLong(0L))
    val total2 = objc_getClassList(data, total)
    return (0 until total2).map { ObjcClassRef(data.getS64((it * 8))) }
}

interface ObjcProtocolClassBaseRef {
    val name: String
}

data class ObjcMethodRef(val objcClass: ObjcProtocolClassBaseRef, val ptr: FFIPointer?) {
    val name: String by lazy { FFIObjc.method_getNameString(ptr.address) }
    val selName: String by lazy { FFIObjc.sel_getNameString(name) }
    val types: String by lazy { FFIObjc.method_getTypeEncoding(ptr) }
    val parsedTypes: ObjcMethodDesc by lazy { ObjcTypeParser.parse(types) }

    override fun toString(): String = "${objcClass.name}.$name"
}

data class ObjcClassRef(val ref: Long) : ObjcProtocolClassBaseRef {
    companion object {
        fun listAll(): List<ObjcClassRef> = FFIObjc.getAllClassIDs()
        fun fromName(name: String): ObjcClassRef? = FFIObjc.getClassByName(name)
    }

    fun createInstance(extraBytes: Int = 0): Long {
        return FFIObjc.class_createInstance(ref, extraBytes.toLong())
    }

    fun dumpKotlin() {
        println("class $name(id: Long) : NSObject(id) {")
        listIVars()
        listProperties()
        for (method in listMethods()) {
            println("  " + createKotlinMethod(method.name, method.parsedTypes))
        }
        println("}")
    }

    override val name: String by lazy { FFIObjc.object_getClassName(ref) }
    val imageName: String by lazy { FFIObjc.class_getImageName(ref) }

    fun listMethods(): List<ObjcMethodRef> {
        val nitemsPtr = IntArray(1)
        val items2 = FFIObjc.class_copyMethodList(ref, nitemsPtr)!!
        val nitems = nitemsPtr[0]
        return (0 until nitems).map {
            ObjcMethodRef(this, items2.getFFIPointer(Long.SIZE_BYTES * it))
        }
    }

    fun listProperties() {
        val outCountPtr = IntArray(1)
        val properties = FFIObjc.class_copyPropertyList(ref, outCountPtr)!!
        val outCount = outCountPtr[0]
        for (n in 0 until outCount) {
            val prop = properties.getFFIPointer(n * 8)
            val propName = FFIObjc.property_getName(prop.address)
            val attributes = FFIObjc.property_getAttributes(prop.address)
            println("  // PROP: * $propName : $attributes")
        }
        //TODO("listProperties. outCount=$outCount")
    }

    fun listIVars() {
        val outCountPtr = IntArray(1)
        val ivars = FFIObjc.class_copyIvarList(ref, outCountPtr)
        val outCount = outCountPtr[0]
        for (n in 0 until outCount) {
            val ivar = ivars!!.getFFIPointer(n * 8)
            val ivarName = FFIObjc.ivar_getName(ivar)
            val encoding = FFIObjc.ivar_getTypeEncoding(ivar)
            println("  // IVAR: * ivar=$ivarName : $encoding")
        }
        //TODO("listIVars. outCount=$outCount")
    }

    override fun toString(): String = "ObjcClass[$name]"
}

class ObjcProtocol(val ref: Long) : ObjcProtocolClassBaseRef {
    override val name: String by lazy { FFIObjc.protocol_getName(ref) }

    fun dumpKotlin() {
        println("interface $name : ObjcDynamicInterface {")
        for (method in listMethods()) {
            method.dumpKotlin()
        }
        println("}")
    }

    fun listMethods(): List<ObjcMethodDescription> {
        val nitemsPtr = IntArray(1)
        val items2 = FFIObjc.protocol_copyMethodDescriptionList(ref, true, true, nitemsPtr)!!
        val nitems = nitemsPtr[0]
        val out = ArrayList<String>(nitems)
        //println("nitems=$nitems")
        return (0 until nitems).map { n ->
            val namePtr = items2.getS64(Long.SIZE_BYTES * (n * 2 + 0))
            val typesPtr = items2.getS64(Long.SIZE_BYTES * (n * 2 + 1))
            val typesStr = CreateFFIPointer(typesPtr)!!.getStringz()
            ObjcMethodDescription(this, namePtr, typesStr)
            //println("$selName: $typesStr")
            //val selName = ObjectiveC.sel_getName(mname)
        }
    }
    override fun toString(): String = "ObjcProtocolRef(${FFIObjc.protocol_getName(ref)})"

    companion object {
        fun fromName(name: String): ObjcProtocol? =
            FFIObjc.objc_getProtocol(name).takeIf { it != 0L }?.let { ObjcProtocol(it) }

        fun listAll(): List<ObjcProtocol> {
            val sizePtr = IntArray(1)
            val ptr = FFIObjc.objc_copyProtocolList(sizePtr)!!
            val size = sizePtr[0]
            return (0 until size).map { ObjcProtocol(ptr.getFFIPointer(FFI_POINTER_SIZE * it).address) }
        }
    }
}

data class ObjcMethodDescription(
    val protocol: ObjcProtocol,
    val id: Long,
    val types: String
) {
    val method by lazy { FFIObjc.class_getInstanceMethod(protocol.ref, id) }
    val name: String by lazy { FFIObjc.sel_getName(id) }
    val parsedTypes: ObjcMethodDesc by lazy { ObjcTypeParser.parse(types) }

    override fun toString(): String = "ObjcMethodDescription[$name$parsedTypes]"
    fun dumpKotlin() {
        println("  " + createKotlinMethod(name, parsedTypes))
    }
}

private fun String.keywordQuotedIfRequired(): String = if (this in KotlinKeywords) "`$this`" else this

private fun createKotlinMethod(
    name: String, parsedTypes: ObjcMethodDesc,
    setName: String? = null, setParsedTypes: ObjcMethodDesc? = null,
): String {
    val types = parsedTypes.desc
    val parts = name.split(":")
    val fullName = parts[0]
    val firstArg = fullName.substringAfterLast("With").replaceFirstChar { it.lowercase() }
    val baseName = fullName.substringBeforeLast("With")
    val argNames = listOf(firstArg) + parts.drop(1)
    val paramsWithNames = argNames.zip(parsedTypes.params.drop(2))
    val paramsStr = paramsWithNames.joinToString(", ") { "${it.first.keywordQuotedIfRequired()}: ${it.second.type.toKotlinString()}" }
    val returnTypeStr = parsedTypes.returnType.type.toKotlinString()
    return when {
        paramsWithNames.isEmpty() && parsedTypes.returnType.type != PrimitiveObjcType.VOID -> {
            "val $baseName: $returnTypeStr by objProp(\"$name\", \"$types\")"
        }
        else -> {
            //return "@ObjcDesc(\"$name\", \"$types\") fun $baseName($paramsStr): $returnTypeStr"
            "val $baseName: ($paramsStr) -> $returnTypeStr by objFunc(\"$name\", \"$types\")"
        }
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

val KotlinKeywords = setOf("object", "val", "class")

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
        NINT -> "FFINativeLong"
        NUINT -> "FFINativeLong"
        FLOAT -> "Float"
        DOUBLE -> "Double"
        BLOCK -> "(() -> Unit)"
    }
}

object ObjcTypeParser {
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
            '#' -> PrimitiveObjcType.ID
            ':' -> PrimitiveObjcType.SEL
            'C' -> PrimitiveObjcType.BYTE
            'i' -> PrimitiveObjcType.INT
            'I' -> PrimitiveObjcType.UINT
            'q' -> PrimitiveObjcType.NINT
            'Q' -> PrimitiveObjcType.NUINT
            '^' -> PointerObjcType(parseType(str))
            '?' -> PrimitiveObjcType.BLOCK
            '*' -> PointerObjcType(PrimitiveObjcType.BYTE)
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

open class NSClass(id: Long) : NSObject(id) {
    constructor(name: String) : this(FFIObjc.objc_getClass(name))

    val OBJ_CLASS = ObjcRef(id)

    val name by lazy { FFIObjc.class_getName(id) }

    override fun toString(): String = "NSClass[$id]($name)"
}

//open class NSObject(val id: Long) : FFILib() {
open class NSObject(val id: Long) {
    constructor(id: ObjcRef, unit: Unit = Unit) : this(id.id)

    init {
        check(id != 0L) { "NSObject is null" }
    }

    val ref = ObjcRef(id)

    class FuncInfo<T : Function<*>>(val type: KType, val selector: String, val def: String) {
        operator fun provideDelegate(
            thisRef: NSObject,
            prop: KProperty<*>
        ): ReadOnlyProperty<NSObject, T> = NSObject.FuncDelegate<T>(thisRef, selector, def, type).also {
            //thisRef.functions.add(it)
            //if (!thisRef.lazyCreate) it.getValue(thisRef, prop)
        }
    }

    class FuncDelegate<T : Function<*>>(val base: NSObject, val selector: String, val def: String, val type: KType) : ReadOnlyProperty<NSObject, T> {
        val parts = FFILib.extractTypeFunc(type)
        //val generics = type.arguments.map { it.type?.classifier }
        val params = parts.paramsClass
        val ret = parts.retClass
        var cached: T? = null
        override fun getValue(thisRef: NSObject, property: KProperty<*>): T {
            return FFICreateProxyFunction<T>(type) { args ->
                thisRef.msgSend(selector, *args)
            }
        }
    }

    inline fun <reified T : Function<*>> objFunc(selector: String, def: String): FuncInfo<T> {
        return FuncInfo<T>(typeOf<T>(), selector, def)
    }

    class PropInfo<T>(val type: KType, val selector: String, val def: String) {
        val info = FuncInfo<() -> T>(type, selector, def)

        operator fun getValue(thisRef: NSObject, property: KProperty<*>): T {
            return info.provideDelegate(thisRef, property).getValue(thisRef, property).invoke()
        }
    }

    inline fun <reified T> objProp(selector: String, def: String): PropInfo<T> {
        return PropInfo<T>(typeOf<() -> T>(), selector, def)
    }

    fun msgSend(sel: String, vararg args: Any?): Long = ref.msgSend(sel, *args)
    fun msgSendRef(sel: String, vararg args: Any?): ObjcRef = ref.msgSendRef(sel, *args)
    fun msgSendVoid(sel: String, vararg args: Any?): Unit = ref.msgSendVoid(sel, *args)
    fun msgSendInt(sel: String, vararg args: Any?): Int = ref.msgSendInt(sel, *args)
    fun msgSendFloat(sel: String, vararg args: Any?): Float = ref.msgSendFloat(sel, *args)
    fun msgSendDouble(sel: String, vararg args: Any?): Double = ref.msgSendDouble(sel, *args)
    //fun msgSendCGFloat(sel: String, vararg args: Any?): CGFloat = FFIObjc.objc_msgSendCGFloat(id, sel(sel), *args)
    //fun msgSendNSPoint(sel: String, vararg args: Any?): NSPointRes = FFIObjc.objc_msgSendNSPoint(id, sel(sel), *args)
    //fun msgSend_stret(sel: String, vararg args: Any?): Unit = FFIObjc.objc_msgSend_stret(id, sel(sel), *args)

    fun alloc(): NSObject = NSObject(msgSend("alloc"))

    companion object : NSClass("NSObject") {
        fun sel(name: String): Long = ObjcSel(name).id
    }

    val objcClass: NSClass get() = NSClass(msgSend("class"))

    override fun toString(): String = "NSObject(${objcClass})"
}

inline class ObjcRef(val id: Long) {
    constructor(ref: ObjcRef, unit: Unit = Unit) : this(ref.id)

    fun msgSend(sel: ObjcSel, vararg args: Any?): Long = FFIObjc.objc_msgSend(FFIVarargs(id, (sel.id), *args))
    fun msgSendRef(sel: ObjcSel, vararg args: Any?): ObjcRef = ObjcRef(msgSend(sel, *args))
    fun msgSendVoid(sel: ObjcSel, vararg args: Any?): Unit = FFIObjc.objc_msgSendVoid(FFIVarargs(id, (sel.id), *args))
    fun msgSendInt(sel: ObjcSel, vararg args: Any?): Int = FFIObjc.objc_msgSendInt(FFIVarargs(id, (sel.id), *args))
    fun msgSendFloat(sel: ObjcSel, vararg args: Any?): Float = FFIObjc.objc_msgSendFloat(FFIVarargs(id, (sel.id), *args))
    fun msgSendDouble(sel: ObjcSel, vararg args: Any?): Double = FFIObjc.objc_msgSendDouble(FFIVarargs(id, (sel.id), *args))

    fun msgSend(sel: String, vararg args: Any?): Long = msgSend(ObjcSel(sel), *args)
    fun msgSendRef(sel: String, vararg args: Any?): ObjcRef = msgSendRef(ObjcSel(sel), *args)
    fun msgSendVoid(sel: String, vararg args: Any?): Unit = msgSendVoid(ObjcSel(sel), *args)
    fun msgSendInt(sel: String, vararg args: Any?): Int = msgSendInt(ObjcSel(sel), *args)
    fun msgSendFloat(sel: String, vararg args: Any?): Float = msgSendFloat(ObjcSel(sel), *args)
    fun msgSendDouble(sel: String, vararg args: Any?): Double = msgSendDouble(ObjcSel(sel), *args)
    //fun msgSendCGFloat(sel: String, vararg args: Any?): CGFloat = FFIObjc.objc_msgSendCGFloat(id, sel(sel), *args)
    //fun msgSendNSPoint(sel: String, vararg args: Any?): NSPointRes = FFIObjc.objc_msgSendNSPoint(id, sel(sel), *args)
    //fun msgSend_stret(sel: String, vararg args: Any?): Unit = FFIObjc.objc_msgSend_stret(id, sel(sel), *args)
}

inline class ObjcSel(val id: Long) {
    companion object {
        private val lock = Lock()
        private val selectors = HashMap<String, ObjcSel>()

        operator fun invoke(name: String): ObjcSel = lock {
            selectors.getOrPut(name) {
                ObjcSel(FFIObjc.sel_registerName(name).also {
                    if (it == 0L) error("Invalid selector '$name'")
                })
            }
        }
    }
}
