package com.soywiz.korgw

import com.soywiz.kgl.toInt
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.file.*
import com.soywiz.korio.net.*
import com.soywiz.korma.geom.*
import kotlinx.cinterop.*
import platform.opengl32.*
import platform.windows.*


//override val ag: AG = AGNative()

val windowsGameWindow: WindowsGameWindow = WindowsGameWindow()
actual fun CreateDefaultGameWindow(): GameWindow = windowsGameWindow

fun processString(maxLen: Int, callback: (ptr: CPointer<WCHARVar>, maxLen: Int) -> Unit): String {
    return memScoped {
        val ptr = allocArray<WCHARVar>(maxLen)
        callback(ptr, maxLen)
        ptr.toKString()
    }
}

private fun Bitmap32.toWin32Icon(): HICON? {
    val bmp = this.clone().flipY().toBMP32()
    memScoped {
        val bi = alloc<BITMAPV5HEADER>()
        bi.bV5Size = BITMAPV5HEADER.size.convert()
        bi.bV5Width = bmp.width
        bi.bV5Height = bmp.height
        bi.bV5Planes = 1.convert()
        bi.bV5BitCount = 32.convert()
        bi.bV5Compression = BI_BITFIELDS.convert()
        // The following mask specification specifies a supported 32 BPP
        // alpha format for Windows XP.
        bi.bV5RedMask = 0x00_00_00_FF.convert()
        bi.bV5GreenMask = 0x00_00_FF_00.convert()
        bi.bV5BlueMask = 0x00_FF_00_00.convert()
        bi.bV5AlphaMask = 0xFF_00_00_00.convert()

        val lpBits = alloc<COpaquePointerVar>()
        val hdc = GetDC(null)
        val hBitmap = CreateDIBSection(hdc, bi.ptr as CPointer<BITMAPINFO>, DIB_RGB_COLORS, lpBits.ptr, NULL, 0.convert())
        val memdc = CreateCompatibleDC(null)
        ReleaseDC(null, hdc);

        val bitsPtr = lpBits.reinterpret<CPointerVar<IntVar>>().value!!
        for (n in 0 until bmp.data.size) {
            bitsPtr[n] = bmp.data[n].value
        }

        // Create an empty mask bitmap.
        val hMonoBitmap = CreateBitmap(bmp.width, bmp.height, 1.convert(), 1.convert(), NULL)

        val ii = alloc<ICONINFO>()
        ii.fIcon = TRUE;  // Change fIcon to TRUE to create an alpha icon
        ii.xHotspot = 0.convert()
        ii.yHotspot = 0.convert()
        ii.hbmMask = hMonoBitmap
        ii.hbmColor = hBitmap
        val icon = CreateIconIndirect(ii.ptr)

        DeleteDC(memdc)
        DeleteObject(hBitmap)
        DeleteObject(hMonoBitmap)

        return icon
    }
}

private fun Bitmap32.scaled(width: Int, height: Int): Bitmap32 {
    val scaleX = width.toDouble() / this.width.toDouble()
    val scaleY = height.toDouble() / this.height.toDouble()
    return scaleLinear(scaleX, scaleY)
}

@ThreadLocal
var setSwapInterval = false
@ThreadLocal
var swapIntervalEXT: CPointer<CFunction<(Int) -> Unit>>? = null

class WindowsGameWindow : EventLoopGameWindow() {
    val agNativeComponent = Any()
    var hwnd: HWND? = null
    var glRenderContext: HGLRC? = null
    override val ag: AG = AGOpenglFactory.create(agNativeComponent).create(agNativeComponent, AGConfig())

    override var title: String
        get() = if (hwnd != null) processString(4096) { ptr, len -> GetWindowTextW(hwnd, ptr, len) } else lastTitle
        set(value) {
            lastTitle = value
            if (hwnd != null) SetWindowTextW(hwnd, value)
        }
    override val width: Int get() = getClientDim(height = false)
    override val height: Int get() = getClientDim(height = true)

    private fun getClientDim(height: Boolean): Int {
        return memScoped {
            if (hwnd != null) {
                val rect = alloc<RECT>()
                GetClientRect(hwnd, rect.ptr)
                if (height) rect.height else rect.width
            } else {
                return if (height) lastHeight else lastWidth
            }
        }
    }

