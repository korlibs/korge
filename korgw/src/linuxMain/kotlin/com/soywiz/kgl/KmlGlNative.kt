// WARNING: File autogenerated DO NOT modify
// https://www.khronos.org/registry/OpenGL/api/GLES2/gl2.h
@file:Suppress("unused", "RedundantUnitReturnType", "PropertyName")

package com.soywiz.kgl

import X11Embed.*
import com.soywiz.kmem.dyn.*
import kotlinx.cinterop.*

typealias XVisualInfo = ULongVar // Only used as pointer
typealias GLXDrawable = COpaquePointer
typealias GLXContext = COpaquePointer
typealias KeySym = Int
typealias CString = CPointer<ByteVar>
typealias CDisplayPointer = CPointer<Display>?

internal object GLLib : DynamicLibrary("libGLX.so.0") {
    val glXGetProcAddress by func<(name: CPointer<ByteVar>) -> CPointer<out CPointed>?>()
    val glXChooseVisual by func<(d: CDisplayPointer, scr: Int, ptr: CPointer<IntVar>?) -> CPointer<XVisualInfo>?>()
    val glXGetCurrentDisplay by func<() -> CDisplayPointer>()
    val glXGetCurrentDrawable by func<() -> GLXDrawable>()
    val glXSwapBuffers by func<(d: CDisplayPointer, w: Window) -> Unit>()
    val glXMakeCurrent by func<(d: CDisplayPointer, w: Window, glc: GLXContext) -> Unit>()
    val glXCreateContext by func<(d: CDisplayPointer, vi: CPointer<XVisualInfo>?, shareList: GLXContext?, direct: Int) -> GLXContext>()
}

internal actual fun glGetProcAddressAnyOrNull(name: String): COpaquePointer? = memScoped {
    GLLib.glXGetProcAddress(name.cstr.placeTo(this)) ?: GLLib.getSymbol(name)?.toCPointer()
}

actual class KmlGlNative actual constructor() : NativeBaseKmlGl() {
    override val gles: Boolean = true
    override val linux: Boolean = true
}
