package korlibs.render.win32

import korlibs.event.*
import korlibs.io.util.*
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

internal class XInputEventAdapter {
    val input = XInput()
    val joy = Win32Joy()
    val common = Win32XInputEventAdapterCommon(
        XInput { dwUserIndex, pState -> input?.XInputGetState(dwUserIndex, pState.pointer?.optr) ?: -1 },
        Joy32 { uJoyID, pjc, cbjc -> joy?.joyGetDevCapsW(uJoyID, pjc.pointer?.optr, cbjc) ?: -1 }
    )

    fun updateGamepadsWin32(emitter: GamepadInfoEmitter) {
        common.updateGamepads(emitter)
    }

    internal interface XInput : Library {
        fun XInputGetState(dwUserIndex: Int, pState: Pointer?): Int

        companion object {
            operator fun invoke(): XInput? = runCatching {
                Native.load("xinput9_1_0.dll", XInput::class.java).also { it.XInputGetState(0, null) }
            }.getOrNullLoggingError()
        }
    }

    internal interface Win32Joy : Library {
        companion object {
            operator fun invoke(): Win32Joy? = runCatching {
                Native.load("Winmm.dll", Win32Joy::class.java).also { it.joyGetDevCapsW(0, null, 0) }
            }.getOrNullLoggingError()
        }

        fun joyGetDevCapsW(uJoyID: Int, pjc: Pointer?, cbjc: Int): Int
    }

}
