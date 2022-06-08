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

class Win32AGOpengl(val hwnd: () -> HWND?) : AGNative() {
    override val pixelsPerInch: Double get() {
        try {
            return kotlin.math.max(pixelsPerInchRaw, 96.0)
        } catch (e: Throwable) {
            e.printStackTrace()
            return 96.0
        }
    }
    val pixelsPerInchRaw: Double get() = memScoped {
        val hwnd = hwnd()

        // Windows 8.1
        if (GetDpiForMonitor != null) {
            val monitor = MonitorFromWindow(hwnd, MONITOR_DEFAULTTONEAREST)
            val dpiX = alloc<UIntVar>()
            val dpiY = alloc<UIntVar>()
            val result = GetDpiForMonitor!!(monitor, MDT_RAW_DPI, dpiX.ptr, dpiY.ptr)
            //println("GetDpiForMonitor: hwnd=$hwnd, result=$result, monitor=$monitor, dpiX=${dpiX.value}")
            if (result == 0) {
                return dpiX.value.toDouble()
            }
        }

        // Windows 10 only or greater
        if (GetDpiForWindow != null) {
            // This usually returns 96.0 or other thing if scaled
            val dpi = GetDpiForWindow!!(hwnd).toDouble()
            //println("GetDpiForWindow.dpi: hwnd=$hwnd, $dpi")
            if (dpi >= 96.0) return dpi
        }

        // Older Windows Versions

        // @TODO: Use monitor information:
        // @TODO: https://stackoverflow.com/questions/577736/how-to-obtain-the-correct-physical-size-of-the-monitor
        return 96.0
    }

    companion object {
        const val MDT_EFFECTIVE_DPI = 0
        const val MDT_ANGULAR_DPI = 1
        const val MDT_RAW_DPI = 2
        const val MDT_DEFAULT = MDT_EFFECTIVE_DPI

        private val shCore by lazy { LoadLibraryA("Shcore.dll") }
        private val GetDpiForMonitor: CPointer<CFunction<(hmonitor: HMONITOR?, dpiType: Int, dpiX: CPointer<UIntVar>, dpiY: CPointer<UIntVar>) -> Int>>? by lazy {
            GetProcAddress(shCore, "GetDpiForMonitor")?.reinterpret()
        }
        private val user32 by lazy { LoadLibraryA("user32.dll") }
        private val GetDpiForWindow: CPointer<CFunction<(hwnd: HWND?) -> Int>>? by lazy {
            GetProcAddress(user32, "GetDpiForWindow")?.reinterpret()
        }
    }
}
