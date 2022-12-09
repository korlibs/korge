package com.soywiz.korgw

import com.soywiz.kgl.GLFuncNull
import com.soywiz.korag.gl.AGNative
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.windows.GetProcAddress
import platform.windows.HANDLE
import platform.windows.HMONITOR
import platform.windows.HMONITORVar
import platform.windows.HWND
import platform.windows.LoadLibraryA
import platform.windows.MONITOR_DEFAULTTONEAREST
import platform.windows.MonitorFromWindow

class Win32AGOpengl constructor(val hwnd: () -> HWND?) : AGNative() {
    companion object {

    }
}
