package korlibs.kgl

import com.sun.jna.*
import com.sun.jna.platform.win32.*
import korlibs.io.lang.*
import korlibs.logger.*
import korlibs.platform.Platform
import korlibs.memory.dyn.*
import korlibs.render.*
import korlibs.render.osx.*
import korlibs.render.platform.*
import korlibs.render.win32.*
import korlibs.render.x11.*
import java.util.concurrent.atomic.*

val GLOBAL_HEADLESS_KML_CONTEXT by lazy { KmlGlContextDefault() }

//val ENABLE_JVM_MAC_OPENGL41_CORE = true
val ENABLE_JVM_MAC_OPENGL41_CORE = Environment["ENABLE_JVM_MAC_OPENGL41_CORE"] == "true"

actual fun KmlGlContextDefault(window: Any?, parent: KmlGlContext?): KmlGlContext = when {
    Platform.isMac -> MacKmlGlContextRaw(window, parent)
    //Platform.isMac -> MacKmlGlContextManaged(window, parent)

    //Platform.isLinux -> LinuxKmlGlContext(window, parent)
    Platform.isLinux -> EGLKmlGlContext(window, parent)

    //Platform.isWindows -> Win32KmlGlContext(window, parent)
    Platform.isWindows -> Win32KmlGlContextManaged(window, parent)
    else -> error("Unsupported OS ${Platform.os}")
}

class Win32KmlGlContextManaged(window: Any? = null, parent: KmlGlContext? = null) : KmlGlContext(window, Win32KmlGl, parent) {
    val win = Win32DummyWindow()
    val glCtx = Win32OpenglContext(GameWindowConfig.Impl(), win.hWND, win.hDC).init()

    override fun set() = glCtx.makeCurrent()
    override fun unset() = glCtx.releaseCurrent()
    override fun swap() = glCtx.swapBuffers()
    override fun close() {
        glCtx.dispose()
        win.dispose()
    }
}

class Win32DummyWindow : Disposable {
    val hWND = Win32.CreateWindowEx(
        0, dummyName, "Dummy OpenGL Window",
        0, CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT,
        null, null, winClass.hInstance, null
    )
    val hDC = Win32.GetDC(hWND)

    companion object {
        val dummyName = "OffScreen_WGL_korgw"
        val CS_VREDRAW = 1
        val CS_HREDRAW = 2
        val CS_OWNDC = 0x0020
        val CW_USEDEFAULT = 0x80000000L.toInt()
        val winClass = WinUser.WNDCLASSEX().also {
            it.style = CS_HREDRAW or CS_VREDRAW or CS_OWNDC
            it.lpfnWndProc = WinUser.WindowProc { hwnd, uMsg, wParam, lParam -> Win32.DefWindowProc(hwnd, uMsg, wParam, lParam) }
            it.hInstance = Win32.GetModuleHandle(null)
            it.lpszClassName = dummyName
        }.also {
            Win32.RegisterClassEx(it)
        }
        //val hWND = HWND(Native.getWindowPointer(Window(Frame())))
    }

    override fun dispose() {
        Win32.ReleaseDC(hWND, hDC)
        Win32.DestroyWindow(hWND)
    }
}

open class Win32KmlGlContext(window: Any? = null, parent: KmlGlContext? = null) : KmlGlContext(window, Win32KmlGl, parent) {
    val dummyName = "OffScreen_WGL_korgw"
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
    init { Win32.RegisterClassEx(winClass) }
    //val hWND = HWND(Native.getWindowPointer(Window(Frame())))
    val hWND = Win32.CreateWindowEx(
        0, dummyName, "Dummy OpenGL Window",
        0, CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT,
        null, null, winClass.hInstance, null
    )
    val hDC = Win32.GetDC(hWND)

