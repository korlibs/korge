package korlibs.ffi

import korlibs.image.bitmap.*
import korlibs.io.*
import korlibs.io.runtime.deno.*
import korlibs.js.*
import korlibs.memory.*
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
        NativeImage::class -> "buffer"
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
    val symbolsByName: Map<String, FFILib.FuncDelegate<*>> by lazy { lib.functions.associateBy { it.name } }

    val syms: dynamic by lazy {
        lib as FFILib
        (listOfNotNull(lib.resolvedPath) + lib.paths).firstNotNullOfOrNull { path ->
            try {
                Deno.dlopen<dynamic>(
                    path, jsObject(
                        *lib.functions.map {
                            it.name to it.type.funcToDenoDef()
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
        return preprocessFunc(symbolsByName[name]!!.type, syms[name], name)
    }

    override fun close() {
        super.close()
    }
}


// @TODO: Optimize this
private fun preprocessFunc(type: KType, func: dynamic, name: String?): dynamic {
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


actual typealias FFIPointer = DenoPointer
actual val FFI_POINTER_SIZE: Int = 8

actual typealias FFIMemory = Uint8Array

actual fun CreateFFIMemory(size: Int): FFIMemory = Uint8Array(size)
actual fun CreateFFIMemory(bytes: ByteArray): FFIMemory = bytes.asDynamic()

actual val FFIMemory.pointer: FFIPointer get() = Deno.UnsafePointer.of(this)

actual fun FFIPointer.getStringz(): String {
    return getCString(this) ?: "<null>"
    //return this.readStringz()
}
actual val FFIPointer?.address: Long get() {
    val res = Deno.UnsafePointer.value(this)
    return if (res is Number) res.toLong() else res.unsafeCast<JsBigInt>().toLong()
}
actual fun CreateFFIPointer(ptr: Long): FFIPointer? = if (ptr == 0L) null else Deno.UnsafePointer.create(ptr.toJsBigInt())
actual val FFIPointer?.str: String get() = if (this == null) "Pointer(null)" else "Pointer($value)"

fun FFIPointer.getDataView(offset: Int, size: Int): DataView {
    return DataView(Deno.UnsafePointerView(this).getArrayBuffer(size, offset))
}

actual fun FFIPointer.getUnalignedI8(offset: Int): Byte = Deno.UnsafePointerView(this).getInt8(offset)
actual fun FFIPointer.getUnalignedI16(offset: Int): Short = Deno.UnsafePointerView(this).getInt16(offset)
actual fun FFIPointer.getUnalignedI32(offset: Int): Int = Deno.UnsafePointerView(this).getInt32(offset)
actual fun FFIPointer.getUnalignedI64(offset: Int): Long {
    val low = getUnalignedI32(offset)
    val high = getUnalignedI32(offset + 4)
    return Long.fromLowHigh(low, high)
}
actual fun FFIPointer.getUnalignedF32(offset: Int): Float = Deno.UnsafePointerView(this).getFloat32(offset)
actual fun FFIPointer.getUnalignedF64(offset: Int): Double = Deno.UnsafePointerView(this).getFloat64(offset)
actual fun FFIPointer.setUnalignedI8(value: Byte, offset: Int) = getDataView(offset, 1).setInt8(0, value)
actual fun FFIPointer.setUnalignedI16(value: Short, offset: Int) = getDataView(offset, 2).setInt16(0, value, true)
actual fun FFIPointer.setUnalignedI32(value: Int, offset: Int) = getDataView(offset, 4).setInt32(0, value, true)
actual fun FFIPointer.setUnalignedI64(value: Long, offset: Int) {
    setUnalignedI32(value._low, offset)
    setUnalignedI32(value._high, offset + 4)
}
actual fun FFIPointer.setUnalignedF32(value: Float, offset: Int) = getDataView(offset, 4).setFloat32(0, value, true)
actual fun FFIPointer.setUnalignedF64(value: Double, offset: Int) = getDataView(offset, 8).setFloat64(0, value, true)

actual fun FFIPointer.getIntArray(size: Int, offset: Int): IntArray {
    val view = Deno.UnsafePointerView(this)
    val out = IntArray(size)
    for (n in 0 until size) {
        out[n] = view.asDynamic().getInt32(offset + n * 4)
    }
    //Deno.UnsafePointerView.getCString()
    //TODO("Not yet implemented")
    return out
}

actual fun <T> FFIPointer.castToFunc(type: KType): T {
    val def = type.funcToDenoDef()
    val res = Deno.UnsafeFnPointer(this, def)
    //console.log("castToFunc.def=", def, "res=", res)
    val func: dynamic = {
        val arguments = js("(arguments)")
        res.asDynamic().call.apply(res, arguments)
    }
    return preprocessFunc(type, func, null)

}
