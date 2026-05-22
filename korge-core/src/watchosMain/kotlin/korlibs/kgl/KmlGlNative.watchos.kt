@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package korlibs.kgl

import kotlinx.cinterop.COpaquePointer

internal actual fun glGetProcAddressAnyOrNull(name: String): COpaquePointer? = null

actual class KmlGlNative actual constructor() : NativeBaseKmlGl()
