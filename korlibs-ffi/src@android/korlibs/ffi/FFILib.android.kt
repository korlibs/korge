package korlibs.ffi

import kotlin.reflect.*

actual fun FFILibSym(lib: FFILib): FFILibSym {
    return object : FFILibSym {
    }
}

actual fun <T> FFICreateProxyFunction(type: KType, handler: (args: Array<Any?>) -> Any?): T {
    TODO()
}

actual class FFIPointer

actual class FFIMemory

actual val FFI_SUPPORTED: Boolean = false

actual fun CreateFFIMemory(size: Int): FFIMemory = TODO()
actual fun CreateFFIMemory(bytes: ByteArray): FFIMemory = TODO()
actual val FFIMemory.pointer: FFIPointer get() = TODO()

actual fun CreateFFIPointer(ptr: Long): FFIPointer? = TODO()

actual val FFI_POINTER_SIZE: Int get() = TODO()

actual val FFIPointer?.address: Long get() = TODO()

actual fun FFIPointer.getStringz(): String {
    TODO("Not yet implemented")
}

actual fun FFIPointer.getWideStringz(): String {
    TODO("Not yet implemented")
}

actual val FFIPointer?.str: String
    get() = TODO("Not yet implemented")


actual fun FFIPointer.getIntArray(size: Int, byteOffset: Int): IntArray {
    TODO("Not yet implemented")
}

actual fun <T> FFIPointer.castToFunc(type: KType, config: FFIFuncConfig): T {
    TODO("Not yet implemented")
}

actual fun FFIPointer.getS8(byteOffset: Int): Byte = TODO()
actual fun FFIPointer.getS16(byteOffset: Int): Short = TODO()
actual fun FFIPointer.getS32(byteOffset: Int): Int = TODO()
actual fun FFIPointer.getS64(byteOffset: Int): Long = TODO()
actual fun FFIPointer.getF32(byteOffset: Int): Float = TODO()
actual fun FFIPointer.getF64(byteOffset: Int): Double = TODO()

actual fun FFIPointer.set8(value: Byte, byteOffset: Int): Unit = TODO()
actual fun FFIPointer.set16(value: Short, byteOffset: Int): Unit = TODO()
actual fun FFIPointer.set32(value: Int, byteOffset: Int): Unit = TODO()
actual fun FFIPointer.set64(value: Long, byteOffset: Int): Unit = TODO()
actual fun FFIPointer.setF32(value: Float, byteOffset: Int): Unit = TODO()
actual fun FFIPointer.setF64(value: Double, byteOffset: Int): Unit = TODO()

actual class FFIArena actual constructor() {
    actual fun allocBytes(size: Int): FFIPointer = TODO()
    actual fun clear(): Unit = TODO()
}
