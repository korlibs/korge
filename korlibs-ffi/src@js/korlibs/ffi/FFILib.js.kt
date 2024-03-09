package korlibs.ffi

import JsArray
import korlibs.image.bitmap.*
import korlibs.js.*
import korlibs.memory.*
import korlibs.memory.Buffer
import korlibs.platform.*
import kotlinx.coroutines.*
import org.khronos.webgl.*
import kotlin.js.Promise
import kotlin.reflect.*

fun KType.funcToDenoDef(): dynamic {
    val ftype = FFILib.extractTypeFunc(this)
    return def(
        ftype.ret?.toDenoFFI(ret = true),
        *ftype.params.map { it?.toDenoFFI(ret = false) }.toTypedArray(),
        nonblocking = ftype.retClass == Deferred::class
    )
}

fun KType.toDenoFFI(ret: Boolean): dynamic {
    if (this.classifier == Deferred::class) {
        return this.arguments.first().type?.classifier?.toDenoFFI(ret)
    } else {
        return this?.classifier?.toDenoFFI(ret)
    }
}

fun KClassifier.toDenoFFI(ret: Boolean): dynamic {
    return when (this) {
        Long::class -> "usize"
        Int::class -> "i32"
        Float::class -> "f32"
        Double::class -> "f64"
        Boolean::class -> "i8"
        NativeImageRef::class -> "buffer"
        ByteArray::class -> "buffer"
        ShortArray::class -> "buffer"
        CharArray::class -> "buffer"
        IntArray::class -> "buffer"
        FloatArray::class -> "buffer"
        DoubleArray::class -> "buffer"
        BooleanArray::class -> "buffer"
        FFIPointerArray::class -> "buffer"
        //LongArray::class -> "buffer"
        Buffer::class -> "buffer"
        DenoPointer::class -> "pointer"
        String::class -> if (ret) "pointer" else "buffer"
        Unit::class -> "void"
        else -> TODO("$this")
    }
}

external class WebAssembly {
    class Instance(module: Module, imports: dynamic) {
        val exports: dynamic
        val memory: ArrayBuffer
    }
    class Module(data: ByteArray)
}

private external val JSON: dynamic

actual fun FFILibSym(lib: FFILib): FFILibSym {
    return FFILibSymJS(lib)
}

class FFILibSymJS(val lib: FFILib) : FFILibSym {
    val symbolsByName: Map<String, FFILib.FuncDelegate<*>> by lazy { lib.functions.associateBy { it.bname } }

    val syms: dynamic by lazy {
        lib as FFILib
        (listOfNotNull(lib.resolvedPath) + lib.paths).firstNotNullOfOrNull { path ->
            try {
                Deno.dlopen<dynamic>(
                    path, jsObject(
                        *lib.functions.map {
                            it.bname to it.type.funcToDenoDef()
                        }.toTypedArray()
                    )
                ).symbols
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }
        }.unsafeCast<Any?>().also {
            if (it == null) {
                println("Couldn't load library: dymlib=$it : ${lib.resolvedPath}")
            }
        }
    }

    override fun <T> get(name: String, type: KType): T {
        if (syms == null) error("Can't get symbol '$name' for ${lib::class} : '${(lib as FFILib).paths}'")
        //return syms[name]
        val sym = symbolsByName[name]!!
        return preprocessFunc(sym.type, syms[name], name, sym.config)
    }

    override fun close() {
        super.close()
    }
}


// @TODO: Optimize this
private fun preprocessFunc(type: KType, func: dynamic, name: String?, config: FFIFuncConfig): dynamic {
    val ftype = FFILib.extractTypeFunc(type)
    val convertToString = ftype.retClass == String::class
    return {
        val arguments = js("(arguments)")
        val params = ftype.paramsClass
        for (n in 0 until params.size) {
            val param = params[n]
            var v = arguments[n]
            if (param == String::class) {
                v = (v.toString() + "\u0000").encodeToByteArray()
            }
            if (v is FFIPointerArray) v = v.data
            if (v is Buffer) v = v.dataView
            if (v is Boolean) v = if (v) 1 else 0
            if (v is Long) v = (v as Long).toJsBigInt()
            //console.log("param", n, v)
            arguments[n] = v
        }
        //console.log("arguments", arguments)
        try {
            val result = func.apply(null, arguments)
            //console.log("result", result)
            val res2 = when {
                result == null -> null
                convertToString -> {
                    val ptr = (result.unsafeCast<DenoPointer>())
                    getCString(ptr)
                }
                else -> result
            }
            if (res2 is Promise<*>) {
                (res2.unsafeCast<Promise<*>>()).asDeferred()
            } else {
                res2
            }
        } catch (e: dynamic) {
            println("ERROR calling[$name]: $type : ${JsArray.from(arguments).toList()}")
            throw e
        }
    }
}

//fun strlen(ptr: FFIPointer?): Int {
//    if (ptr == null) return 0
//    for (n in 0 until 1000000) {
//        if (ptr.getUnalignedI8(n) == 0.toByte()) return n
//    }
//    error("String too long")
//}
//
//fun getCString(ptr: FFIPointer?): String? {
//    if (ptr == null) return null
//    val len = strlen(ptr)
//    val ba = ByteArray(len)
//    for (n in 0 until ba.size) ba[n] = ptr.getUnalignedI8(n)
//    return ba.decodeToString()
//}
fun getCString(ptr: FFIPointer?): String? {
    if (ptr == null) return null
    return Deno.UnsafePointerView.getCString(ptr)
}

