package com.soywiz.krypto

import kotlinx.cinterop.*
import platform.windows.*

private val bscryptLibrary by lazy { LoadLibraryA("Bcrypt.dll") }
// public fun BCryptGenRandom(hAlgorithm: platform.windows.BCRYPT_ALG_HANDLE? /* = kotlinx.cinterop.CPointer<out kotlinx.cinterop.CPointed>? */, pbBuffer: platform.windows.PUCHAR? /* = kotlinx.cinterop.CPointer<platform.windows.UCHARVar /* = kotlinx.cinterop.UByteVarOf<platform.windows.UCHAR /* = kotlin.UByte */> */>? */, cbBuffer: platform.windows.ULONG /* = kotlin.UInt */, dwFlags: platform.windows.ULONG /* = kotlin.UInt */): platform.windows.NTSTATUS /* = kotlin.Int */ { /* compiled code */ }
private val BCryptGenRandomDynamic by lazy {
    GetProcAddress(bscryptLibrary, "BCryptGenRandom")
		?.reinterpret<CFunction<Function4<BCRYPT_ALG_HANDLE?, PUCHAR?, ULONG, ULONG, Int>>>()
		?: error("Can't find BCryptGenRandom @ Bcrypt.dll")
}

actual fun fillRandomBytes(array: ByteArray) {
    if (array.size == 0) return

    array.usePinned { pin ->
        val ptr = pin.addressOf(0)
        val status = BCryptGenRandomDynamic(null, ptr.reinterpret(), array.size.convert(), 2.convert())
    }
}

actual fun seedExtraRandomBytes(array: ByteArray) {
    // No implementation for this?
}
