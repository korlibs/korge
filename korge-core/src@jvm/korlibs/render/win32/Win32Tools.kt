package korlibs.render.win32

import korlibs.kgl.*
import korlibs.logger.*
import korlibs.render.*
import korlibs.render.platform.*
import korlibs.image.bitmap.*
import korlibs.io.lang.*
import com.sun.jna.*
import com.sun.jna.Function
import com.sun.jna.platform.win32.*
import com.sun.jna.platform.win32.WinDef.*
import com.sun.jna.ptr.*
import com.sun.jna.win32.*
import java.awt.*
import java.io.*
import java.lang.reflect.*
import java.util.concurrent.*

//open class Win32KmlGl : CheckErrorsKmlGlProxy(NativeKgl(Win32GL)) {
open class Win32KmlGl : NativeKgl(DirectGL) {
    companion object : Win32KmlGl()

    var vertexArrayCachedVersion = -1
    var vertexArray = -1

    override fun beforeDoRender(contextVersion: Int) {
        if (vertexArrayCachedVersion != contextVersion) {
            vertexArrayCachedVersion = contextVersion
            val out = intArrayOf(-1)
            //checkError("before glGenVertexArrays")
            DirectGL.glGenVertexArrays(1, out)
            checkError("glGenVertexArrays")
            vertexArray = out[0]
        }
        DirectGL.glBindVertexArray(vertexArray)
        //checkError("glBindVertexArray")
    }
}

//interface Win32GL : INativeGL, StdCallLibrary {
interface Win32GL : StdCallLibrary {
    //fun wglChoosePixelFormat(hDC: HDC, piAttribIList: Pointer?, pfAttribFList: Pointer?, nMaxFormats: Int, piFormats: Pointer?, nNumFormats: Pointer?): Int
    //fun wglCreateContextAttribs(hDC: HDC, hshareContext: WinGDI.PIXELFORMATDESCRIPTOR.ByReference, attribList: Pointer?): Int

    fun wglChoosePixelFormatARB(hDC: HDC, piAttribIList: IntArray?, pfAttribFList: FloatArray?, nMaxFormats: Int, piFormats: Pointer?, nNumFormats: Pointer?): Int
    //fun wglCreateContextAttribsARB(hDC: HDC, hshareContext: WinGDI.PIXELFORMATDESCRIPTOR.ByReference?, attribList: Pointer?): Int
    fun wglCreateContextAttribsARB(hDC: HDC, hshareContext: WinGDI.PIXELFORMATDESCRIPTOR.ByReference?, attribList: IntArray?): HGLRC?

    companion object : Win32GL by Win32OpenglLoader.OpenglLoadProxy()
}

interface WGL : StdCallLibrary {
    fun wglGetProcAddress(name: String): Pointer?
    fun wglGetCurrentContext(): HGLRC?
    fun wglGetCurrentDC(): HDC?
    fun wglMakeCurrent(windowDC: HDC?, hglrc: HGLRC?): Boolean
    fun wglCreateContext(windowDC: HDC?): HGLRC?
    fun wglDeleteContext(hglrc: HGLRC?): Boolean

    //fun wglChoosePixelFormatARB(hDC: HDC, piAttribIList: IntArray?, pfAttribFList: FloatArray?, nMaxFormats: Int, piFormats: Pointer?, nNumFormats: Pointer?): Int
    //fun wglCreateContextAttribsARB(hDC: HDC, hshareContext: WinGDI.PIXELFORMATDESCRIPTOR.ByReference?, attribList: IntArray?): HGLRC?

    companion object : WGL by Native.load("opengl32", WGL::class.java)
}

object Win32OpenglLoader {
    // https://gist.github.com/nickrolfe/1127313ed1dbf80254b614a721b3ee9c
    val dummyName = "Dummy_WGL_korgw"
    val CS_VREDRAW = 1
    val CS_HREDRAW = 2
    val CS_OWNDC = 0x0020
    val CW_USEDEFAULT = 0x80000000L.toInt()
    val winClass = WinUser.WNDCLASSEX().also {
        it.style = CS_HREDRAW or CS_VREDRAW or CS_OWNDC
        it.lpfnWndProc = WinUser.WindowProc { hwnd, uMsg, wParam, lParam -> Win32.DefWindowProc(hwnd, uMsg, wParam, lParam) }
        it.hInstance = Win32.GetModuleHandle(null)
        it.lpszClassName = dummyName
    }
    val opengl32Lib = NativeLibrary.getInstance("opengl32")