    override var icon: Bitmap? = null
        set(value) {
            field = value
            _setIcon()
        }

    private fun _setIcon(bmp: Bitmap? = this.icon) {
        if (bmp != null && hwnd != null) {
            SendMessageA(hwnd, WM_SETICON.convert(), ICON_BIG.convert(), bmp.toBMP32().scaled(32, 32).toWin32Icon().toLong().convert())
            SendMessageA(hwnd, WM_SETICON.convert(), ICON_SMALL.convert(), bmp.toBMP32().scaled(16, 16).toWin32Icon().toLong().convert())
        }
    }

    private var fsX = 0
    private var fsY = 0
    private var fsW = 128
    private var fsH = 128
    private var lastFullScreen: Boolean? = null
    override var fullscreen: Boolean
        get() = if (hwnd != null) GetWindowLongPtrA(hwnd, GWL_STYLE.convert()).toLong().hasBits(WS_POPUP.toLong()) else lastFullScreen ?: false
        set(value) {
            lastFullScreen = value
            if (hwnd == null) return
            if (fullscreen == value) return
            memScoped {
                val style = GetWindowLongPtrA(hwnd, GWL_STYLE.convert())
                if (value) {
                    val rect = alloc<RECT>()
                    GetWindowRect(hwnd, rect.ptr)
                    fsX = rect.left
                    fsY = rect.top
                    fsW = rect.width
                    fsH = rect.height

                    SetWindowLongPtrA(hwnd, GWL_STYLE.convert(), getWinStyle(true, style).convert())
                    MoveWindow(hwnd, 0, 0, GetSystemMetrics(SM_CXSCREEN), GetSystemMetrics(SM_CYSCREEN), TRUE)
                    SetWindowLongPtrA(hwnd, GWL_EXSTYLE.convert(), getWinExStyle(true).convert())
                    //ShowWindow(hwnd, SW_MAXIMIZE)
                } else {
                    SetWindowLongPtrA(hwnd, GWL_STYLE.convert(), getWinStyle(false, style).convert())
                    MoveWindow(hwnd, fsX, fsY, fsW, fsH, TRUE)
                    SetWindowLongPtrA(hwnd, GWL_EXSTYLE.convert(), getWinExStyle(false).convert())
                    //ShowWindow(hwnd, SW_RESTORE)
                }
            }
        }

    override var visible: Boolean
        get() = IsWindowVisible(hwnd) != 0
        set(value) {
            ShowWindow(hwnd, if (value) SW_SHOW else SW_HIDE)
        }
    override var quality: Quality
        get() = super.quality
        set(value) {}

    var RECT.width
        set(value) = run { right = left + value }
        get() = right - left
    var RECT.height
        set(value) = run { bottom = top + value }
        get() = bottom - top

    private var lastTitle: String = ""
    private var lastWidth: Int = 100
    private var lastHeight: Int = 100

    override fun setSize(width: Int, height: Int): Unit = memScoped {
        lastWidth = width
        lastHeight = height
        if (hwnd != null) {
            val rect = alloc<RECT>()
            val borderSize = getBorderSize()
            GetWindowRect(hwnd, rect.ptr)
            //println("setSize: width=$width, height=$height")
            //println("RECT: ${rect.left}, ${rect.top}, ${rect.width}, ${rect.height}")
            //println("BORDER: ${borderSize.width}, ${borderSize.height}")
            MoveWindow(hwnd, rect.left, rect.top, width + borderSize.width, height + borderSize.height, true.toInt().convert())
        }
        Unit
    }

    override suspend fun browse(url: URL) {
        super.browse(url)
    }

    override suspend fun alert(message: String) {
        super.alert(message)
    }

    override suspend fun confirm(message: String): Boolean {
        return super.confirm(message)
    }

    override suspend fun prompt(message: String, default: String): String {
        return super.prompt(message, default)
    }

    override suspend fun openFileDialog(filter: FileFilter?, write: Boolean, multi: Boolean, currentDir: VfsFile?): List<VfsFile> {
        val selectedFile = openSelectFile(hwnd = hwnd)
        if (selectedFile != null) {
            return listOf(com.soywiz.korio.file.std.localVfs(selectedFile))
        } else {
            throw com.soywiz.korio.lang.CancelException()
        }
    }