    val pf: Int = run {
        val attribList = intArrayOf(
            Win32OpenglContext.WGL_DRAW_TO_WINDOW_ARB, Win32OpenglContext.GL_TRUE,
            Win32OpenglContext.WGL_SUPPORT_OPENGL_ARB, Win32OpenglContext.GL_TRUE,
            Win32OpenglContext.WGL_DOUBLE_BUFFER_ARB, Win32OpenglContext.GL_TRUE,
            //WGL_DOUBLE_BUFFER_ARB, GL_FALSE,
            Win32OpenglContext.WGL_PIXEL_TYPE_ARB, Win32OpenglContext.WGL_TYPE_RGBA_ARB,
            Win32OpenglContext.WGL_COLOR_BITS_ARB, 32,
            Win32OpenglContext.WGL_DEPTH_BITS_ARB, 16,
            Win32OpenglContext.WGL_STENCIL_BITS_ARB, 8,
            Win32OpenglContext.WGL_SAMPLE_BUFFERS_ARB, 1, // Number of buffers (must be 1 at time of writing)
            Win32OpenglContext.WGL_SAMPLES_ARB, 4,        // Number of samples
            0, // End
        )
        val pixelFormatMemory = Memory(8L)
        val numFormatsMemory = Memory(8L)
        pixelFormatMemory.setInt(0L, 0)
        numFormatsMemory.setInt(0L, 0)
        Win32GL.wglChoosePixelFormatARB(hDC, attribList, null, 1, pixelFormatMemory, numFormatsMemory)
        if (numFormatsMemory.getInt(0) == 0) error("wglChoosePixelFormatARB: Can't get opengl formats")
        pixelFormatMemory.getInt(0)
    }

    val pfd: WinGDI.PIXELFORMATDESCRIPTOR.ByReference = WinGDI.PIXELFORMATDESCRIPTOR.ByReference().also { pfd ->
        Win32.DescribePixelFormat(hDC, pf, WinGDI.PIXELFORMATDESCRIPTOR().size(), pfd)
    }
    //println("PF: $pf, PFD: $pfd")

    //DescribePixelFormat(hDC, pf, sizeof(PIXELFORMATDESCRIPTOR), &pfd);
    init {
        Win32.SetPixelFormat(hDC, pf, pfd)
    }

    //val attribs = intArrayOf(
    //    WGL_CONTEXT_MAJOR_VERSION_ARB, 3,
    //    WGL_CONTEXT_MINOR_VERSION_ARB, 3
    //)

    val requestCoreProfile = false

    val hRC = Win32GL.wglCreateContextAttribsARB(hDC, null, intArrayOf(
        WGL_CONTEXT_MAJOR_VERSION_ARB, 3,
        WGL_CONTEXT_MINOR_VERSION_ARB, 3,
        WGL_CONTEXT_PROFILE_MASK_ARB, if (requestCoreProfile) WGL_CONTEXT_CORE_PROFILE_BIT_ARB else WGL_CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB,
        0
    )).also {
        logger.debug { "wglCreateContextAttribsARB.error: ${Native.getLastError()}" }
    }

    init {
        logger.debug { "hWND: $hWND, hDC: $hDC, hRC: $hRC" }
        set()
    }

    val extensions by lazy {
        (0 until Win32KmlGl.getIntegerv(Win32OpenglContext.GL_NUM_EXTENSIONS)).map {
            DirectGL.glGetStringi(Win32KmlGl.EXTENSIONS, it)
        }.toSet()
    }

    val isCore by lazy {
        !extensions.contains("GL_ARB_compatibility")
    }