    init {
        Win32.RegisterClassEx(winClass)
    }
    //val hWND = HWND(Native.getWindowPointer(Window(Frame())))
    val hWND = Win32.CreateWindowEx(
        0, dummyName, "Dummy OpenGL Window",
        0, CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT,
        null, null, winClass.hInstance, null
    )
    val hDC = Win32.GetDC(hWND)
    val pfd = WinGDI.PIXELFORMATDESCRIPTOR.ByReference().also { pfd ->
        pfd.nSize = pfd.size().toShort()
        pfd.nVersion = 1
        pfd.dwFlags =
            WinGDI.PFD_DRAW_TO_WINDOW or WinGDI.PFD_SUPPORT_OPENGL or (if (true) WinGDI.PFD_DOUBLEBUFFER else 0)
        //pfd.dwFlags = WinGDI.PFD_DRAW_TO_WINDOW or WinGDI.PFD_SUPPORT_OPENGL;
        pfd.iPixelType = WinGDI.PFD_TYPE_RGBA.toByte()
        pfd.cColorBits = 24
        pfd.cStencilBits = 8.toByte()
        //pfd.cColorBits = 24
        //pfd.cDepthBits = 16
        //pfd.cDepthBits = 32
        pfd.cDepthBits = 24
    }
    val pf = Win32.ChoosePixelFormat(hDC, pfd).also { pf ->
        Win32.SetPixelFormat(hDC, pf, pfd)
    }
    //DescribePixelFormat(hDC, pf, sizeof(PIXELFORMATDESCRIPTOR), &pfd);

    //val attribs = intArrayOf(
    //    WGL_CONTEXT_MAJOR_VERSION_ARB, 3,
    //    WGL_CONTEXT_MINOR_VERSION_ARB, 3
    //)

    //hRC = wglCreateContextAttribsARB (hDC, null, attribs);
    val hRC = WGL.wglCreateContext(hDC)

    inline fun <T> makeCurrentTemporarily(block: () -> T): T {
        val oldRC = WGL.wglGetCurrentContext()
        val oldDC = WGL.wglGetCurrentDC()
        try {
            WGL.wglMakeCurrent(hDC, hRC)
            return block()
        } finally {
            WGL.wglMakeCurrent(oldDC, oldRC)
        }
    }

    fun ensureAnyOpenglContext() {
        if (WGL.wglGetCurrentContext() == null) {
            WGL.wglMakeCurrent(hDC, hRC)
        }
    }

    private fun __loadFunction(name: String): Function? {
        return WGL.wglGetProcAddress(name)?.let { Function.getFunction(it) }
            ?: try { opengl32Lib.getFunction(name) } catch (e: UnsatisfiedLinkError) { null }
    }

    fun loadFunction(name: String): Function? {
        makeCurrentTemporarily {
            __loadFunction(name)?.let { return it }
            if (name.endsWith("ARB")) __loadFunction(name.removeSuffix("ARB"))?.let { return it }
            return null
        }
    }

    fun loadFunctionCachedOrNull(name: String): Function? {
        if (!funcsSet.contains(name)) {
            funcsSet[name] = true
            loadFunction(name)?.let { funcs[name] = it }
        }
        return funcs[name]
    }

    fun loadFunctionCached(name: String): Function {
        return loadFunctionCachedOrNull(name) ?: error("Can't find opengl method $name")
    }

    fun close() {
        WGL.wglMakeCurrent(hDC, null)
        WGL.wglDeleteContext(hRC)
        Win32.ReleaseDC(hWND, hDC)
        Win32.DestroyWindow(hWND)
        opengl32Lib.close()
    }

    private val funcsSet = ConcurrentHashMap<String, Boolean>()
    private val funcs = ConcurrentHashMap<String, Function?>()
    //val opengl32Lib by lazy { NativeLibrary.getInstance("opengl32") }

    init {
        if (Environment["DEBUG_OPENGL32_LOAD"] == "true") {
            println("opengl32Lib=$opengl32Lib, CWD=${File(".").absoluteFile}")
        }
    }

    fun OpenglLoadProxy(): Win32GL {
        try {
            val classLoader = Win32KmlGl::class.java.classLoader
            return Proxy.newProxyInstance(
                classLoader,
                arrayOf(Win32GL::class.java)
            ) { obj: Any?, method: Method, args: Array<Any?>? ->
                loadFunctionCached(method.name).invoke(method.returnType, args)
            } as Win32GL
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }
    }
}