    fun resized(width: Int, height: Int) {
        dispatchReshapeEvent(0, 0, width, height)
        render(doUpdate = false)
    }

    override fun doInitRender() {
        if (hwnd == null || glRenderContext == null) return
        val hdc = GetDC(hwnd)
        //println("render")
        wglMakeCurrent(hdc, glRenderContext)

        // https://github.com/spurious/SDL-mirror/blob/4c1c6d03ddaa3095b3c63c38ddd0a6cfad58b752/src/video/windows/SDL_windowsopengl.c#L439-L447
        if (!setSwapInterval) {
            setSwapInterval = true
            swapIntervalEXT = wglGetProcAddress("wglSwapIntervalEXT")?.reinterpret()
            println("swapIntervalEXT: $swapIntervalEXT")
        }
        if (vsync) {
            swapIntervalEXT?.invoke(1)
        } else {
            swapIntervalEXT?.invoke(0)
        }
    }

    override fun doSwapBuffers() {
        if (hwnd == null || glRenderContext == null) return
        val hdc = GetDC(hwnd)
        SwapBuffers(hdc)
    }

    override fun doDestroy() {
        //DestroyWindow(hwnd)
    }

    override fun doHandleEvents() {
        xInputEventAdapter.updateGamepadsWin32(this)
        memScoped {
            val msg = alloc<MSG>()
            while (
                PeekMessageW(
                    msg.ptr,
                    null,
                    0.convert(),
                    0.convert(),
                    PM_REMOVE.convert()
                ).toInt() != 0
            ) {
                TranslateMessage(msg.ptr)
                DispatchMessageW(msg.ptr)
                if (mustPerformRender()) break
            }
        }
    }

    override fun doInitialize() {
        memScoped {
            // https://www.khronos.org/opengl/wiki/Creating_an_OpenGL_Context_(WGL)

            val windowWidth = this@WindowsGameWindow.width
            val windowHeight = this@WindowsGameWindow.height

            val wc = alloc<WNDCLASSW>()

            val clazzName = "ogl_korge_kotlin_native"
            val clazzNamePtr = clazzName.wcstr.getPointer(this@memScoped)
            wc.lpfnWndProc = staticCFunction(::WndProc)
            wc.hInstance = null
            wc.hbrBackground = COLOR_BACKGROUND.toLong().toCPointer()

            val hInstance = GetModuleHandleA(null)
            //FindResourceA(null, null, 124)
            wc.hIcon = LoadIconAFunc(hInstance, 1000)
            //wc.hIcon = LoadIconAFunc(hInstance, 1)
            //wc.hIcon = LoadIconAFunc(hInstance, 32512)
            //wc.hIcon = LoadIconAFunc(null, 32512) // IDI_APPLICATION - MAKEINTRESOURCE(32512)

            wc.lpszClassName = clazzNamePtr.reinterpret()
            wc.style = CS_OWNDC.convert()
            if (RegisterClassW(wc.ptr).toInt() == 0) {
                return
            }

            val screenWidth = GetSystemMetrics(SM_CXSCREEN)
            val screenHeight = GetSystemMetrics(SM_CYSCREEN)

            val realSize = getRealSize(windowWidth, windowHeight)
            val realWidth = realSize.width//.clamp(0, screenWidth)
            val realHeight = realSize.height//.clamp(0, screenHeight)

            //println("Initial window size: $windowWidth, $windowHeight")

            fsX = ((screenWidth - realWidth) / 2).clamp(0, screenWidth - 16)
            fsY = ((screenHeight - realHeight) / 2).clamp(0, realHeight - 16)
            fsW = realWidth
            fsH = realHeight

            //val initialFullScreen = lastFullScreen
            val initialFullScreen = false

            hwnd = CreateWindowExW(
                getWinExStyle(initialFullScreen).convert(),
                clazzName,
                title,
                getWinStyle(initialFullScreen).convert(),
                if (initialFullScreen) 0 else fsX.convert(),
                if (initialFullScreen) 0 else fsY.convert(),
                if (initialFullScreen) screenWidth else fsW.convert(),
                if (initialFullScreen) screenHeight else fsH.convert(),
                null, null, null, null
            )
            println("ERROR: " + GetLastError())

            _setIcon()
            ShowWindow(hwnd, SW_SHOWNORMAL.convert())
            if (lastFullScreen != null) {
                fullscreen = lastFullScreen!!
            }

            //SetTimer(hwnd, 1, 1000 / 60, staticCFunction(::WndTimer))
        }
    }

