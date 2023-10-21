package korlibs.render.x11

import com.sun.jna.*
import com.sun.jna.platform.unix.*
import com.sun.jna.ptr.*
import korlibs.ffi.*
import korlibs.graphics.shader.gl.*
import korlibs.io.annotations.*
import korlibs.render.platform.*

typealias XVisualInfo = Pointer
typealias GLXContext = Pointer

// https://www.khronos.org/registry/OpenGL/api/GL/glxext.h

internal const val GLX_RENDER_TYPE                  = 0x8011
internal const val GLX_RGBA_TYPE                    = 0x8014
internal const val GLX_RGBA_BIT                     = 0x00000001

internal const val GLX_RGBA = 4
internal const val GLX_RED_SIZE	= 8
internal const val GLX_GREEN_SIZE = 9
internal const val GLX_BLUE_SIZE = 10
internal const val GLX_ALPHA_SIZE = 11
internal const val GLX_DEPTH_SIZE = 12
internal const val GLX_STENCIL_SIZE = 13
internal const val GLX_DOUBLEBUFFER = 5

//internal fun FFIStructure.display() = pointer<X11.Display?>()
//internal fun FFIStructure.window() = pointer<X11.Window?>()

private fun FFIStructure.display() = pointer()
private fun FFIStructure.window() = pointer()

internal class XConfigureEvent(p: FFIPointer? = null) : FFIStructure(p) {
    var type by int()
    var serial by nativeLong()
    var send_event by int()
    var display by display()
    var event by window()
    var window by window()
    var x by int()
    var y by int()
    var width by int()
    var height by int()
    var border_width by int()
    var above by window()
    var override_redirect by int()
}

internal class XKeyEvent(p: FFIPointer? = null) : FFIStructure(p) {
    var type by int()
    var serial by nativeLong()
    var send_event by int()
    var display by display()
    var window by window()
    var root by window()
    var subwindow by window()
    var time by nativeLong()
    var x by int()
    var y by int()
    var x_root by int()
    var y_root by int()
    var state by int()
    var keycode by int()
    var same_screen by int()
}

internal class MyXMotionEvent(p: FFIPointer? = null) : FFIStructure(p) {
    var type by int()
    var serial by nativeLong()
    var send_event by int()
    var display by display()
    var window by window()
    var root by window()
    var subwindow by window()
    var time by nativeLong()
    var x by int()
    var y by int()
    var x_root by int()
    var y_root by int()
    var state by int()
    var button by int()
    var same_screen by int()
}

@Keep
object EGL {
    external fun eglGetDisplay(displayType: Long): Pointer?
    external fun eglInitialize(display: Pointer?, major: Pointer?, minor: Pointer?): Boolean
    external fun eglGetError(): Int
    external fun eglTerminate(display: Pointer?): Pointer
    external fun eglChooseConfig(display: Pointer?, attribList: Pointer, configs: Pointer, configSize: Int, numConfig: Pointer): Boolean
    external fun eglCreatePbufferSurface(display: Pointer?, config: Pointer?, attribList: Pointer?): Pointer?
    external fun eglBindAPI(api: Int)
    external fun eglCreateContext(display: Pointer?, config: Pointer?, shareContext: Pointer?, attribList: Pointer?): Pointer?
    external fun eglMakeCurrent(display: Pointer?, draw: Pointer?, read: Pointer?, context: Pointer?): Boolean
    external fun eglDestroyContext(display: Pointer?, context: Pointer?): Boolean
    external fun eglDestroySurface(display: Pointer?, surface: Pointer?)
    external fun eglSwapBuffers(display: Pointer?, eglSurface: Pointer?): Boolean

    init {
        Native.register("EGL")
    }
}

object X :
    X11Impl by Native.load(System.getenv("X11LIB_PATH") ?: "libX11", X11Impl::class.java),
    GL by Native.load(System.getenv("GLLIB_PATH") ?: "libGL", GL::class.java)

internal interface X11Impl : X11 {
    fun XCreateWindow(
        display: X11.Display, parent: X11.Window,
        x: Int, y: Int, width: Int, height: Int,
        border_width: Int, depth: Int, clazz: Int, visual: X11.Visual,
        valuemask: NativeLong,
        attributes: X11.XSetWindowAttributes
    ): X11.Window
    //Window XCreateSimpleWindow(Display display, Window parent, int x, int y,
    //int width, int height, int border_width,
    //int border, int background);
    fun XDefaultGC(display: X11.Display?, scn: Int): X11.GC?
    //fun XBlackPixel(display: X11.Display?, scn: Int): Int
    //fun XWhitePixel(display: X11.Display?, scn: Int): Int
    fun XBlackPixel(display: X11.Display?, scn: Int): Int = 0
    fun XWhitePixel(display: X11.Display?, scn: Int): Int = -1

    fun XStoreName(display: X11.Display?, w: X11.Window?, window_name: String)
    fun XSetIconName(display: X11.Display?, w: X11.Window?, window_name: String)
    fun XLookupKeysym(e: X11.XEvent?, i: Int): Int
    fun XDisplayString(display: X11.Display?): String?
    fun XSynchronize(display: X11.Display?, value: Boolean)
}

internal interface GL : INativeGL, Library {
    fun glXQueryDrawable(dpy: X11.Display, draw: X11.Drawable?, attribute: Int, value: IntByReference): Int
    fun glXQueryContext(dpy: X11.Display, ctx: GLXContext?, attribute: Int, value: Pointer): Int

    //fun glClearColor(r: Float, g: Float, b: Float, a: Float)
    //fun glClear(flags: Int)
    //fun glGetString(id: Int): String
    //fun glViewport(x: Int, y: Int, width: Int, height: Int)
    fun glXChooseVisual(display: X11.Display?, screen: Int, attribList: LongArray): XVisualInfo?
    fun glXChooseVisual(display: X11.Display?, screen: Int, attribList: IntArray): XVisualInfo?
    fun glXChooseVisual(display: X11.Display?, screen: Int, attribList: Pointer): XVisualInfo?
    fun glXCreateContext(display: X11.Display?, vis: XVisualInfo?, shareList: GLXContext?, direct: Boolean): GLXContext?
    fun glXDestroyContext(display: X11.Display?, context: GLXContext?): Unit
    fun glXMakeCurrent(display: X11.Display?, drawable: X11.Drawable?, ctx: GLXContext?): Boolean
    fun glXMakeContextCurrent(display: X11.Display?, draw: X11.Drawable?, read: X11.Drawable?, ctx: GLXContext?): Boolean
    fun glXSwapBuffers(display: X11.Display?, drawable: X11.Drawable?)
    fun glXGetProcAddress(name: String): Pointer
    fun glXGetCurrentDrawable(): Pointer
    fun glXGetCurrentDisplay(): X11.Display?

    //fun glXChooseVisual(display: X11.Display, screen: Int, attribList: IntArray): XVisualInfo
    //fun glXCreateContext(display: X11.Display, vis: XVisualInfo, shareList: GLXContext?, direct: Boolean): GLXContext
    //fun glXMakeCurrent(display: X11.Display, drawable: X11.Window, ctx: GLXContext?): Boolean
    //fun glXSwapBuffers(display: X11.Display, drawable: X11.Window)


    companion object {
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

//internal object X11KmlGl : NativeKgl(X)

open class X11KmlGl : NativeKgl(X) {
    override val variant: GLVariant get() = GLVariant.JVM_X11
}

interface glXSwapIntervalEXTCallback : Callback {
    fun callback(dpy: X11.Display?, draw: Pointer, value: Int)
}