private fun Bitmap32.toWin32Icon(): HICON? {
    val bmp = this.clone().flipY().toBMP32()

    val bi = BITMAPV5HEADER()
    bi.bV5Size = bi.size()
    bi.bV5Width = bmp.width
    bi.bV5Height = bmp.height
    bi.bV5Planes = 1
    bi.bV5BitCount = 32
    bi.bV5Compression = WinGDI.BI_BITFIELDS
    // The following mask specification specifies a supported 32 BPP
    // alpha format for Windows XP.
    bi.bV5RedMask = 0x00FF0000
    bi.bV5GreenMask = 0x0000FF00
    bi.bV5BlueMask = 0x000000FF
    bi.bV5AlphaMask = 0xFF000000.toInt()

    val hdc = Win32.GetDC(null)

    val lpBits = PointerByReference()
    val hBitmap = Win32.CreateDIBSection(hdc, bi, WinGDI.DIB_RGB_COLORS, lpBits, null, 0)
    val memdc = Win32.CreateCompatibleDC(null)
    Win32.ReleaseDC(null, hdc);

    val bitsPtr = lpBits.value
    for (n in 0 until bmp.ints.size) {
        bitsPtr.setInt((n * 4).toLong(), bmp.ints[n])
    }

    val hMonoBitmap = Win32.CreateBitmap(bmp.width, bmp.height, 1, 1, null)

    val ii = WinGDI.ICONINFO()
    ii.fIcon = true // Change fIcon to TRUE to create an alpha icon
    ii.xHotspot = 0
    ii.yHotspot = 0
    ii.hbmMask = hMonoBitmap
    ii.hbmColor = hBitmap
    val icon = Win32.CreateIconIndirect(ii)

    Win32.DeleteDC( memdc );
    Win32.DeleteObject( hBitmap )
    Win32.DeleteObject(hMonoBitmap)

    return icon
}

private fun Bitmap32.scaled(width: Int, height: Int): Bitmap32 {
    val scaleX = width.toDouble() / this.width.toDouble()
    val scaleY = height.toDouble() / this.height.toDouble()
    return scaleLinear(scaleX, scaleY)
}

// https://www.khronos.org/opengl/wiki/Creating_an_OpenGL_Context_(WGL)
class Win32OpenglContext(val gwconfig: GameWindowConfig, val hWnd: WinDef.HWND, val hDC: HDC, val doubleBuffered: Boolean = false, val component: Component? = null) : BaseOpenglContext {

    //val pfd = WinGDI.PIXELFORMATDESCRIPTOR.ByReference().also { pfd ->
    //    pfd.nSize = pfd.size().toShort()
    //    pfd.nVersion = 1
    //    pfd.dwFlags =
    //        WinGDI.PFD_DRAW_TO_WINDOW or WinGDI.PFD_SUPPORT_OPENGL or (if (doubleBuffered) WinGDI.PFD_DOUBLEBUFFER else 0)
    //    //pfd.dwFlags = WinGDI.PFD_DRAW_TO_WINDOW or WinGDI.PFD_SUPPORT_OPENGL;
    //    pfd.iPixelType = WinGDI.PFD_TYPE_RGBA.toByte()
    //    pfd.cColorBits = 24
    //    pfd.cStencilBits = 8.toByte()
    //    //pfd.cColorBits = 24
    //    //pfd.cDepthBits = 16
    //    //pfd.cDepthBits = 32
    //    pfd.cDepthBits = 24
    //}
    //val pf = Win32.ChoosePixelFormat(hDC, pfd)

    var pf: Int = 0
    lateinit var pfd: WinGDI.PIXELFORMATDESCRIPTOR.ByReference

    //hRC = wglCreateContextAttribsARB (hDC, null, attribs);

    //val requestCoreProfile = false
    val requestCoreProfile = true
    var hRC: HGLRC? = null

