package com.soywiz.korgw.win32

import com.soywiz.korev.Key
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.*
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.W32APIOptions
import java.nio.Buffer

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
    fun BringWindowToTop(hWnd: WinDef.HWND?): WinDef.HWND?
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
    set(value) = run { right = left + value }
    get() = right - left

var WinDef.RECT.height: Int
    set(value) = run { bottom = top + value }
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
internal const val VK_LBUTTON = 0x01  /* Left mouse button */
internal const val VK_RBUTTON = 0x02  /* Right mouse button */
internal const val VK_CANCEL = 0x03   /* Control-break processing */
internal const val VK_MBUTTON = 0x04  /* Middle mouse button (three-button mouse) */
internal const val VK_XBUTTON1 = 0x05 /* Windows 2000/XP: X1 mouse button */
internal const val VK_XBUTTON2 = 0x06 /* Windows 2000/XP: X2 mouse button */
internal const val VK_BACK = 0x08 /* BACKSPACE key */
internal const val VK_TAB = 0x09  /* TAB key */
internal const val VK_CLEAR = 0x0C  /* CLEAR key */
internal const val VK_RETURN = 0x0D /* ENTER key */
internal const val VK_SHIFT = 0x10   /* SHIFT key */
internal const val VK_CONTROL = 0x11 /* CTRL key */
internal const val VK_MENU = 0x12    /* ALT key */
internal const val VK_PAUSE = 0x13   /* PAUSE key */
internal const val VK_CAPITAL = 0x14 /* CAPS LOCK key */
internal const val VK_KANA = 0x15    /* Input Method Editor (IME) Kana mode */
internal const val VK_HANGUEL = 0x15               /* IME Hanguel mode (maintained for compatibility; use #define VK_HANGUL) */
internal const val VK_HANGUL = 0x15 /* IME Hangul mode */
internal const val VK_JUNJA = 0x17 /* IME Junja mode */
internal const val VK_FINAL = 0x18 /* IME final mode */
internal const val VK_HANJA = 0x19 /* IME Hanja mode */
internal const val VK_KANJI = 0x19 /* IME Kanji mode */
internal const val VK_HKTG = 0x1A       /* Hiragana/Katakana toggle */
internal const val VK_ESCAPE = 0x1B     /* ESC key */
internal const val VK_CONVERT = 0x1C    /* IME convert */
internal const val VK_NONCONVERT = 0x1D /* IME nonconvert */
internal const val VK_ACCEPT = 0x1E     /* IME accept */
internal const val VK_MODECHANGE = 0x1F /* IME mode change request */
internal const val VK_SPACE = 0x20    /* SPACEBAR */
internal const val VK_PRIOR = 0x21    /* PAGE UP key */
internal const val VK_NEXT = 0x22     /* PAGE DOWN key */
internal const val VK_END = 0x23      /* END key */
internal const val VK_HOME = 0x24     /* HOME key */
internal const val VK_LEFT = 0x25     /* LEFT ARROW key */
internal const val VK_UP = 0x26       /* UP ARROW key */
internal const val VK_RIGHT = 0x27    /* RIGHT ARROW key */
internal const val VK_DOWN = 0x28     /* DOWN ARROW key */
internal const val VK_SELECT = 0x29   /* SELECT key */
internal const val VK_PRINT = 0x2A    /* PRINT key */
internal const val VK_EXECUTE = 0x2B  /* EXECUTE key */
internal const val VK_SNAPSHOT = 0x2C /* PRINT SCREEN key */
internal const val VK_INSERT = 0x2D   /* INS key */
internal const val VK_DELETE = 0x2E   /* DEL key */
internal const val VK_HELP = 0x2F     /* HELP key */
internal const val VK_KEY_0 = 0x30 /* '0' key */
internal const val VK_KEY_1 = 0x31 /* '1' key */
internal const val VK_KEY_2 = 0x32 /* '2' key */
internal const val VK_KEY_3 = 0x33 /* '3' key */
internal const val VK_KEY_4 = 0x34 /* '4' key */
internal const val VK_KEY_5 = 0x35 /* '5' key */
internal const val VK_KEY_6 = 0x36 /* '6' key */
internal const val VK_KEY_7 = 0x37 /* '7' key */
internal const val VK_KEY_8 = 0x38 /* '8' key */
internal const val VK_KEY_9 = 0x39 /* '9' key */
internal const val VK_KEY_A = 0x41 /* 'A' key */
internal const val VK_KEY_B = 0x42 /* 'B' key */
internal const val VK_KEY_C = 0x43 /* 'C' key */
internal const val VK_KEY_D = 0x44 /* 'D' key */
internal const val VK_KEY_E = 0x45 /* 'E' key */
internal const val VK_KEY_F = 0x46 /* 'F' key */
internal const val VK_KEY_G = 0x47 /* 'G' key */
internal const val VK_KEY_H = 0x48 /* 'H' key */
internal const val VK_KEY_I = 0x49 /* 'I' key */
internal const val VK_KEY_J = 0x4A /* 'J' key */
internal const val VK_KEY_K = 0x4B /* 'K' key */
internal const val VK_KEY_L = 0x4C /* 'L' key */
internal const val VK_KEY_M = 0x4D /* 'M' key */
internal const val VK_KEY_N = 0x4E /* 'N' key */
internal const val VK_KEY_O = 0x4F /* 'O' key */
internal const val VK_KEY_P = 0x50 /* 'P' key */
internal const val VK_KEY_Q = 0x51 /* 'Q' key */
internal const val VK_KEY_R = 0x52 /* 'R' key */
internal const val VK_KEY_S = 0x53 /* 'S' key */
internal const val VK_KEY_T = 0x54 /* 'T' key */
internal const val VK_KEY_U = 0x55 /* 'U' key */
internal const val VK_KEY_V = 0x56 /* 'V' key */
internal const val VK_KEY_W = 0x57 /* 'W' key */
internal const val VK_KEY_X = 0x58 /* 'X' key */
internal const val VK_KEY_Y = 0x59 /* 'Y' key */
internal const val VK_KEY_Z = 0x5A /* 'Z' key */
internal const val VK_LWIN = 0x5B /* Left Windows key (Microsoft Natural keyboard) */
internal const val VK_RWIN = 0x5C /* Right Windows key (Natural keyboard) */
internal const val VK_APPS = 0x5D /* Applications key (Natural keyboard) */
internal const val VK_POWER = 0x5E /* Power key */
internal const val VK_SLEEP = 0x5F /* Computer Sleep key */
internal const val VK_NUMPAD0 = 0x60 /* Numeric keypad '0' key */
internal const val VK_NUMPAD1 = 0x61 /* Numeric keypad '1' key */
internal const val VK_NUMPAD2 = 0x62 /* Numeric keypad '2' key */
internal const val VK_NUMPAD3 = 0x63 /* Numeric keypad '3' key */
internal const val VK_NUMPAD4 = 0x64 /* Numeric keypad '4' key */
internal const val VK_NUMPAD5 = 0x65 /* Numeric keypad '5' key */
internal const val VK_NUMPAD6 = 0x66 /* Numeric keypad '6' key */
internal const val VK_NUMPAD7 = 0x67 /* Numeric keypad '7' key */
internal const val VK_NUMPAD8 = 0x68 /* Numeric keypad '8' key */
internal const val VK_NUMPAD9 = 0x69 /* Numeric keypad '9' key */
internal const val VK_MULTIPLY = 0x6A  /* Multiply key */
internal const val VK_ADD = 0x6B       /* Add key */
internal const val VK_SEPARATOR = 0x6C /* Separator key */
internal const val VK_SUBTRACT = 0x6D  /* Subtract key */
internal const val VK_DECIMAL = 0x6E   /* Decimal key */
internal const val VK_DIVIDE = 0x6F    /* Divide key */
internal const val VK_F1 = 0x70  /* F1 key */
internal const val VK_F2 = 0x71  /* F2 key */
internal const val VK_F3 = 0x72  /* F3 key */
internal const val VK_F4 = 0x73  /* F4 key */
internal const val VK_F5 = 0x74  /* F5 key */
internal const val VK_F6 = 0x75  /* F6 key */
internal const val VK_F7 = 0x76  /* F7 key */
internal const val VK_F8 = 0x77  /* F8 key */
internal const val VK_F9 = 0x78  /* F9 key */
internal const val VK_F10 = 0x79 /* F10 key */
internal const val VK_F11 = 0x7A /* F11 key */
internal const val VK_F12 = 0x7B /* F12 key */
internal const val VK_F13 = 0x7C /* F13 key */
internal const val VK_F14 = 0x7D /* F14 key */
internal const val VK_F15 = 0x7E /* F15 key */
internal const val VK_F16 = 0x7F /* F16 key */
internal const val VK_F17 = 0x80 /* F17 key */
internal const val VK_F18 = 0x81 /* F18 key */
internal const val VK_F19 = 0x82 /* F19 key */
internal const val VK_F20 = 0x83 /* F20 key */
internal const val VK_F21 = 0x84 /* F21 key */
internal const val VK_F22 = 0x85 /* F22 key */
internal const val VK_F23 = 0x86 /* F23 key */
internal const val VK_F24 = 0x87 /* F24 key */
internal const val VK_NUMLOCK = 0x90 /* NUM LOCK key */
internal const val VK_SCROLL = 0x91  /* SCROLL LOCK key */
internal const val VK_LSHIFT = 0xA0   /* Left SHIFT key */
internal const val VK_RSHIFT = 0xA1   /* Right SHIFT key */
internal const val VK_LCONTROL = 0xA2 /* Left CONTROL key */
internal const val VK_RCONTROL = 0xA3 /* Right CONTROL key */
internal const val VK_LMENU = 0xA4    /* Left MENU key */
internal const val VK_RMENU = 0xA5    /* Right MENU key */
internal const val VK_BROWSER_BACK = 0xA6      /* Windows 2000/XP: Browser Back key */
internal const val VK_BROWSER_FORWARD = 0xA7   /* Windows 2000/XP: Browser Forward key */
internal const val VK_BROWSER_REFRESH = 0xA8   /* Windows 2000/XP: Browser Refresh key */
internal const val VK_BROWSER_STOP = 0xA9      /* Windows 2000/XP: Browser Stop key */
internal const val VK_BROWSER_SEARCH = 0xAA    /* Windows 2000/XP: Browser Search key */
internal const val VK_BROWSER_FAVORITES = 0xAB /* Windows 2000/XP: Browser Favorites key */
internal const val VK_BROWSER_HOME = 0xAC      /* Windows 2000/XP: Browser Start and Home key */
internal const val VK_VOLUME_MUTE = 0xAD /* Windows 2000/XP: Volume Mute key */
internal const val VK_VOLUME_DOWN = 0xAE /* Windows 2000/XP: Volume Down key */
internal const val VK_VOLUME_UP = 0xAF   /* Windows 2000/XP: Volume Up key */
internal const val VK_MEDIA_NEXT_TRACK = 0xB0 /* Windows 2000/XP: Next Track key */
internal const val VK_MEDIA_PREV_TRACK = 0xB1 /* Windows 2000/XP: Previous Track key */
internal const val VK_MEDIA_STOP = 0xB2       /* Windows 2000/XP: Stop Media key */
internal const val VK_MEDIA_PLAY_PAUSE = 0xB3 /* Windows 2000/XP: Play/Pause Media key */
internal const val VK_LAUNCH_MAIL = 0xB4         /* Windows 2000/XP: Start Mail key */
internal const val VK_MEDIA_SELECT = 0xB5        /* Windows 2000/XP: Select Media key */
internal const val VK_LAUNCH_MEDIA_SELECT = 0xB5 /* Windows 2000/XP: Select Media key */
internal const val VK_LAUNCH_APP1 = 0xB6         /* Windows 2000/XP: Start Application 1 key */
internal const val VK_LAUNCH_APP2 = 0xB7         /* Windows 2000/XP: Start Application 2 key */
internal const val VK_OEM_1 = 0xBA /* Used for miscellaneous characters; it can vary by keyboard. */
internal const val VK_OEM_PLUS = 0xBB   /* Windows 2000/XP: For any country/region, the '+' key */
internal const val VK_OEM_COMMA = 0xBC  /* Windows 2000/XP: For any country/region, the ',' key */
internal const val VK_OEM_MINUS = 0xBD  /* Windows 2000/XP: For any country/region, the '-' key */
internal const val VK_OEM_PERIOD = 0xBE /* Windows 2000/XP: For any country/region, the '.' key */
internal const val VK_OEM_2 = 0xBF /* Used for miscellaneous characters; it can vary by keyboard. */
internal const val VK_OEM_3 = 0xC0 /* Used for miscellaneous characters; it can vary by keyboard. */
internal const val VK_ABNT_C1 = 0xC1 /* Brazilian (ABNT) Keyboard */
internal const val VK_ABNT_C2 = 0xC2 /* Brazilian (ABNT) Keyboard */
internal const val VK_OEM_4 = 0xDB /* Used for miscellaneous characters; it can vary by keyboard. */
internal const val VK_OEM_5 = 0xDC /* Used for miscellaneous characters; it can vary by keyboard. */
internal const val VK_OEM_6 = 0xDD /* Used for miscellaneous characters; it can vary by keyboard. */
internal const val VK_OEM_7 = 0xDE /* Used for miscellaneous characters; it can vary by keyboard. */
internal const val VK_OEM_8 = 0xDF /* Used for miscellaneous characters; it can vary by keyboard. */
internal const val VK_OEM_AX = 0xE1 /* AX key on Japanese AX keyboard */
internal const val VK_OEM_102 = 0xE2 /* Windows 2000/XP: Either the angle bracket key or */
internal const val VK_PROCESSKEY = 0xE5 /* Windows 95/98/Me, Windows NT 4.0, Windows 2000/XP: IME PROCESS key */
internal const val VK_PACKET = 0xE7 /* Windows 2000/XP: Used to pass Unicode characters as if they were keystrokes. */
internal const val VK_OEM_RESET = 0xE9
internal const val VK_OEM_JUMP = 0xEA
internal const val VK_OEM_PA1 = 0xEB
internal const val VK_OEM_PA2 = 0xEC
internal const val VK_OEM_PA3 = 0xED
internal const val VK_OEM_WSCTRL = 0xEE
internal const val VK_OEM_CUSEL = 0xEF
internal const val VK_OEM_ATTN = 0xF0
internal const val VK_OEM_FINISH = 0xF1
internal const val VK_OEM_COPY = 0xF2
internal const val VK_OEM_AUTO = 0xF3
internal const val VK_OEM_ENLW = 0xF4
internal const val VK_OEM_BACKTAB = 0xF5
internal const val VK_ATTN = 0xF6      /* Attn key */
internal const val VK_CRSEL = 0xF7     /* CrSel key */
internal const val VK_EXSEL = 0xF8     /* ExSel key */
internal const val VK_EREOF = 0xF9     /* Erase EOF key */
internal const val VK_PLAY = 0xFA      /* Play key */
internal const val VK_ZOOM = 0xFB      /* Zoom key */
internal const val VK_NONAME = 0xFC    /* Reserved */
internal const val VK_PA1 = 0xFD       /* PA1 key */
internal const val VK_OEM_CLEAR = 0xFE /* Clear key */
internal const val VK_NONE = 0xFF /* no key */

