package com.soywiz.korgw.platform

import com.soywiz.korag.*
import com.soywiz.korgw.*
import com.soywiz.korgw.osx.*
import com.soywiz.korgw.win32.Win32OpenglContext
import com.soywiz.korgw.x11.X
import com.soywiz.korgw.x11.X11OpenglContext
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.OS
import com.soywiz.korma.geom.*
import com.sun.jna.Native
import com.sun.jna.platform.unix.X11
import java.awt.*
import java.lang.reflect.Method

interface BaseOpenglContext : Disposable {
    val isCore: Boolean get() = false
    val scaleFactor: Double get() = 1.0
    data class ContextInfo(
        val scissors: RectangleInt? = null,
        val viewport: RectangleInt? = null
    ) {
        companion object {
            val DEFAULT = ContextInfo()
        }
    }
    fun useContext(g: Graphics, ag: AG, action: (Graphics, ContextInfo) -> Unit) {
        makeCurrent()
        try {
            action(g, ContextInfo.DEFAULT)
        } finally {
            swapBuffers()
            releaseCurrent()
        }
    }

    fun isCurrent(): Boolean? = getCurrent() != null

    fun getCurrent(): Any? = null

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

/*
inline fun BaseOpenglContext.useContext(block: () -> Unit) {
    makeCurrent()
    try {
        block()
    } finally {
        swapBuffers()
        releaseCurrent()
    }
}
*/

object DummyOpenglContext : BaseOpenglContext {
    override fun makeCurrent() {
        println("WARNING: DummyOpenglContext.makeCurrent (using a Dummy implementation)")
    }

    override fun swapBuffers() {
    }
}

fun glContextFromComponent(c: Component, gwconfig: GameWindowConfig): BaseOpenglContext {
    return when {
        OS.isMac -> {
            try {
                MacAWTOpenglContext(gwconfig = gwconfig, c)
            } catch (e: Throwable) {
                e.printStackTrace()
                System.err.println("Might require run the JVM with --add-opens=java.desktop/sun.java2d.opengl=ALL-UNNAMED")
                DummyOpenglContext
            }
        }
        OS.isWindows -> Win32OpenglContext(c, gwconfig, doubleBuffered = true).init()
        else -> {
            try {
                val display = X.XOpenDisplay(null)
                val screen = X.XDefaultScreen(display)
                if (c is Frame) {
                    val contentWindow = c.awtGetPeer().reflective().dynamicInvoke("getContentWindow") as Long
                    X11OpenglContext(gwconfig, display, X11.Drawable(contentWindow), screen, doubleBuffered = true)
                } else {
                    val componentId = Native.getComponentID(c)
                    X11OpenglContext(gwconfig, display, X11.Drawable(componentId), screen, doubleBuffered = true)
                }
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

    val nativeScaleFactor = if (getScaleFactorMethod == null) null else {
        try {
            val scale: Any = getScaleFactorMethod.invoke(device)
            ((scale as? Number)?.toDouble()) ?: 1.0
        } catch (e: Throwable) {
            if (e::class.qualifiedName != "java.lang.IllegalAccessException") {
                e.printStackTrace()
            }
            null
        }
    }

    return nativeScaleFactor ?:
        ((component.graphicsConfiguration ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration).defaultTransform.scaleX)
}
