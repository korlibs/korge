package com.soywiz.korgw.jnrffi

/*
import com.soywiz.korgw.win32.Win32
import jnr.ffi.CallingConvention
import jnr.ffi.LibraryLoader
import jnr.ffi.Runtime
import jnr.ffi.Struct
import jnr.ffi.annotations.Delegate
import jnr.ffi.annotations.In
import jnr.ffi.annotations.StdCall
import jnr.ffi.types.size_t
import jnr.ffi.types.uintptr_t
import kotlin.reflect.KProperty


object HelloWorld {
    @JvmStatic
    fun main(args: Array<String>) {
        User32.apply {
            val windowClass = "KorgwWindowClass"
            val hInst = GetModuleHandleA(null)
            val wClass = WNDClassEx()

            wClass.hInstance = hInst
            wClass.lpfnWndProc = windProc
            wClass.lpszClassName = windowClass
            wClass.hCursor = LoadCursorA(0L, 32512); // IDC_ARROW
            val res = RegisterClassExA(wClass)
            println(res)
        }
        User32.MessageBoxA(0L, "Hello", "World", 0)
    }

    interface User32 {
        @StdCall
        //fun MessageBoxA(@Pinned @Out @Transient @uintptr_t hwnd: Long, lpText: String, lpCaption: String, uType: Int): Int
        fun MessageBoxA(@uintptr_t hwnd: Long, lpText: String, lpCaption: String, uType: Int): Int

        @StdCall
        fun RegisterClassExA(@In lpwcx: WNDClassEx): ATOM

        @StdCall
        @size_t
        fun GetModuleHandleA(lpModuleName: String?): HMODULE

        @StdCall  @size_t  fun LoadCursorA(hInstance: HINSTANCE, lpCursorName: String?): HCURSOR
        @StdCall  @size_t  fun LoadCursorA(hInstance: HINSTANCE, @size_t lpCursorName: Int): HCURSOR

        private object Lib {
            internal val lib = LibraryLoader.create(User32::class.java).load("user32")
        }

        companion object : User32 by Lib.lib {
            internal val runtime by lazy { Runtime.getRuntime(Lib.lib) }
        }
    }

    class Demo(runtime: Runtime) : Struct(runtime) {
        val tv_sec by int32_t()
    }

    class WNDClassEx() : Struct(User32.runtime) {
        var cbSize by u_int32_t()
        var style by u_int32_t()
        var lpfnWndProc: WNDPROC? = null
        var cbClsExtra by int32_t()
        var cbWndExtra by int32_t()
        var hInstance by ssize_t()
        var hIcon by ssize_t()
        var hCursor by ssize_t()
        var hbrBackground by ssize_t()
        var lpszMenuName by uintptr_t()
        var lpszClassName by UTFStringRef(Charsets.UTF_8)
        var hIconSm by ssize_t()
    }
}

private operator fun Struct.String.setValue(s: Struct, property: KProperty<*>, value: String) = this.set(value)
private operator fun Struct.String.getValue(s: Struct, property: KProperty<*>): String = this.get()
private operator fun Struct.IntegerAlias.getValue(s: Struct, property: KProperty<*>): Long = this.get()
private operator fun Struct.IntegerAlias.setValue(s: Struct, property: KProperty<*>, value: Long): Unit = this.set(value)

interface WNDPROC {
    @Delegate(convention = CallingConvention.STDCALL)
    operator fun invoke(
        hwnd: HWND,
        uMsg: UINT,
        @size_t wParam: Long,
        @size_t lParam: Long
    ): LRESULT
}

typealias UINT = Int
typealias LRESULT = Int
typealias ATOM = Int
typealias HWND = Long
typealias HMODULE = Long
typealias HINSTANCE = Long
typealias HCURSOR = Long

/*
                 | for handles     | for pointers  |
                 | and numbers     |               |
| OS             | WPARAM          | LPARAM        |
|----------------|-----------------|---------------|
| 16-bit Windows | 16-bit unsigned | 32-bit signed |
| 32-bit Windows | 32-bit unsigned | 32-bit signed |
| 64-bit Windows | 64-bit unsigned | 64-bit signed |
 */
*/
