package com.soywiz.korgw.platform

import com.soywiz.korgw.win32.Win32OpenglContext
import com.soywiz.korgw.x11.X
import com.soywiz.korgw.x11.X11OpenglContext
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.OS
import com.sun.jna.Native
import com.sun.jna.platform.unix.X11
import com.sun.jna.platform.win32.WinDef
import java.awt.Component
import java.awt.Graphics
import java.awt.GraphicsEnvironment
import java.lang.reflect.Method

interface BaseOpenglContext : Disposable {
    val scaleFactor: Double get() = 1.0
    fun useContext(obj: Any?, action: Runnable) {
        makeCurrent()
        try {
            action.run()
        } finally {
            swapBuffers()
            releaseCurrent()
        }
    }

    fun makeCurrent()
    fun releaseCurrent() {
    }
    fun swapBuffers() {
    }
    fun supportsSwapInterval(): Boolean = false
    fun swapInterval(value: Int) {
    }

    override fun dispose() = Unit
}

inline fun BaseOpenglContext.useContext(block: () -> Unit) {
    makeCurrent()
    try {
        block()
    } finally {
        swapBuffers()
        releaseCurrent()
    }
}

object DummyOpenglContext : BaseOpenglContext {
    override fun makeCurrent() {
    }

    override fun swapBuffers() {
    }
}

fun glContextFromComponent(c: Component): BaseOpenglContext {
    return when {
        OS.isMac -> {
            val utils = Class.forName("sun.java2d.opengl.OGLUtilities")
            val invokeWithOGLContextCurrentMethod = utils.getDeclaredMethod(
                "invokeWithOGLContextCurrent",
                Graphics::class.java, Runnable::class.java
            )
            invokeWithOGLContextCurrentMethod.isAccessible = true

            //var timeSinceLast = 0L
            object : BaseOpenglContext {
                override val scaleFactor: Double get() = getDisplayScalingFactor(c)

                override fun useContext(obj: Any?, action: Runnable) {
                    invokeWithOGLContextCurrentMethod.invoke(null, obj as Graphics, action)
                }
                override fun makeCurrent() = Unit
                override fun releaseCurrent() = Unit
                override fun swapBuffers() = Unit
            }
        }
        OS.isWindows -> Win32OpenglContext(
            WinDef.HWND(Native.getComponentPointer(c)),
            doubleBuffered = true
        )
        else -> {
            try {
                val display = X.XOpenDisplay(null)
                val screen = X.XDefaultScreen(display)
                val componentId = Native.getComponentID(c)
                X11OpenglContext(display, X11.Drawable(componentId), screen, doubleBuffered = true)
            } catch (e: Throwable) {
                e.printStackTrace()
                throw e
            }
        }
    }
}

fun getDisplayScalingFactor(component: Component): Double {
    val device = (component.graphicsConfiguration?.device ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice)
    val getScaleFactorMethod: Method? = try { device.javaClass.getMethod("getScaleFactor") } catch (e: Throwable) { null }
    return if (getScaleFactorMethod != null) {
        val scale: Any = getScaleFactorMethod.invoke(device)
        ((scale as? Number)?.toDouble()) ?: 1.0
    } else {
        (component.graphicsConfiguration ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration).defaultTransform.scaleX
    }
}
