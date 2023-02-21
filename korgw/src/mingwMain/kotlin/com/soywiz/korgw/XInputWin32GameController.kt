package com.soywiz.korgw

import com.soywiz.korev.*
import com.soywiz.korgw.win32.*
import kotlinx.cinterop.*
import platform.windows.*

private val XINPUT_DLL by lazy { LoadLibraryA("xinput9_1_0.dll") }
private val XInputGetState by lazy {
    GetProcAddress(XINPUT_DLL, "XInputGetState")?.reinterpret<CFunction<(dwUserIndex: Int, pState: CPointer<ByteVar>?) -> Int>>()
}
private val WINMM_DLL by lazy { LoadLibraryA("Winmm.dll") }
private val joyGetDevCapsWDyn by lazy {
    GetProcAddress(WINMM_DLL, "joyGetDevCapsW")?.reinterpret<CFunction<(uJoyID: Int, pjc: CPointer<ByteVar>?, cbjc: Int) -> Int>>()
}

internal class XInputEventAdapter {
    val adapter = Win32XInputEventAdapterCommon(
        XInput { dwUserIndex, pState -> XInputGetState?.invoke(dwUserIndex, pState.pointer?.reinterpret()) ?: -1 },
        Joy32 { uJoyID, pjc, cbjc -> joyGetDevCapsWDyn?.invoke(uJoyID, pjc.pointer?.reinterpret(), cbjc) ?: -1 }
    )
    fun updateGamepadsWin32(emitter: GamepadInfoEmitter) {
        adapter.updateGamepads(emitter)
    }
}