    init {
        Win32KmlGl.apply {
            logger.debug { "GL_VERSION: ${Win32KmlGl.getString(Win32KmlGl.VERSION)}, GL_VENDOR: ${Win32KmlGl.getString(Win32KmlGl.VENDOR)}" }
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

    override fun set() {
        makeCurrent(hDC, hRC)
        /*
        when (gwconfig.quality) {
            GameWindow.Quality.QUALITY -> Win32GL.glEnable(GL_MULTISAMPLE)
            GameWindow.Quality.PERFORMANCE,
            GameWindow.Quality.AUTOMATIC -> Win32GL.glDisable(GL_MULTISAMPLE)
        }
        */
    }

    override fun unset() {
        makeCurrent(null, null)
    }

    private fun makeCurrent(hDC: WinDef.HDC?, hRC: WinDef.HGLRC?) {
        if (!WGL.wglMakeCurrent(hDC, hRC)) {
            val error = Win32.GetLastError()
            logger.error { "WGL.wglMakeCurrent($hDC, $hRC).error = $error" }
        }
    }

    override fun swap() {
        Win32.glFlush()
        Win32.SwapBuffers(hDC)
    }

    override fun close() {
        unset()
        Win32.ReleaseDC(hWND, hDC)
        WGL.wglDeleteContext(hRC)
    }

    companion object {
        val logger = Logger("Win32KmlGlContext")

        const val GL_MULTISAMPLE = 0x809D

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

// http://renderingpipeline.com/2012/05/windowless-opengl/
open class X11KmlGlContext(window: Any? = null, parent: KmlGlContext? = null) : KmlGlContext(window, MacKmlGl(), parent) {
    init {
        val display = X.XOpenDisplay(null)
        //X.glXChooseFBConfig()
        /*
        val display = EGL.eglGetDisplay(0)
        val majorPtr = Memory(8).also { it.clear() }
        val minorPtr = Memory(8).also { it.clear() }
        val initialize = EGL.eglInitialize(display, majorPtr, minorPtr)
        if (!initialize) error("Can't initialize EGL")
        val major = majorPtr.getInt(0)
        val minor = minorPtr.getInt(0)
        println("display=$display, initialize=$initialize, major=${major}, minor=${minor}")
        val attrib_list = Memory()
        EGL.eglChooseConfig(display, attrib_list)
        //X.XBlackPixel(null, 0)
         */

        TODO()
    }
}

// https://forums.developer.nvidia.com/t/egl-without-x11/58733
open class EGLKmlGlContext(window: Any? = null, parent: KmlGlContext? = null) : KmlGlContext(window, MacKmlGl(), parent) {
    val display: Pointer? = run {
        EGL.eglGetDisplay(0).also {
            if (it == null) error("Can't get EGL main display. Try setting env DISPLAY=:0 ?")
        }
    }

    val eglCfg: Pointer? = run {
        val majorPtr = Memory(8).also { it.clear() }
        val minorPtr = Memory(8).also { it.clear() }
        val initialize = EGL.eglInitialize(display, majorPtr, minorPtr)
        INIT_COUNT.incrementAndGet()
        val major = majorPtr.getInt(0)
        val minor = minorPtr.getInt(0)
        if (!initialize) error("Can't initialize EGL : errorCode=${EGL.eglGetError()}, display=$display, major=$major, minor=$minor, initCount=${INIT_COUNT.get()}, terminateCount=${TERMINATE_COUNT.get()}")
        //println("display=$display, initialize=$initialize, major=${major}, minor=${minor}")

        //val attrib_list = Memory()
        val eglCfgPtr = Memory(8)
        val numConfigsPtr = Memory(4)

        EGL.eglChooseConfig(display, Memory(intArrayOf(
            EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
            EGL_ALPHA_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_RED_SIZE, 8,
            EGL_DEPTH_SIZE, 16,
            EGL_STENCIL_SIZE, 8,
            EGL_RENDERABLE_TYPE, EGL_OPENGL_BIT,
            EGL_NONE
        )), eglCfgPtr, 1, numConfigsPtr)

        val numConfigs = numConfigsPtr.getInt(0)
        if (numConfigs != 1) error("Failed to choose exactly 1 config")

        eglCfgPtr.getPointer(0L)
    }

    val eglCtx: Pointer? = run {
        EGL.eglBindAPI(EGL_OPENGL_API)
        EGL.eglCreateContext(display, eglCfg, (parent as? EGLKmlGlContext?)?.eglCtx, null)
    }

    val eglSurface = EGL.eglCreatePbufferSurface(display, eglCfg, Memory(intArrayOf(
        EGL_WIDTH, 1024,
        EGL_HEIGHT, 1024,
        EGL_NONE,
    )))

    override fun set() {
        EGL.eglMakeCurrent(display, eglSurface, eglSurface, eglCtx)
    }

    override fun unset() {
        EGL.eglMakeCurrent(display, eglSurface, eglSurface, null)
    }

    override fun swap() {
        EGL.eglSwapBuffers(display, eglSurface)
    }

    override fun close() {
        EGL.eglDestroyContext(display, eglCtx);
        EGL.eglDestroySurface(display, eglSurface)
        EGL.eglTerminate(display)
        TERMINATE_COUNT.incrementAndGet()
    }

    companion object {
        val INIT_COUNT = AtomicInteger(0)
        val TERMINATE_COUNT = AtomicInteger(0)

        const val EGL_PBUFFER_BIT                   = 0x0001
        const val EGL_OPENGL_BIT                    = 0x0008

        const val EGL_ALPHA_SIZE                    = 0x3021
        const val EGL_BLUE_SIZE                     = 0x3022
        const val EGL_GREEN_SIZE                    = 0x3023
        const val EGL_RED_SIZE                      = 0x3024
        const val EGL_DEPTH_SIZE                    = 0x3025
        const val EGL_STENCIL_SIZE                  = 0x3026

        const val EGL_SURFACE_TYPE                  = 0x3033
        const val EGL_TRANSPARENT_TYPE              = 0x3034
        const val EGL_TRANSPARENT_BLUE_VALUE        = 0x3035
        const val EGL_TRANSPARENT_GREEN_VALUE       = 0x3036
        const val EGL_TRANSPARENT_RED_VALUE         = 0x3037
        const val EGL_NONE                          = 0x3038

        const val EGL_RENDERABLE_TYPE               = 0x3040

        const val EGL_TRANSPARENT_RGB               = 0x3052
        const val EGL_HEIGHT                        = 0x3056
        const val EGL_WIDTH                         = 0x3057

        const val EGL_OPENGL_API                    = 0x30A2

    }
}


private interface CoreGraphics : Library {
    fun CGMainDisplayID(): Int
    companion object : CoreGraphics by NativeLoad("/System/Library/Frameworks/CoreGraphics.framework/Versions/A/CoreGraphics")
}

open class MacKmlGlContextManaged(window: Any? = null, parent: KmlGlContext? = null) : KmlGlContext(window, MacKmlGl(), parent) {
    val glCtx: MacosGLContext = MacosGLContext(sharedContext = (parent as MacKmlGlContextManaged?)?.glCtx?.openGLContext ?: 0L)

    override fun set() = glCtx.makeCurrent()
    override fun unset() = glCtx.releaseCurrent()
    override fun swap() = glCtx.swapBuffers()
    override fun close() = glCtx.dispose()

    //override fun set() = Unit
    //override fun unset() = Unit
    //override fun swap() = Unit
    //override fun close() = Unit
}


// http://renderingpipeline.com/2012/05/windowless-opengl-on-macos-x/
open class MacKmlGlContextRaw(window: Any? = null, parent: KmlGlContext? = null) : KmlGlContext(window, MacKmlGl(), parent) {
    var ctx: com.sun.jna.Pointer? = run {
        val ctx = Memory(8L).also { it.clear() } // void**
        checkError("CGLCreateContext", MacGL.CGLCreateContext(pix, (parent as? MacKmlGlContextRaw)?.ctx, ctx))
        ctx.getPointer(0L)
    }

    override fun set() {
        checkError("CGLSetCurrentContext:ctx", MacGL.CGLSetCurrentContext(ctx))
    }

    override fun unset() {
        checkError("CGLSetCurrentContext:null", MacGL.CGLSetCurrentContext(null))
    }

    override fun swap() {
        //super.swap()
    }

    override fun close() {
        if (ctx == null) return
        if (MacGL.CGLGetCurrentContext() == ctx) {
            checkError("CGLSetCurrentContext:null", MacGL.CGLSetCurrentContext(null))
        }
        checkError("CGLDestroyContext", MacGL.CGLDestroyContext(ctx))
        //println("CGLDestroyContext")
        ctx = null
    }

    companion object {

        val pix: Pointer? by lazy {
            //initializeMacOnce()
            //CoreGraphics.CGMainDisplayID()
            //NSClass("NSOpenGLPixelFormat").alloc().msgSend("initWithAttributes:", intArrayOf(0)) // This might help initializing CoreGraphics?

            //checkError("CGLDestroyPixelFormat", MacGL.CGLDestroyPixelFormat(pix))

            fun formatsProvider(): Sequence<IntArray> = sequence<IntArray> {
                for (glVersion in (if (ENABLE_JVM_MAC_OPENGL41_CORE) listOf(410, 320, 210) else listOf(210))) {
                //for (glVersion in listOf(210)) {
                    for (extra in listOf(true, false)) {
                        for (accelerated in listOf(true, false)) {
                            yield(buildList {
                                // Let's not specify profile version, so we are using old shader syntax
                                //add(kCGLPFAOpenGLProfile); add(kCGLOGLPVersion_GL3_Core)
                                add(kCGLPFAOpenGLProfile)
                                add(when (glVersion) {
                                    410 -> kCGLOGLPVersion_GL4_Core
                                    320 -> kCGLOGLPVersion_GL3_Core
                                    else -> kCGLOGLPVersion_Legacy
                                })
                                if (accelerated) add(kCGLPFAAccelerated)
                                add(kCGLPFAAllowOfflineRenderers)
                                if (extra) {
                                    add(kCGLPFAColorSize); add(24)
                                    add(kCGLPFADepthSize); add(16)
                                    add(kCGLPFAStencilSize); add(8)
                                }
                                //kCGLPFADoubleBuffer,
                                //kCGLPFASupersample,
                                add(0)
                            }.toIntArray())
                        }
                    }
                }
            }

            val num = Memory(4L).also { it.clear() }
            val pix = Memory(8L).also { it.clear() } // void**
            val exceptions = arrayListOf<Throwable>()
            for (format in formatsProvider()) {
                val attributes = Memory(format)
                try {
                    //println("ctx=${MacGL.CGLGetCurrentContext()}")
                    checkError("CGLChoosePixelFormat", MacGL.CGLChoosePixelFormat(attributes, pix, num).also {
                        //println("CGLChoosePixelFormat: num=${num.getInt(0L)}, pix=${pix.getPointer(0L)}")
                    })
                    return@lazy pix.getPointer(0L)
                } catch (e: Throwable) {
                    exceptions += e
                    e.printStackTrace()
                }
            }
            if (exceptions.isNotEmpty()) {
                for (e in exceptions) {
                    e.printStackTrace()
                }
            }
            return@lazy null
        }

        private fun checkError(name: String, value: Int): Int {
            val error = MacGL.Error[value]
            if (error != MacGL.Error.kCGLNoError) error("Error in $name, errorCode=$value : $error")
            return value
        }

        // https://github.com/apitrace/apitrace/blob/master/retrace/glretrace_cgl.cpp
        const val kCGLPFAAccelerated = 73
        const val kCGLPFAAllowOfflineRenderers = 96
        const val kCGLPFAOpenGLProfile = 99
        //const val kCGLOGLPVersion_GL4_Core = 16640

        const val kCGLOGLPVersion_Legacy = 0x1000
        const val kCGLOGLPVersion_3_2_Core = 0x3200
        const val kCGLOGLPVersion_GL3_Core = 0x3200
        const val kCGLOGLPVersion_GL4_Core = 0x4100


        const val kCGLPFAColorSize    = 8
        const val kCGLPFAAlphaSize    = 11
        const val kCGLPFADepthSize    = 12
        const val kCGLPFAStencilSize  = 13
        const val kCGLPFAAccumSize    = 14
        const val kCGLPFADoubleBuffer = 5

    }
}