    private val hasMenu = false
    private val winStyle: Long get() = getWinStyle(fullscreen)
    private val winExStyle: Long get() = getWinExStyle(fullscreen)

    fun getWinStyle(fullscreen: Boolean, extra: Long = 0): Long = if (fullscreen) extra.without(WS_OVERLAPPEDWINDOW.toLong()).with(WS_POPUP.toLong()) else extra.with(WS_OVERLAPPEDWINDOW.toLong()).without(WS_POPUP.toLong())
    fun getWinExStyle(fullscreen: Boolean): Long = if (fullscreen) 0L else WS_EX_OVERLAPPEDWINDOW.toLong()

    fun getBorderSize(): SizeInt {
        val w = 1000
        val h = 1000
        val out = getRealSize(w, h)
        return SizeInt(out.width - w, out.height - h)
    }

    fun getRealSize(width: Int, height: Int): SizeInt {
        return memScoped {
            val rect = alloc<RECT>()
            rect.width = width
            rect.height = height
            AdjustWindowRectEx(rect.ptr, winStyle.convert(), hasMenu.toInt().convert(), winExStyle.convert())
            SizeInt(rect.width, rect.height)
        }
    }

    fun keyEvent(keyCode: Int, type: KeyEvent.Type) {
        dispatch(keyEvent.apply {
            this.type = type
            this.id = 0
            this.key = KEYS[keyCode] ?: com.soywiz.korev.Key.UNKNOWN
            this.keyCode = keyCode
            this.character = keyCode.toChar()
            this.alt = GetKeyState(VK_MENU) < 0
            this.ctrl = GetKeyState(VK_CONTROL) < 0
            this.shift = GetKeyState(VK_SHIFT) < 0
            this.meta = GetKeyState(VK_LWIN) < 0 || GetKeyState(VK_RWIN) < 0
        })
    }

    fun keyUpdate(keyCode: Int, down: Boolean) {
        keyEvent(keyCode, if (down) com.soywiz.korev.KeyEvent.Type.DOWN else com.soywiz.korev.KeyEvent.Type.UP)
    }

    fun keyType(character: Int) {
        keyEvent(character, com.soywiz.korev.KeyEvent.Type.TYPE)
    }

    fun mouseEvent(
        etype: com.soywiz.korev.MouseEvent.Type, ex: Int, ey: Int,
        ebutton: Int, wParam: Int, scrollDeltaX: Double = 0.0, scrollDeltaY: Double = 0.0, scrollDeltaZ: Double = 0.0,
        scrollDeltaMode: MouseEvent.ScrollDeltaMode = MouseEvent.ScrollDeltaMode.LINE
    ) {
        val lbutton = (wParam and MK_LBUTTON) != 0
        val rbutton = (wParam and MK_RBUTTON) != 0
        val shift = (wParam and MK_SHIFT) != 0
        val control = (wParam and MK_CONTROL) != 0
        val mbutton = (wParam and MK_MBUTTON) != 0
        val xbutton1 = (wParam and MK_XBUTTON1) != 0
        val xbutton2 = (wParam and MK_XBUTTON2) != 0
        val anyButton = lbutton || rbutton || mbutton || xbutton1 || xbutton2
        var buttons = 0
        if (lbutton) buttons = buttons or 1
        if (rbutton) buttons = buttons or 2
        if (mbutton) buttons = buttons or 4
        if (xbutton1) buttons = buttons or 8
        if (xbutton2) buttons = buttons or 16

        dispatch(mouseEvent.apply {
            this.type = etype
            this.x = ex
            this.y = ey
            this.button = MouseButton[ebutton]
            this.buttons = buttons
            this.isAltDown = GetKeyState(VK_MENU) < 0
            this.isCtrlDown = control
            this.isShiftDown = shift
            this.isMetaDown = GetKeyState(VK_LWIN) < 0 || GetKeyState(VK_RWIN) < 0
            this.scrollDeltaX = scrollDeltaX
            this.scrollDeltaY = scrollDeltaY
            this.scrollDeltaZ = scrollDeltaZ
            this.scrollDeltaMode = MouseEvent.ScrollDeltaMode.LINE
            //this.scaleCoords = false
        })
    }
}

