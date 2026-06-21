@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package korlibs.kgl

import kotlinx.cinterop.COpaquePointer
import platform.posix.RTLD_DEFAULT
import platform.posix.RTLD_LAZY
import platform.posix.dlopen
import platform.posix.dlsym

private val openGlFrameworkHandle: COpaquePointer? by lazy {
    dlopen("/System/Library/Frameworks/OpenGL.framework/OpenGL", RTLD_LAZY)
}

internal actual fun glGetProcAddressAnyOrNull(name: String): COpaquePointer? {
    return openGlFrameworkHandle?.let { dlsym(it, name) }
        ?: dlsym(RTLD_DEFAULT, name)
}

actual class KmlGlNative actual constructor() : NativeBaseKmlGl()
