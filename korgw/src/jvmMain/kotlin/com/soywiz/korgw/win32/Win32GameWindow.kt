package com.soywiz.korgw.win32

/*
import com.soywiz.kgl.KmlGl
import com.soywiz.korag.AGOpengl
import com.soywiz.korev.Key
import com.soywiz.korev.KeyEvent
import com.soywiz.korev.MouseButton
import com.soywiz.korev.MouseEvent
import com.soywiz.korgw.GameWindow
import com.soywiz.korgw.GameWindowCoroutineDispatcher
import com.soywiz.korgw.platform.BaseOpenglContext
import com.soywiz.korgw.platform.INativeGL
import com.soywiz.korgw.platform.NativeKgl
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.net.URL
import com.soywiz.korma.geom.*
import com.sun.jna.*
import com.sun.jna.Function
import com.sun.jna.platform.win32.*
import com.sun.jna.platform.win32.WinDef.*
import com.sun.jna.platform.win32.WinUser.*
import com.sun.jna.ptr.PointerByReference
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.coroutines.*
import kotlin.math.max
import kotlin.math.min


class Win32Ag(val window: Win32GameWindow, override val gl: KmlGl = Win32KmlGl) : AGOpengl() {
    override val gles: Boolean = true
    override val nativeComponent: Any = window
}

class Win32GameWindow : GameWindow() {
    var hRC: WinDef.HGLRC? = null
    var hDC: WinDef.HDC? = null
    var glCtx: Win32OpenglContext? = null
    var hWnd: HWND? = null

    override val key: CoroutineContext.Key<*>
        get() = super.key
    override val ag: Win32Ag = Win32Ag(this)
    override val coroutineDispatcher: GameWindowCoroutineDispatcher
        get() = super.coroutineDispatcher
    override var title: String = "Korgw"
        set(value) {
            field = value
            if (hWnd != null) {
                Win32.SetWindowText(hWnd, value)
            }
        }
    override var width: Int = 200; private set
    override var height: Int = 200; private set
    override var icon: Bitmap? = null
        set(value) {
            field = value
            if (value != null) {
                Win32.SendMessage(hWnd, WM_SETICON, WinDef.WPARAM(ICON_BIG.toLong())  , WinDef.LPARAM(Pointer.nativeValue(value.toBMP32().scaled(32, 32).toWin32Icon()!!.pointer)))
                Win32.SendMessage(hWnd, WM_SETICON, WinDef.WPARAM(ICON_SMALL.toLong()), WinDef.LPARAM(Pointer.nativeValue(value.toBMP32().scaled(16, 16).toWin32Icon()!!.pointer)))
            }
        }
    override var fullscreen: Boolean
        get() = super.fullscreen
        set(value) {}
    override var visible: Boolean
        get() = super.visible
        set(value) {}
    override var quality: Quality
        get() = super.quality
        set(value) {}

    override fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height

        if (hWnd != null) {
            val rect = WinDef.RECT()
            val borderSize = getBorderSize()
            Win32.GetWindowRect(hWnd, rect)
            Win32.MoveWindow(hWnd, rect.left, rect.top, width + borderSize.width, height + borderSize.height, true)
        }
    }

    override suspend fun browse(url: URL) {
        //Shell32.ShellExecute()
        //ShellExecute(0, 0, L"http://www.google.com", 0, 0 , SW_SHOW );

        super.browse(url)
    }

    override suspend fun alert(message: String) {
        Win32.MessageBox(hWnd, "Alert", message, 0)
    }

    override suspend fun confirm(message: String): Boolean {
        return super.confirm(message)
    }

    override suspend fun prompt(message: String, default: String): String {
        return super.prompt(message, default)
    }

    override suspend fun openFileDialog(filter: FileFilter?, write: Boolean, multi: Boolean, currentDir: VfsFile?): List<VfsFile> {
        return super.openFileDialog(filter, write, multi)
    }

    var exiting = false

    override fun close() {
        super.close()
        exiting = true
    }

    fun putWindowsToTop() {
        val HWND_TOPMOST = HWND(Pointer.createConstant(-1))
        Win32.SetWindowPos(hWnd, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOMOVE or SWP_NOSIZE)
        //Win32.SetWindowPos(hWnd, HWND(Pointer.createConstant(-1)), 0, 0, 0, 0, SWP_NOSIZE or SWP_NOMOVE);
        Win32.SetActiveWindow(hWnd)
        Win32.BringWindowToTop(hWnd)
        Win32.SetForegroundWindow(hWnd)
    }

    /*
    fun SetForegroundWindowInternal(hWnd: HWND?)
    {
        if (!Win32.IsWindow(hWnd)) return;

        //relation time of SetForegroundWindow lock
        val lockTimeOut = 0;
        val  hCurrWnd = Win32.GetForegroundWindow();
        val dwThisTID = Win32.GetCurrentThreadId(),
        val dwCurrTID = Win32.GetWindowThreadProcessId(hCurrWnd,0);

        //we need to bypass some limitations from Microsoft :)
        if(dwThisTID != dwCurrTID)
        {
            Win32.AttachThreadInput(dwThisTID, dwCurrTID, true);

            Win32.SystemParametersInfo(SPI_GETFOREGROUNDLOCKTIMEOUT,0,&lockTimeOut,0);
            Win32.SystemParametersInfo(SPI_SETFOREGROUNDLOCKTIMEOUT,0,0,SPIF_SENDWININICHANGE | SPIF_UPDATEINIFILE);

            Win32.AllowSetForegroundWindow(ASFW_ANY);
        }

        Win32.SetForegroundWindow(hWnd);

        if(dwThisTID != dwCurrTID)
        {
            Win32.SystemParametersInfo(SPI_SETFOREGROUNDLOCKTIMEOUT,0,(PVOID)lockTimeOut,SPIF_SENDWININICHANGE | SPIF_UPDATEINIFILE);
            Win32.AttachThreadInput(dwThisTID, dwCurrTID, FALSE);
        }
    }
    */

    val windProc = object : WinUser.WindowProc {
        override fun callback(
            hwnd: HWND,
            uMsg: Int,
            wParam: WPARAM,
            lParam: LPARAM
        ): LRESULT {
            return when (uMsg) {
                WM_PAINT -> {
                    //FillRect(hdc, ps.rcPaint, (HBRUSH) (COLOR_WINDOW+1));
                    //EndPaint(hwnd, ps);
                    glCtx?.makeCurrent()
                    Win32.wglMakeCurrent(hDC, hRC)
                    Win32.glClearColor(.3f, .6f, .9f, 1f)

                    Win32.glClear(MyOpenGL32.GL_COLOR_BUFFER_BIT)
                    //println(
                    //    glGetString(GL_VENDOR) + " - " +
                    //        glGetString(GL_VERSION) + " - " +
                    //        glGetString(GL_RENDERER) + " - " +
                    //        glGetString(GL_EXTENSIONS) + " - " +
                    //        glGetString(GL_SHADING_LANGUAGE_VERSION)
                    //)

                    frame()

                    //SwapBuffers(hDC)
                    glCtx?.swapBuffers()

                    LRESULT(0)
                }
                WM_SIZE -> {
                    val rect = WinDef.RECT()
                    rect.autoWrite()
                    rect.autoRead()
                    val result = Win32.GetWindowRect(hWnd, rect)
                    rect.read()
                    //println("rect: $rect ${rect.pointer}")
                    val borderSize = getBorderSize()
                    width = rect.width - borderSize.width
                    height = rect.height - borderSize.height
                    //println("WM_SIZE: $result, $width, $height")
                    Win32.wglMakeCurrent(hDC, hRC)
                    Win32.glViewport(0, 0, width, height)
                    dispatchReshapeEvent(rect.left, rect.top, width, height)
                    LRESULT(0)
                }
                WM_CREATE -> {
                    //onCreate(wParam, lParam)
                    //display()
                    //BeginPaint(hWnd, & ps)
                    //EndPaint(hWnd, & ps)
                    //putWindowsToTop()

                    putWindowsToTop()
                    LRESULT(0)
                }
                WM_DESTROY -> {
                    Win32.PostQuitMessage(0)
                    exiting = true
                    LRESULT(0)
                }
                WM_SESSION_CHANGE -> {
                    //onSessionChange(wParam, lParam)
                    LRESULT(0)
                }
                WM_DEVICECHANGE -> {
                    Win32.DefWindowProc(hwnd, uMsg, wParam, lParam)
                }
                WM_MOUSEMOVE
                    , WM_LBUTTONDOWN, WM_LBUTTONUP
                    , WM_MBUTTONDOWN, WM_MBUTTONUP
                    , WM_RBUTTONDOWN, WM_RBUTTONUP
                -> {
                    val xPos: Int = (lParam.toInt() ushr 0) and 0xFFFF
                    val yPos: Int = (lParam.toInt() ushr 16) and 0xFFFF
                    //println("WM_MOUSEMOVE: $hwnd, $uMsg, $wParam, $lParam, $point ($xPos, $yPos)")

                    val event = when (uMsg) {
                        WM_MOUSEMOVE -> MouseEvent.Type.MOVE
                        WM_LBUTTONDOWN, WM_MBUTTONDOWN, WM_RBUTTONDOWN -> MouseEvent.Type.DOWN
                        WM_LBUTTONUP, WM_MBUTTONUP, WM_RBUTTONUP -> MouseEvent.Type.UP
                        else -> MouseEvent.Type.ENTER
                    }

                    val button = when (uMsg) {
                        WM_LBUTTONDOWN, WM_LBUTTONUP -> MouseButton.LEFT
                        WM_MBUTTONDOWN, WM_MBUTTONUP -> MouseButton.MIDDLE
                        WM_RBUTTONDOWN, WM_RBUTTONUP -> MouseButton.RIGHT
                        else -> MouseButton.BUTTON_UNKNOWN
                    }

                    dispatchSimpleMouseEvent(event, 0, xPos, yPos, button, simulateClickOnUp = true)
                    LRESULT(0)
                }
                WM_KEYDOWN, WM_KEYUP -> {
                    dispatchKeyEvent(
                        when (uMsg) {
                            WM_KEYDOWN -> KeyEvent.Type.DOWN
                            else -> KeyEvent.Type.UP
                        }, 0,
                        ' ',
                        VK_TABLE[wParam.toInt()] ?: Key.UNKNOWN,
                        wParam.toInt()
                    )
                    LRESULT(0)
                }
                WM_SYSCHAR -> {
                    LRESULT(0)
                }
                else -> {
                    //println("EVENT: $hwnd, $uMsg, $wParam, $lParam")
                    Win32.DefWindowProc(
                        hwnd,
                        uMsg,
                        wParam,
                        lParam
                    )
                }
            }
        }
    }

    private val hasMenu = false
    private val winStyle = WS_OVERLAPPEDWINDOW
    private val winExStyle = WS_EX_CLIENTEDGE

    fun getBorderSize(): SizeInt {
        val w = 1000
        val h = 1000
        val out = getRealSize(w, h)
        return SizeInt(out.width - w, out.height - h)
    }

    fun getRealSize(width: Int, height: Int): SizeInt {
        val rect = WinDef.RECT()
        rect.width = width
        rect.height = height
        Win32.AdjustWindowRectEx(rect, WinDef.DWORD(winStyle.toLong()), WinDef.BOOL(hasMenu), WinDef.DWORD(winExStyle.toLong()))
        return SizeInt(rect.width, rect.height)
    }

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        launchImmediately(getCoroutineDispatcherWithCurrentContext()) {
            entry()
        }

        Win32.apply {
            val windowClass = "KorgwWindowClass"
            val hInst = GetModuleHandle(null)
            val wClass = WinUser.WNDCLASSEX()

            wClass.hInstance = hInst
            wClass.lpfnWndProc = windProc
            wClass.lpszClassName = windowClass
            wClass.hCursor = Win32.LoadCursor(null, IDC_ARROW)
            RegisterClassEx(wClass)

            val windowWidth = max(128, width)
            val windowHeight = max(128, height)
            val screenWidth = GetSystemMetrics(SM_CXSCREEN)
            val screenHeight = GetSystemMetrics(SM_CYSCREEN)

            val realSize = getRealSize(windowWidth, windowHeight)
            val realWidth = realSize.width
            val realHeight = realSize.height

            println("Initial window size: $windowWidth, $windowHeight")

            hWnd = CreateWindowEx(
                winExStyle,
                windowClass,
                title,
                winStyle,
                min(max(0, (screenWidth - realWidth) / 2), screenWidth - 16),
                min(max(0, (screenHeight - realHeight) / 2), screenHeight - 16),
                realWidth,
                realHeight,
                null, null, hInst, null
            )

            glCtx = Win32OpenglContext(hWnd!!)
            hDC = glCtx!!.hDC
            hRC = glCtx!!.hRC
            glCtx!!.makeCurrent()

            //val test2 = OpenGL32.INSTANCE.wglGetProcAddress("wglCreateContextAttribsARB")

            //wglGetProcAddress()

            //ReleaseDC(hWnd, hDC)

            Win32.AllowSetForegroundWindow(-1)
            //ShowWindow(hWnd, SW_SHOWNORMAL)
            //ShowWindow(hWnd, SW_SHOW)
            ShowWindow(hWnd, SW_RESTORE)
            //putWindowsToTop()
            //putWindowsToTop()
            val lastError = Native.getLastError()

            println("HELLO $screenWidth, $screenHeight : $hWnd, $lastError")

            val msg = WinUser.MSG()
            while (GetMessage(msg, hWnd, 0, 0) != 0) {
                TranslateMessage(msg)
                DispatchMessage(msg)
                if (exiting) break
            }

            wglMakeCurrent(null, null)
            ReleaseDC(hWnd, hDC)
            wglDeleteContext(hRC)
            DestroyWindow(hWnd)
        }
    }
}
*/