// @TODO: when + cases with .toUInt() or .convert() didn't work
val _WM_CREATE: UINT = WM_CREATE.convert()
val _WM_SIZE: UINT = WM_SIZE.convert()
val _WM_QUIT: UINT = WM_QUIT.convert()
val _WM_MOUSEMOVE: UINT = WM_MOUSEMOVE.convert()
val _WM_MOUSELEAVE: UINT = WM_MOUSELEAVE.convert()
val _WM_MOUSEWHEEL: UINT = WM_MOUSEWHEEL.convert()
val _WM_MOUSEHWHEEL: UINT = WM_MOUSEHWHEEL.convert()
val _WM_LBUTTONDOWN: UINT = WM_LBUTTONDOWN.convert()
val _WM_MBUTTONDOWN: UINT = WM_MBUTTONDOWN.convert()
val _WM_RBUTTONDOWN: UINT = WM_RBUTTONDOWN.convert()
val _WM_LBUTTONUP: UINT = WM_LBUTTONUP.convert()
val _WM_MBUTTONUP: UINT = WM_MBUTTONUP.convert()
val _WM_RBUTTONUP: UINT = WM_RBUTTONUP.convert()
val _WM_KEYDOWN: UINT = WM_KEYDOWN.convert()
val _WM_KEYUP: UINT = WM_KEYUP.convert()
val _WM_SYSKEYDOWN: UINT = WM_SYSKEYDOWN.convert()
val _WM_SYSKEYUP: UINT = WM_SYSKEYUP.convert()
val _WM_CLOSE: UINT = WM_CLOSE.convert()
val _WM_CHAR: UINT = WM_CHAR.convert()
val _WM_UNICHAR: UINT = WM_UNICHAR.convert()