    fun init() = this.apply {
        pf = run {
            val attribList = intArrayOf(
                WGL_DRAW_TO_WINDOW_ARB, GL_TRUE,
                WGL_SUPPORT_OPENGL_ARB, GL_TRUE,
                WGL_DOUBLE_BUFFER_ARB, GL_TRUE,
                //WGL_DOUBLE_BUFFER_ARB, GL_FALSE,
                WGL_PIXEL_TYPE_ARB, WGL_TYPE_RGBA_ARB,
                WGL_COLOR_BITS_ARB, 32,
                WGL_DEPTH_BITS_ARB, 24,
                WGL_STENCIL_BITS_ARB, 8,
                WGL_SAMPLE_BUFFERS_ARB, 1, // Number of buffers (must be 1 at time of writing)
                WGL_SAMPLES_ARB, 4,        // Number of samples
                0, // End
            )
            val pixelFormatMemory = Memory(8L).also { it.clear() }
            val numFormatsMemory = Memory(8L).also { it.clear() }
            // https://registry.khronos.org/OpenGL/extensions/ARB/WGL_ARB_pixel_format.txt
            val result = Win32GL.wglChoosePixelFormatARB(hDC, attribList, null, 1, pixelFormatMemory, numFormatsMemory)
            val numFormats = numFormatsMemory.getInt(0)
            val pixelFormat = pixelFormatMemory.getInt(0)
            if (numFormats == 0) {
                error("wglChoosePixelFormatARB: Can't get opengl formats, hWnd=$hWnd, hDC=$hDC, numFormats=$numFormats, pixelFormat=$pixelFormat, result=$result")
            }
            pixelFormat
        }

        pfd = WinGDI.PIXELFORMATDESCRIPTOR.ByReference().also { pfd ->
            Win32.DescribePixelFormat(hDC, pf, WinGDI.PIXELFORMATDESCRIPTOR().size(), pfd)
        }
        //println("PF: $pf, PFD: $pfd")

        //DescribePixelFormat(hDC, pf, sizeof(PIXELFORMATDESCRIPTOR), &pfd);
        Win32.SetPixelFormat(hDC, pf, pfd)

        //val attribs = intArrayOf(
        //    WGL_CONTEXT_MAJOR_VERSION_ARB, 3,
        //    WGL_CONTEXT_MINOR_VERSION_ARB, 3
        //)

        hRC = Win32GL.wglCreateContextAttribsARB(hDC, null, intArrayOf(
            WGL_CONTEXT_MAJOR_VERSION_ARB, 3,
            WGL_CONTEXT_MINOR_VERSION_ARB, 3,
            WGL_CONTEXT_PROFILE_MASK_ARB, if (requestCoreProfile) WGL_CONTEXT_CORE_PROFILE_BIT_ARB else WGL_CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB,
            0
        )).also {
            logger.debug { "wglCreateContextAttribsARB.error: ${Native.getLastError()}" }
        }

        logger.debug { "hWnd: $hWnd, hDC: $hDC, hRC: $hRC, component: $component" }
        makeCurrent()

        Win32KmlGl.apply {
            logger.debug { "GL_VERSION: ${getString(VERSION)}, GL_VENDOR: ${getString(VENDOR)}" }
            // Only available on GL_ES?
            //Console.trace(
            //    "GL_RED_BITS: ${getIntegerv(RED_BITS)}, GL_GREEN_BITS: ${getIntegerv(GREEN_BITS)}, " +
            //        "GL_BLUE_BITS: ${getIntegerv(BLUE_BITS)}, GL_ALPHA_BITS: ${getIntegerv(ALPHA_BITS)}, " +
            //        "GL_DEPTH_BITS: ${getIntegerv(DEPTH_BITS)}, GL_STENCIL_BITS: ${getIntegerv(STENCIL_BITS)}"
            //)
            //println()
        }

        logger.debug { "requestCoreProfile=$requestCoreProfile, isCore=$isCore, extensions=${extensions.size}" }
    }

    val extensions by lazy {
        (0 until Win32KmlGl.getIntegerv(GL_NUM_EXTENSIONS)).map {
            DirectGL.glGetStringi(Win32KmlGl.EXTENSIONS, it)
        }.toSet()
    }

    override val isCore by lazy {
        !extensions.contains("GL_ARB_compatibility")
    }

    override fun getCurrent(): Any? {
        return WGL.wglGetCurrentContext()
    }

    val GL_MULTISAMPLE = 0x809D

    override fun makeCurrent() {
        //println("makeCurrent")
        makeCurrent(hDC, hRC)
        when (gwconfig.quality) {
            GameWindow.Quality.QUALITY -> DirectGL.glEnable(GL_MULTISAMPLE)
            GameWindow.Quality.PERFORMANCE,
            GameWindow.Quality.AUTOMATIC -> DirectGL.glDisable(GL_MULTISAMPLE)
        }

    }

