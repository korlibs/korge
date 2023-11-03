package korlibs.render.win32

import com.sun.jna.*
import com.sun.jna.platform.win32.*
import com.sun.jna.ptr.*
import com.sun.jna.win32.*
import java.nio.*

object Win32 : MyKernel32 by MyKernel32,
    MyUser32 by MyUser32,
    MyGdi32 by MyGdi32,
    MyOpenGL32 by MyOpenGL32 {
}

interface MyKernel32 : Kernel32 {
    companion object : MyKernel32 by Native.load("kernel32", MyKernel32::class.java, W32APIOptions.DEFAULT_OPTIONS)
}

internal const val WM_SETICON = 0x0080

interface MyUser32 : User32 {
    fun SetWindowText(hWnd: WinDef.HWND?, lpString: String?): Boolean
    fun SetActiveWindow(hWnd: WinDef.HWND?): WinDef.HWND?
    fun MessageBox(hWnd: WinDef.HWND?, text: String, caption: String, type: Int): Int
    fun LoadCursor(hInstance: WinDef.HINSTANCE?, lpCursorName: String?): WinDef.HCURSOR?
    fun LoadCursor(hInstance: WinDef.HINSTANCE?, lpCursorName: Int): WinDef.HCURSOR?
    fun AllowSetForegroundWindow(dwProcessId: Int): Boolean
    fun CreateIconIndirect(piconinfo: WinGDI.ICONINFO): WinDef.HICON


    companion object : MyUser32 by Native.load("user32", MyUser32::class.java, W32APIOptions.DEFAULT_OPTIONS)
}

interface MyGdi32 : GDI32 {
    fun SwapBuffers(Arg1: WinDef.HDC?): WinDef.BOOL?
    fun CreateBitmap(
        nWidth: Int,
        nHeight: Int,
        nPlanes: Int,
        nBitCount: Int,
        buffer: Buffer?
    ): WinDef.HBITMAP

    fun CreateDIBSection(hDC: WinDef.HDC?, pbmi: BITMAPV5HEADER?, iUsage: Int, ppvBits: PointerByReference?, hSection: Pointer?, dwOffset: Int): WinDef.HBITMAP?
    fun DescribePixelFormat(
        hdc: WinDef.HDC?,
        iPixelFormat: Int,
        nBytes: Int,
        ppfd: WinGDI.PIXELFORMATDESCRIPTOR.ByReference?
    ): Int
    companion object : MyGdi32 by Native.load("gdi32", MyGdi32::class.java, W32APIOptions.DEFAULT_OPTIONS)
}

@Structure.FieldOrder("ciexyzX", "ciexyzY", "ciexyzZ")
class CIEXYZ : Structure() {
    @JvmField
    var ciexyzX: Int = 0
    @JvmField
    var ciexyzY: Int = 0
    @JvmField
    var ciexyzZ: Int = 0
}

@Structure.FieldOrder("ciexyzRed", "ciexyzGreen", "ciexyzBlue")
class CIEXYZTRIPLE : Structure() {
    @JvmField
    var ciexyzRed = CIEXYZ()
    @JvmField
    var ciexyzGreen = CIEXYZ()
    @JvmField
    var ciexyzBlue = CIEXYZ()
}