@Suppress("UNUSED_PARAMETER")
fun WndProc(hWnd: HWND?, message: UINT, wParam: WPARAM, lParam: LPARAM): LRESULT {
    //println("WndProc: $hWnd, $message, $wParam, $lParam")
    when (message) {
        _WM_CREATE -> {
            memScoped {
                val pfd = alloc<PIXELFORMATDESCRIPTOR>()
                pfd.nSize = PIXELFORMATDESCRIPTOR.size.convert()
                pfd.nVersion = 1.convert()
                pfd.dwFlags = (PFD_DRAW_TO_WINDOW or PFD_SUPPORT_OPENGL or PFD_DOUBLEBUFFER).convert()
                pfd.iPixelType = PFD_TYPE_RGBA.convert()
                pfd.cColorBits = 32.convert()
                pfd.cDepthBits = 24.convert()
                pfd.cStencilBits = 8.convert()
                pfd.iLayerType = PFD_MAIN_PLANE.convert()
                val hDC = GetDC(hWnd)
                val letWindowsChooseThisPixelFormat = ChoosePixelFormat(hDC, pfd.ptr)

                SetPixelFormat(hDC, letWindowsChooseThisPixelFormat, pfd.ptr)
                windowsGameWindow.glRenderContext = wglCreateContext(hDC)
                wglMakeCurrent(hDC, windowsGameWindow.glRenderContext)

                val wglSwapIntervalEXT = wglGetProcAddressAny("wglSwapIntervalEXT")!!
                    .reinterpret<CFunction<Function1<Int, Int>>>()

                println("wglSwapIntervalEXT: $wglSwapIntervalEXT")
                wglSwapIntervalEXT?.invoke(0)
                glClear(0) // Required since wglMakeCurrent is in the windows package but requires openGL32.dll

                println("GL_CONTEXT: ${windowsGameWindow.glRenderContext}")
            }
        }
        _WM_SIZE -> {
            var width = 0
            var height = 0
            memScoped {
                val rect = alloc<RECT>()
                GetClientRect(hWnd, rect.ptr)
                width = (rect.right - rect.left).convert()
                height = (rect.bottom - rect.top).convert()
            }
            //val width = (lParam.toInt() ushr 0) and 0xFFFF
            //val height = (lParam.toInt() ushr 16) and 0xFFFF
            windowsGameWindow.resized(width, height)
        }
        _WM_QUIT -> {
            kotlin.system.exitProcess(0.convert())
        }
        _WM_MOUSEMOVE -> {
            val x = lParam.toInt().extract(0, 16)
            val y = lParam.toInt().extract(16, 16)
            mouseMove(x, y, wParam.toInt())
        }
        _WM_MOUSEWHEEL, _WM_MOUSEHWHEEL -> {
            val vertical = message == _WM_MOUSEWHEEL
            val type = com.soywiz.korev.MouseEvent.Type.SCROLL
            // #define GET_WHEEL_DELTA_WPARAM(wParam) ((short)HIWORD(wParam))
            // @TODO: To retrieve the wheel scroll units, use the inputData filed of the POINTER_INFO
            // @TODO: structure returned by calling GetPointerInfo function. This field contains a signed value
            // @TODO: and is expressed in a multiple of WHEEL_DELTA. A positive value indicates a rotation forward
            // @TODO: and a negative value indicates a rotation backward.
            // @TODO: https://docs.microsoft.com/en-us/windows/win32/inputmsg/wm-pointerhwheel
            val intWheelDelta = wParam.toInt().extract(16, 16).toShort().toInt()
            val scrollDelta = (-intWheelDelta.toDouble() / 120) * 3
            //println("vertical=$vertical, scrollDelta=$scrollDelta, intWheelDelta=$intWheelDelta")
            windowsGameWindow.mouseEvent(
                type, mouseX, mouseY, 8, wParam.toInt(),
                scrollDeltaX = if (!vertical) scrollDelta else 0.0,
                scrollDeltaY = if (vertical) scrollDelta else 0.0,
                scrollDeltaZ = 0.0,
                scrollDeltaMode = MouseEvent.ScrollDeltaMode.LINE
            )
        }
        _WM_LBUTTONDOWN -> mouseButton(0, true, wParam.toInt())
        _WM_MBUTTONDOWN -> mouseButton(1, true, wParam.toInt())
        _WM_RBUTTONDOWN -> mouseButton(2, true, wParam.toInt())
        _WM_LBUTTONUP -> mouseButton(0, false, wParam.toInt())
        _WM_MBUTTONUP -> mouseButton(1, false, wParam.toInt())
        _WM_RBUTTONUP -> mouseButton(2, false, wParam.toInt())
        _WM_KEYDOWN -> windowsGameWindow.keyUpdate(wParam.toInt(), true)
        _WM_KEYUP -> windowsGameWindow.keyUpdate(wParam.toInt(), false)
        _WM_SYSKEYDOWN -> windowsGameWindow.keyUpdate(wParam.toInt(), true)
        _WM_SYSKEYUP -> windowsGameWindow.keyUpdate(wParam.toInt(), false)
        _WM_CHAR -> windowsGameWindow.keyType(wParam.toInt())
        //_WM_UNICHAR -> windowsGameWindow.keyType(wParam.toInt())
        _WM_CLOSE -> kotlin.system.exitProcess(0)
    }
    return DefWindowProcW(hWnd, message, wParam, lParam)
}


val COMDLG32_DLL: HMODULE? by lazy { LoadLibraryA("comdlg32.dll") }

val GetOpenFileNameWFunc by lazy {
    GetProcAddress(COMDLG32_DLL, "GetOpenFileNameW") as CPointer<CFunction<Function1<CPointer<OPENFILENAMEW>, BOOL>>>
}

data class WinFileFilter(val name: String, val pattern: String)