actual class FFIArena actual constructor() {
    // @TODO: dlopen: malloc and free
    actual fun allocBytes(size: Int): FFIPointer {
        // @TODO: Check!
        return Int8Array(size).unsafeCast<FFIPointer>()
    }
    actual fun clear(): Unit {
    }
}

actual fun <T> FFICreateProxyFunction(type: KType, handler: (args: Array<Any?>) -> Any?): T {
    TODO()
}

actual typealias FFIPointer = DenoPointer
actual val FFI_POINTER_SIZE: Int = 8

actual typealias FFIMemory = Uint8Array

actual val FFI_SUPPORTED: Boolean = Deno.isDeno

actual fun CreateFFIMemory(size: Int): FFIMemory = Uint8Array(size)
actual fun CreateFFIMemory(bytes: ByteArray): FFIMemory = bytes.asDynamic()

actual val FFIMemory.pointer: FFIPointer get() = Deno.UnsafePointer.of(this)

actual fun FFIPointer.getStringz(): String {
    return getCString(this) ?: "<null>"
    //return this.readStringz()
}
actual fun FFIPointer.getWideStringz(): String {
    if (this.value == JsBigInt(0)) return "<null>"

    val ptr = Deno.UnsafePointerView(this)
    var strlen = 0
    while (true) {
        if (ptr.getInt16(strlen * 2).toInt() == 0) break
        strlen++
    }
    val chars = CharArray(strlen)
    for (n in 0 until strlen) {
        chars[n] = ptr.getInt16(n * 2).toInt().toChar()
    }
    return chars.concatToString()
}
actual val FFIPointer?.address: Long get() {
    val res = Deno.UnsafePointer.value(this)
    return if (res is Number) res.toLong() else res.unsafeCast<JsBigInt>().toLong()
}
actual fun CreateFFIPointer(ptr: Long): FFIPointer? = if (ptr == 0L) null else Deno.UnsafePointer.create(ptr.toJsBigInt())
actual val FFIPointer?.str: String get() = if (this == null) "Pointer(null)" else "Pointer($value)"

fun FFIPointer.getDataView(offset: Int, size: Int): org.khronos.webgl.DataView {
    return org.khronos.webgl.DataView(Deno.UnsafePointerView(this).getArrayBuffer(size, offset))
}

actual fun FFIPointer.getS8(byteOffset: Int): Byte = Deno.UnsafePointerView(this).getInt8(byteOffset)
actual fun FFIPointer.getS16(byteOffset: Int): Short = Deno.UnsafePointerView(this).getInt16(byteOffset)
actual fun FFIPointer.getS32(byteOffset: Int): Int = Deno.UnsafePointerView(this).getInt32(byteOffset)
actual fun FFIPointer.getS64(byteOffset: Int): Long {
    val low = getS32(byteOffset)
    val high = getS32(byteOffset + 4)
    return Long.fromLowHigh(low, high)
}
actual fun FFIPointer.getF32(byteOffset: Int): Float = Deno.UnsafePointerView(this).getFloat32(byteOffset)
actual fun FFIPointer.getF64(byteOffset: Int): Double = Deno.UnsafePointerView(this).getFloat64(byteOffset)
actual fun FFIPointer.set8(value: Byte, byteOffset: Int): Unit = getDataView(byteOffset, 1).setInt8(0, value)
actual fun FFIPointer.set16(value: Short, byteOffset: Int): Unit = getDataView(byteOffset, 2).setInt16(0, value, true)
actual fun FFIPointer.set32(value: Int, byteOffset: Int): Unit = getDataView(byteOffset, 4).setInt32(0, value, true)
actual fun FFIPointer.set64(value: Long, byteOffset: Int) {
    set32(value._low, byteOffset)
    set32(value._high, byteOffset + 4)
}
actual fun FFIPointer.setF32(value: Float, byteOffset: Int): Unit = getDataView(byteOffset, 4).setFloat32(0, value, true)
actual fun FFIPointer.setF64(value: Double, byteOffset: Int): Unit = getDataView(byteOffset, 8).setFloat64(0, value, true)

actual fun FFIPointer.getIntArray(size: Int, byteOffset: Int): IntArray {
    val view = Deno.UnsafePointerView(this)
    val out = IntArray(size)
    for (n in 0 until size) {
        out[n] = view.asDynamic().getInt32(byteOffset + n * 4)
    }
    //Deno.UnsafePointerView.getCString()
    //TODO("Not yet implemented")
    return out
}

actual fun <T> FFIPointer.castToFunc(type: KType, config: FFIFuncConfig): T {
    val def = type.funcToDenoDef()
    val res = Deno.UnsafeFnPointer(this, def)
    //console.log("castToFunc.def=", def, "res=", res)
    val func: dynamic = {
        val arguments = js("(arguments)")
        res.asDynamic().call.apply(res, arguments)
    }
    return preprocessFunc(type, func, null, config)

}

private fun def(result: dynamic, vararg params: dynamic, nonblocking: Boolean = false): dynamic =
    jsObject("parameters" to params, "result" to result, "nonblocking" to nonblocking)

private fun jsObject(vararg pairs: Pair<String, Any?>): dynamic {
    val out = jsEmptyObj()
    for (pair in pairs) out[pair.first] = pair.second
    return out
}

private fun jsEmptyObj(): dynamic = js("({})")

private external val Deno: dynamic
