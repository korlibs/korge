package korlibs.internal.osx

import com.sun.jna.*
import korlibs.annotations.*

// @TODO: Change to a FFILib
@KeepNames
object CoreFoundation {
    // CFNumberGetValue(number: platform.CoreFoundation.CFNumberRef? /* = kotlinx.cinterop.CPointer<cnames.structs.__CFNumber>? */, theType: platform.CoreFoundation.CFNumberType /* = kotlin.Int */, valuePtr: kotlinx.cinterop.CValuesRef<*>?): kotlin.Boolean { /* compiled code */ }
    val LIB = "/System/Library/Frameworks/CoreFoundation.framework/Versions/A/CoreFoundation"
    val lib: NativeLibrary = NativeLibrary.getInstance("/System/Library/Frameworks/CoreFoundation.framework/Versions/A/CoreFoundation")
    val kCFBooleanFalse: Pointer? get() = lib.getGlobalVariableAddress("kCFBooleanFalse")
    val kCFBooleanTrue: Pointer? get() = lib.getGlobalVariableAddress("kCFBooleanTrue")
    //val kCFNumberIntType: Pointer? get() = lib.getGlobalVariableAddress("kCFNumberIntType")
    val kCFNumberIntType: Int get() = 9

    @JvmStatic val kCFRunLoopCommonModes: Pointer? = lib.getGlobalVariableAddress("kCFRunLoopCommonModes").getPointer(0L)
    @JvmStatic external fun CFRunLoopGetCurrent(): Pointer?
    @JvmStatic external fun CFRunLoopGetMain(): Pointer?
    @JvmStatic external fun CFRunLoopRun(): Void
    @JvmStatic external fun CFDataCreate(allocator: Pointer?, bytes: Pointer?, length: Int): Pointer?
    @JvmStatic external fun CFDataCreate(allocator: Pointer?, bytes: ByteArray, length: Int): Pointer?
    @JvmStatic external fun CFDataGetBytePtr(data: Pointer?): Pointer?
    @JvmStatic external fun CFDictionaryCreateMutable(allocator: Pointer?, capacity: Int, keyCallbacks: Pointer?, valueCallbacks: Pointer?): Pointer?
    @JvmStatic external fun CFDictionaryAddValue(theDict: Pointer?, key: Pointer?, value: Pointer?): Unit
    @JvmStatic external fun CFDictionaryGetValue(dict: Pointer?, key: Pointer?): Pointer?
    @JvmStatic external fun CFNumberGetValue(number: Pointer?, type: Int, holder: Pointer?)

    init {
        Native.register(LIB)
    }
}
