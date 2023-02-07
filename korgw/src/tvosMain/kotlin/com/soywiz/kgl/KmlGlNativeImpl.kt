package com.soywiz.kgl

import kotlinx.cinterop.*

actual class KmlGlNative actual constructor() : NativeBaseKmlGl() {
}

internal actual fun glGetProcAddressAnyOrNull(name: String): COpaquePointer? {
    TODO()
    return null
}