val VK_TABLE: Map<Int, Key> = mapOf(
    KBDEXT to Key.UNKNOWN,
    KBDMULTIVK to Key.UNKNOWN,
    KBDSPECIAL to Key.UNKNOWN,
    KBDNUMPAD to Key.UNKNOWN,
    KBDUNICODE to Key.UNKNOWN,
    KBDINJECTEDVK to Key.UNKNOWN,
    KBDMAPPEDVK to Key.UNKNOWN,
    KBDBREAK to Key.UNKNOWN,
    VK_LBUTTON to Key.UNKNOWN,
    VK_RBUTTON to Key.UNKNOWN,
    VK_CANCEL to Key.UNKNOWN,
    VK_MBUTTON to Key.UNKNOWN,
    VK_XBUTTON1 to Key.UNKNOWN,
    VK_XBUTTON2 to Key.UNKNOWN,
    VK_BACK to Key.BACKSPACE,
    VK_TAB to Key.TAB,
    VK_CLEAR to Key.CLEAR,
    VK_RETURN to Key.RETURN,
    VK_SHIFT to Key.LEFT_SHIFT,
    VK_CONTROL to Key.LEFT_CONTROL,
    VK_MENU to Key.MENU,
    VK_PAUSE to Key.PAUSE,
    VK_CAPITAL to Key.UNKNOWN,
    VK_KANA to Key.UNKNOWN,
    VK_HANGUEL to Key.UNKNOWN,
    VK_HANGUL to Key.UNKNOWN,
    VK_JUNJA to Key.UNKNOWN,
    VK_FINAL to Key.UNKNOWN,
    VK_HANJA to Key.UNKNOWN,
    VK_KANJI to Key.UNKNOWN,
    VK_HKTG to Key.UNKNOWN,
    VK_ESCAPE to Key.ESCAPE,
    VK_CONVERT to Key.UNKNOWN,
    VK_NONCONVERT to Key.UNKNOWN,
    VK_ACCEPT to Key.UNKNOWN,
    VK_MODECHANGE to Key.UNKNOWN,
    VK_SPACE to Key.SPACE,
    VK_PRIOR to Key.UNKNOWN,
    VK_NEXT to Key.UNKNOWN,
    VK_END to Key.END,
    VK_HOME to Key.HOME,
    VK_LEFT to Key.LEFT,
    VK_UP to Key.UP,
    VK_RIGHT to Key.RIGHT,
    VK_DOWN to Key.DOWN,
    VK_SELECT to Key.SELECT_KEY,
    VK_PRINT to Key.PRINT_SCREEN,
    VK_EXECUTE to Key.UNKNOWN,
    VK_SNAPSHOT to Key.UNKNOWN,
    VK_INSERT to Key.INSERT,
    VK_DELETE to Key.DELETE,
    VK_HELP to Key.HELP,
    VK_KEY_0 to Key.N0,
    VK_KEY_1 to Key.N1,
    VK_KEY_2 to Key.N2,
    VK_KEY_3 to Key.N3,
    VK_KEY_4 to Key.N4,
    VK_KEY_5 to Key.N5,
    VK_KEY_6 to Key.N6,
    VK_KEY_7 to Key.N7,
    VK_KEY_8 to Key.N8,
    VK_KEY_9 to Key.N9,
    VK_KEY_A to Key.A,
    VK_KEY_B to Key.B,
    VK_KEY_C to Key.C,
    VK_KEY_D to Key.D,
    VK_KEY_E to Key.E,
    VK_KEY_F to Key.F,
    VK_KEY_G to Key.G,
    VK_KEY_H to Key.H,
    VK_KEY_I to Key.I,
    VK_KEY_J to Key.J,
    VK_KEY_K to Key.K,
    VK_KEY_L to Key.L,
    VK_KEY_M to Key.M,
    VK_KEY_N to Key.N,
    VK_KEY_O to Key.O,
    VK_KEY_P to Key.P,
    VK_KEY_Q to Key.Q,
    VK_KEY_R to Key.R,
    VK_KEY_S to Key.S,
    VK_KEY_T to Key.T,
    VK_KEY_U to Key.U,
    VK_KEY_V to Key.V,
    VK_KEY_W to Key.W,
    VK_KEY_X to Key.X,
    VK_KEY_Y to Key.Y,
    VK_KEY_Z to Key.Z,
    VK_LWIN to Key.META,
    VK_RWIN to Key.META,
    VK_APPS to Key.UNKNOWN,
    VK_POWER to Key.UNKNOWN,
    VK_SLEEP to Key.UNKNOWN,
    VK_NUMPAD0 to Key.NUMPAD0,
    VK_NUMPAD1 to Key.NUMPAD1,
    VK_NUMPAD2 to Key.NUMPAD2,
    VK_NUMPAD3 to Key.NUMPAD3,
    VK_NUMPAD4 to Key.NUMPAD4,
    VK_NUMPAD5 to Key.NUMPAD5,
    VK_NUMPAD6 to Key.NUMPAD6,
    VK_NUMPAD7 to Key.NUMPAD7,
    VK_NUMPAD8 to Key.NUMPAD8,
    VK_NUMPAD9 to Key.NUMPAD9,
    VK_MULTIPLY to Key.KP_MULTIPLY,
    VK_ADD to Key.KP_ADD,
    VK_SEPARATOR to Key.KP_SEPARATOR,
    VK_SUBTRACT to Key.KP_SUBTRACT,
    VK_DECIMAL to Key.KP_DECIMAL,
    VK_DIVIDE to Key.KP_DIVIDE,
    VK_F1 to Key.F1,
    VK_F2 to Key.F2,
    VK_F3 to Key.F3,
    VK_F4 to Key.F4,
    VK_F5 to Key.F5,
    VK_F6 to Key.F6,
    VK_F7 to Key.F7,
    VK_F8 to Key.F8,
    VK_F9 to Key.F9,
    VK_F10 to Key.F10,
    VK_F11 to Key.F11,
    VK_F12 to Key.F12,
    VK_F13 to Key.F13,
    VK_F14 to Key.F14,
    VK_F15 to Key.F15,
    VK_F16 to Key.F16,
    VK_F17 to Key.F17,
    VK_F18 to Key.F18,
    VK_F19 to Key.F19,
    VK_F20 to Key.F20,
    VK_F21 to Key.F21,
    VK_F22 to Key.F22,
    VK_F23 to Key.F23,
    VK_F24 to Key.F24,
    VK_NUMLOCK to Key.NUM_LOCK,
    VK_SCROLL to Key.SCROLL_LOCK,
    VK_LSHIFT to Key.LEFT_SHIFT,
    VK_RSHIFT to Key.RIGHT_SHIFT,
    VK_LCONTROL to Key.LEFT_CONTROL,
    VK_RCONTROL to Key.RIGHT_CONTROL,
    VK_LMENU to Key.LEFT_SUPER,
    VK_RMENU to Key.RIGHT_SUPER,
    VK_BROWSER_BACK to Key.UNKNOWN,
    VK_BROWSER_FORWARD to Key.UNKNOWN,
    VK_BROWSER_REFRESH to Key.UNKNOWN,
    VK_BROWSER_STOP to Key.UNKNOWN,
    VK_BROWSER_SEARCH to Key.UNKNOWN,
    VK_BROWSER_FAVORITES to Key.UNKNOWN,
    VK_BROWSER_HOME to Key.UNKNOWN,
    VK_VOLUME_MUTE to Key.UNKNOWN,
    VK_VOLUME_DOWN to Key.UNKNOWN,
    VK_VOLUME_UP to Key.UNKNOWN,
    VK_MEDIA_NEXT_TRACK to Key.UNKNOWN,
    VK_MEDIA_PREV_TRACK to Key.UNKNOWN,
    VK_MEDIA_STOP to Key.UNKNOWN,
    VK_MEDIA_PLAY_PAUSE to Key.UNKNOWN,
    VK_LAUNCH_MAIL to Key.UNKNOWN,
    VK_MEDIA_SELECT to Key.UNKNOWN,
    VK_LAUNCH_MEDIA_SELECT to Key.UNKNOWN,
    VK_LAUNCH_APP1 to Key.UNKNOWN,
    VK_LAUNCH_APP2 to Key.UNKNOWN,
    VK_OEM_1 to Key.UNKNOWN,
    VK_OEM_PLUS to Key.PLUS,
    VK_OEM_COMMA to Key.UNKNOWN,
    VK_OEM_MINUS to Key.MINUS,
    VK_OEM_PERIOD to Key.UNKNOWN,
    VK_OEM_2 to Key.UNKNOWN,
    VK_OEM_3 to Key.UNKNOWN,
    VK_ABNT_C1 to Key.UNKNOWN,
    VK_ABNT_C2 to Key.UNKNOWN,
    VK_OEM_4 to Key.UNKNOWN,
    VK_OEM_5 to Key.UNKNOWN,
    VK_OEM_6 to Key.UNKNOWN,
    VK_OEM_7 to Key.UNKNOWN,
    VK_OEM_8 to Key.UNKNOWN,
    VK_OEM_AX to Key.UNKNOWN,
    VK_OEM_102 to Key.UNKNOWN,
    VK_PROCESSKEY to Key.UNKNOWN,
    VK_PACKET to Key.UNKNOWN,
    VK_OEM_RESET to Key.UNKNOWN,
    VK_OEM_JUMP to Key.UNKNOWN,
    VK_OEM_PA1 to Key.UNKNOWN,
    VK_OEM_PA2 to Key.UNKNOWN,
    VK_OEM_PA3 to Key.UNKNOWN,
    VK_OEM_WSCTRL to Key.UNKNOWN,
    VK_OEM_CUSEL to Key.UNKNOWN,
    VK_OEM_ATTN to Key.UNKNOWN,
    VK_OEM_FINISH to Key.UNKNOWN,
    VK_OEM_COPY to Key.UNKNOWN,
    VK_OEM_AUTO to Key.UNKNOWN,
    VK_OEM_ENLW to Key.UNKNOWN,
    VK_OEM_BACKTAB to Key.UNKNOWN,
    VK_ATTN to Key.UNKNOWN,
    VK_CRSEL to Key.UNKNOWN,
    VK_EXSEL to Key.UNKNOWN,
    VK_EREOF to Key.UNKNOWN,
    VK_PLAY to Key.UNKNOWN,
    VK_ZOOM to Key.UNKNOWN,
    VK_NONAME to Key.UNKNOWN,
    VK_PA1 to Key.UNKNOWN,
    VK_OEM_CLEAR to Key.UNKNOWN,
    VK_NONE to Key.UNKNOWN
)