@Structure.FieldOrder(
    "bV5Size",
    "bV5Width",
    "bV5Height",
    "bV5Planes",
    "bV5BitCount",
    "bV5Compression",
    "bV5SizeImage",
    "bV5XPelsPerMeter",
    "bV5YPelsPerMeter",
    "bV5ClrUsed",
    "bV5ClrImportant",
    "bV5RedMask",
    "bV5GreenMask",
    "bV5BlueMask",
    "bV5AlphaMask",
    "bV5CSType",
    "bV5Endpoints",
    "bV5GammaRed",
    "bV5GammaGreen",
    "bV5GammaBlue",
    "bV5Intent",
    "bV5ProfileData",
    "bV5ProfileSize",
    "bV5Reserved"
)
class BITMAPV5HEADER : Structure() {
    @JvmField var bV5Size = size()
    @JvmField var bV5Width = 0
    @JvmField var bV5Height = 0
    @JvmField var bV5Planes: Short = 0
    @JvmField var bV5BitCount: Short = 0
    @JvmField var bV5Compression = 0
    @JvmField var bV5SizeImage = 0
    @JvmField var bV5XPelsPerMeter = 0
    @JvmField var bV5YPelsPerMeter = 0
    @JvmField var bV5ClrUsed = 0
    @JvmField var bV5ClrImportant = 0
    // V4
    @JvmField var bV5RedMask: Int = 0
    @JvmField var bV5GreenMask: Int = 0
    @JvmField var bV5BlueMask: Int = 0
    @JvmField var bV5AlphaMask: Int = 0
    @JvmField var bV5CSType: Int = 0
    @JvmField var bV5Endpoints: CIEXYZTRIPLE = CIEXYZTRIPLE()
    @JvmField var bV5GammaRed: Int = 0
    @JvmField var bV5GammaGreen: Int = 0
    @JvmField var bV5GammaBlue: Int = 0
    // V5
    @JvmField var bV5Intent: Int = 0
    @JvmField var bV5ProfileData: Int = 0
    @JvmField var bV5ProfileSize: Int = 0
    @JvmField var bV5Reserved: Int = 0
}

interface MyOpenGL32 : OpenGL32 {
    fun glClearColor(r: Float, g: Float, b: Float, a: Float)
    fun glClear(mask: Int)
    fun glFlush()
    fun glViewport(x: Int, y: Int, width: Int, height: Int)
    //fun glGetString(name: Int): String

    companion object : MyOpenGL32 by Native.load("opengl32", MyOpenGL32::class.java) {
        val INSTANCE = MyOpenGL32

        const val GL_DEPTH_BUFFER_BIT = 0x00000100
        const val GL_STENCIL_BUFFER_BIT = 0x00000400
        const val GL_COLOR_BUFFER_BIT = 0x00004000

        const val WGL_CONTEXT_MAJOR_VERSION_ARB = 0x2091
        const val WGL_CONTEXT_MINOR_VERSION_ARB = 0x2092

        const val GL_VENDOR = 0x1F00
        const val GL_RENDERER = 0x1F01
        const val GL_VERSION = 0x1F02
        const val GL_SHADING_LANGUAGE_VERSION = 0x8B8C
        const val GL_EXTENSIONS = 0x1F03
    }
}

internal const val WS_EX_CLIENTEDGE = 0x200
internal const val WS_EX_TOPMOST = 0x00000008

var WinDef.RECT.width: Int
    set(value) { right = left + value }
    get() = right - left

var WinDef.RECT.height: Int
    set(value) { bottom = top + value }
    get() = bottom - top

internal const val WM_CAPTURECHANGED = 0x0215
internal const val WM_LBUTTONDBLCLK = 0x0203
internal const val WM_LBUTTONDOWN = 0x0201
internal const val WM_LBUTTONUP = 0x0202
internal const val WM_MBUTTONDBLCLK = 0x0209
internal const val WM_MBUTTONDOWN = 0x0207
internal const val WM_MBUTTONUP = 0x0208
internal const val WM_MOUSEACTIVATE = 0x0021
internal const val WM_MOUSEHOVER = 0x02A1
internal const val WM_MOUSEHWHEEL = 0x020E
internal const val WM_MOUSELEAVE = 0x02A3
internal const val WM_MOUSEMOVE = 0x0200
internal const val WM_MOUSEWHEEL = 0x020A
internal const val WM_RBUTTONDBLCLK = 0x0206
internal const val WM_RBUTTONDOWN = 0x0204
internal const val WM_RBUTTONUP = 0x0205
internal const val WM_XBUTTONDBLCLK = 0x020D
internal const val WM_XBUTTONDOWN = 0x020B
internal const val WM_XBUTTONUP = 0x020C
internal const val WM_SYSCHAR = 0x0106
internal const val KBDEXT = 0x0100
internal const val KBDMULTIVK = 0x0200
internal const val KBDSPECIAL = 0x0400
internal const val KBDNUMPAD = 0x0800
internal const val KBDUNICODE = 0x1000
internal const val KBDINJECTEDVK = 0x2000
internal const val KBDMAPPEDVK = 0x4000
internal const val KBDBREAK = 0x8000