fun openSelectFile(
    initialDir: String? = null,
    filters: List<WinFileFilter> = listOf(WinFileFilter("All (*.*)", "*.*")),
    hwnd: HWND? = null
): String? = memScoped {
    val szFileSize = 1024
    val szFile = allocArray<WCHARVar>(szFileSize + 1)
    val ofn = alloc<OPENFILENAMEW>().apply {
        lStructSize = OPENFILENAMEW.size.convert()
        hwndOwner = hwnd
        lpstrFile = szFile.reinterpret()
        nMaxFile = szFileSize.convert()
        lpstrFilter =
            (filters.flatMap { listOf(it.name, it.pattern) }.joinToString("\u0000") + "\u0000").wcstr.ptr.reinterpret()
        nFilterIndex = 1.convert()
        lpstrFileTitle = null
        nMaxFileTitle = 0.convert()
        lpstrInitialDir = if (initialDir != null) initialDir.wcstr.ptr.reinterpret() else null
        Flags = (OFN_PATHMUSTEXIST or OFN_FILEMUSTEXIST).convert()
    }
    val res = GetOpenFileNameWFunc(ofn.ptr.reinterpret())
    if (res.toInt() != 0) szFile.reinterpret<ShortVar>().toKString() else null
}

@ThreadLocal
private var mouseX: Int = 0

@ThreadLocal
private var mouseY: Int = 0

@ThreadLocal
private val xInputEventAdapter = XInputEventAdapter()

//@ThreadLocal
//private val buttons = BooleanArray(16)

val MK_LBUTTON = 0x0001
val MK_RBUTTON = 0x0002
val MK_SHIFT = 0x0004
val MK_CONTROL = 0x0008
val MK_MBUTTON = 0x0010
val MK_XBUTTON1 = 0x0020
val MK_XBUTTON2 = 0x0040

fun mouseMove(x: Int, y: Int, wParam: Int) {
    mouseX = x
    mouseY = y
    SetCursor(ARROW_CURSOR)

    val anyButton = (wParam and (MK_LBUTTON or MK_RBUTTON or MK_MBUTTON or MK_XBUTTON1 or MK_XBUTTON2)) != 0

    windowsGameWindow.mouseEvent(
        if (anyButton) com.soywiz.korev.MouseEvent.Type.DRAG else com.soywiz.korev.MouseEvent.Type.MOVE,
        mouseX, mouseY, 0, wParam
    )
}

fun mouseButton(button: Int, down: Boolean, wParam: Int) {
    //buttons[button] = down
    if (down) {
        windowsGameWindow.mouseEvent(com.soywiz.korev.MouseEvent.Type.DOWN, mouseX, mouseY, button, wParam)
    } else {
        windowsGameWindow.mouseEvent(com.soywiz.korev.MouseEvent.Type.UP, mouseX, mouseY, button, wParam)
        windowsGameWindow.mouseEvent(
            com.soywiz.korev.MouseEvent.Type.CLICK,
            mouseX, mouseY, button, wParam
        ) // @TODO: Conditionally depending on the down x,y & time
    }
}

//val String.glstr: CPointer<GLcharVar> get() = this.cstr.reinterpret()
val OPENGL32_DLL_MODULE: HMODULE? by lazy { LoadLibraryA("opengl32.dll") }

fun wglGetProcAddressAny(name: String): PROC? {
    return wglGetProcAddress(name)
        ?: GetProcAddress(OPENGL32_DLL_MODULE, name)
        ?: throw RuntimeException("Can't find GL function: '$name'")
}

val USER32_DLL by lazy { LoadLibraryA("User32.dll") }
val LoadCursorAFunc by lazy {
    GetProcAddress(USER32_DLL, "LoadCursorA")!!.reinterpret<CFunction<Function2<Int, Int, HCURSOR?>>>()
}

val LoadIconAFunc by lazy {
    GetProcAddress(USER32_DLL, "LoadIconA")!!.reinterpret<CFunction<Function2<HMODULE?, Int, HICON?>>>()
}

val FindResourceAFunc by lazy {
    GetProcAddress(USER32_DLL, "FindResourceA")!!.reinterpret<CFunction<Function2<HMODULE?, Int, HICON?>>>()
}

//val ARROW_CURSOR by lazy { LoadCursorA(null, 32512.reinterpret<CPointer<ByteVar>>().reinterpret()) }
val ARROW_CURSOR: HICON? by lazy { LoadCursorAFunc(0, 32512)!! }
