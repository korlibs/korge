package com.soywiz.korgw.win32

import com.soywiz.korgw.platform.*
import com.soywiz.korim.bitmap.*
import com.sun.jna.*
import com.sun.jna.Function
import com.sun.jna.platform.win32.*
import com.sun.jna.platform.win32.WinDef.*
import com.sun.jna.ptr.*
import java.lang.reflect.*

object Win32KmlGl : NativeKgl(Win32GL)

interface Win32GL : INativeGL, Library {
    companion object : Win32GL by Win32GL.OpenglLoadProxy() {
        private fun OpenglLoadProxy(): Win32GL {
            val funcs = LinkedHashMap<Method, Function>()
            val classLoader = Win32KmlGl::class.java.classLoader
            val opengl32Lib = NativeLibrary.getInstance("opengl32")
            return Proxy.newProxyInstance(
                classLoader,
                arrayOf(Win32GL::class.java)
            ) { obj: Any?, method: Method, args: Array<Any?>? ->
                val func = funcs.getOrPut(method) {
                    OpenGL32.INSTANCE.wglGetProcAddress(method.name)?.let { Function.getFunction(it) }
                        ?: try {
                            opengl32Lib.getFunction(method.name)
                        } catch (e: UnsatisfiedLinkError) {
                            null
                        }
                        ?: error("Can't find opengl method ${method.name}")

                }
                func.invoke(method.returnType, args)
            } as Win32GL
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
    for (n in 0 until bmp.data.size) {
        bitsPtr.setInt((n * 4).toLong(), bmp.data[n].value)
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

class Win32OpenglContext(val hDC: WinDef.HDC, val doubleBuffered: Boolean = false) : BaseOpenglContext {
    constructor(hWnd: WinDef.HWND, doubleBuffered: Boolean = false) : this(Win32.GetDC(hWnd), doubleBuffered)

    val pfd = WinGDI.PIXELFORMATDESCRIPTOR.ByReference()

    init {
        pfd.nSize = pfd.size().toShort()
        pfd.nVersion = 1
        pfd.dwFlags =
            WinGDI.PFD_DRAW_TO_WINDOW or WinGDI.PFD_SUPPORT_OPENGL or (if (doubleBuffered) WinGDI.PFD_DOUBLEBUFFER else 0)
        //pfd.dwFlags = WinGDI.PFD_DRAW_TO_WINDOW or WinGDI.PFD_SUPPORT_OPENGL;
        pfd.iPixelType = WinGDI.PFD_TYPE_RGBA.toByte()
        pfd.cColorBits = 32
        //pfd.cColorBits = 24
        pfd.cDepthBits = 16
    }

    val pf = Win32.ChoosePixelFormat(hDC, pfd)

    init {
        Win32.SetPixelFormat(hDC, pf, pfd)
        //DescribePixelFormat(hDC, pf, sizeof(PIXELFORMATDESCRIPTOR), &pfd);

        //val attribs = intArrayOf(
        //    WGL_CONTEXT_MAJOR_VERSION_ARB, 3,
        //    WGL_CONTEXT_MINOR_VERSION_ARB, 3
        //)
    }

    //hRC = wglCreateContextAttribsARB (hDC, null, attribs);
    val hRC = Win32.wglCreateContext(hDC)

    init {
        makeCurrent()
        println("GL_VERSION: " + Win32KmlGl.getString(Win32KmlGl.VERSION))
        println("GL_VENDOR: " + Win32KmlGl.getString(Win32KmlGl.VENDOR))
    }

    override fun makeCurrent() {
        //println("makeCurrent")
        Win32.wglMakeCurrent(hDC, hRC)
    }

    override fun releaseCurrent() {
        Win32.wglMakeCurrent(null, null)
    }

    override fun swapBuffers() {
        //println("swapBuffers")
        Win32.glFlush()
        Win32.SwapBuffers(hDC)
        //Thread.sleep(16L)
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
            swapIntervalEXTPointer = Win32.wglGetProcAddress("wglSwapIntervalEXT")

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
}