    override fun releaseCurrent() {
        makeCurrent(hDC, null)
    }

    private fun makeCurrent(hDC: HDC?, hRC: WinDef.HGLRC?) {
        if (hDC == null) return

        if (!WGL.wglMakeCurrent(hDC, hRC)) {
            val error = Win32.GetLastError()
            logger.error { "WGL.wglMakeCurrent($hDC, $hRC).error = $error" }
        }
    }

    override fun swapBuffers() {
        //println("swapBuffers")
        Win32.glFlush()
        Win32.SwapBuffers(hDC)
        //Thread.sleep(16L)
    }

    override fun dispose() {
        releaseCurrent()
        Win32.ReleaseDC(hWnd, hDC)
        WGL.wglDeleteContext(hRC)
    }

    private var wglSwapIntervalEXTSet: Boolean = false
    private var swapIntervalEXT: SwapIntervalCallback? = null
    private var swapIntervalEXTPointer: Pointer? = null

    interface SwapIntervalCallback : Callback {
        fun callback(value: Int)
    }

    private fun getSwapInterval(): SwapIntervalCallback? {
        if (!wglSwapIntervalEXTSet) {
            wglSwapIntervalEXTSet = true
            swapIntervalEXTPointer = WGL.wglGetProcAddress("wglSwapIntervalEXT")

            swapIntervalEXT = when {
                swapIntervalEXTPointer != Pointer.NULL -> CallbackReference.getCallback(SwapIntervalCallback::class.java, swapIntervalEXTPointer) as? SwapIntervalCallback?
                else -> null
            }
            println("swapIntervalEXT: $swapIntervalEXT")
        }
        return swapIntervalEXT
    }

    override fun supportsSwapInterval(): Boolean {
        return getSwapInterval() != null
    }

    override fun swapInterval(value: Int) {
        getSwapInterval()?.callback(value)
    }

    companion object {
        val logger = Logger("Win32OpenglContext")

        operator fun invoke(c: Component, gwconfig: GameWindowConfig, doubleBuffered: Boolean = false): Win32OpenglContext {
            val hWnd = WinDef.HWND(Native.getComponentPointer(c))
            return Win32OpenglContext(hWnd, gwconfig, doubleBuffered, c).init()
        }

        operator fun invoke(hWnd: WinDef.HWND, gwconfig: GameWindowConfig, doubleBuffered: Boolean = false, c: Component? = null): Win32OpenglContext {
            val hDC = Win32.GetDC(hWnd)
            return Win32OpenglContext(gwconfig, hWnd, hDC, doubleBuffered, c).init()
        }

        const val WGL_DRAW_TO_WINDOW_ARB            = 0x2001
        const val WGL_SUPPORT_OPENGL_ARB            = 0x2010
        const val WGL_DOUBLE_BUFFER_ARB             = 0x2011
        const val WGL_PIXEL_TYPE_ARB                = 0x2013
        const val WGL_COLOR_BITS_ARB                = 0x2014
        const val WGL_DEPTH_BITS_ARB                = 0x2022
        const val WGL_STENCIL_BITS_ARB              = 0x2023
        const val WGL_TYPE_RGBA_ARB                 = 0x202B
        const val GL_TRUE = 1
        const val GL_FALSE = 0
        const val WGL_CONTEXT_DEBUG_BIT_ARB =          0x00000001
        const val WGL_CONTEXT_FORWARD_COMPATIBLE_BIT_ARB =  0x00000002
        const val WGL_CONTEXT_MAJOR_VERSION_ARB =      0x2091
        const val WGL_CONTEXT_MINOR_VERSION_ARB =      0x2092
        const val WGL_CONTEXT_LAYER_PLANE_ARB =        0x2093
        const val WGL_CONTEXT_FLAGS_ARB =              0x2094
        const val ERROR_INVALID_VERSION_ARB =          0x2095
        const val WGL_CONTEXT_PROFILE_MASK_ARB      = 0x9126
        const val WGL_CONTEXT_CORE_PROFILE_BIT_ARB  = 0x00000001
        const val WGL_CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB = 0x00000002
        const val ERROR_INVALID_PROFILE_ARB         = 0x2096
        const val GL_NUM_EXTENSIONS = 0x821D
        const val WGL_SAMPLE_BUFFERS_ARB            = 0x2041
        const val WGL_SAMPLES_ARB                   = 0x2042
    }
}
