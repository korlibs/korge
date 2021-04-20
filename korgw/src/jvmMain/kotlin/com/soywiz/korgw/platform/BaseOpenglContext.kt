package com.soywiz.korgw.platform

import com.soywiz.korag.*
import com.soywiz.korgw.awt.*
import com.soywiz.korgw.win32.Win32OpenglContext
import com.soywiz.korgw.x11.X
import com.soywiz.korgw.x11.X11OpenglContext
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.OS
import com.soywiz.korma.awt.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.Rectangle
import com.sun.jna.Native
import com.sun.jna.platform.unix.X11
import com.sun.jna.platform.win32.WinDef
import sun.awt.*
import java.awt.*
import java.lang.reflect.Method
import java.security.*
import javax.swing.*

interface BaseOpenglContext : Disposable {
    val isCore: Boolean get() = false
    val scaleFactor: Double get() = 1.0
    class ContextInfo(
        val scissors: com.soywiz.korma.geom.RectangleInt? = null,
        val viewport: com.soywiz.korma.geom.RectangleInt? = null
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
    }

    override fun swapBuffers() {
    }
}

private inline fun <T : Any> privilegedAction(crossinline block: () -> T): T {
    var result: T? = null
    AccessController.doPrivileged(PrivilegedAction {
        result = block()
    })
    return result!!
}

fun glContextFromComponent(c: Component): BaseOpenglContext {
    return when {
        OS.isMac -> {
            val utils = privilegedAction { Class.forName("sun.java2d.opengl.OGLUtilities") }
            //println(utils.declaredMethods.map { it.name })
            val invokeWithOGLContextCurrentMethod = privilegedAction {
                utils.getDeclaredMethod("invokeWithOGLContextCurrent", Graphics::class.java, Runnable::class.java).also { it.isAccessible = true }
            }
            val isQueueFlusherThread = privilegedAction {
                utils.getDeclaredMethod("isQueueFlusherThread").also { it.isAccessible = true }
            }
            val getOGLViewport = privilegedAction {
                utils.getDeclaredMethod("getOGLViewport", Graphics::class.java, Integer.TYPE, Integer.TYPE).also { it.isAccessible = true }
            }
            val getOGLScissorBox = privilegedAction {
                utils.getDeclaredMethod("getOGLScissorBox", Graphics::class.java).also { it.isAccessible = true }
            }
            val getOGLSurfaceIdentifier = privilegedAction {
                utils.getDeclaredMethod("getOGLSurfaceIdentifier", Graphics::class.java).also { it.isAccessible = true }
            }
            val getOGLSurfaceType = privilegedAction {
                utils.getDeclaredMethod("getOGLSurfaceType", Graphics::class.java).also { it.isAccessible = true }
            }

            val info = BaseOpenglContext.ContextInfo(
                RectangleInt(), RectangleInt()
            )

            //var timeSinceLast = 0L
            object : BaseOpenglContext {
                override val scaleFactor: Double get() = getDisplayScalingFactor(c)

                override fun useContext(g: Graphics, ag: AG, action: (Graphics, BaseOpenglContext.ContextInfo) -> Unit) {
                    invokeWithOGLContextCurrentMethod.invoke(null, g, Runnable {
                        //if (!(isQueueFlusherThread.invoke(null) as Boolean)) error("Can't render on another thread")
                        try {
                            val factor = getDisplayScalingFactor(c)
                            //val window = SwingUtilities.getWindowAncestor(c)
                            val viewport = getOGLViewport.invoke(null, g, (c.width * factor).toInt(), (c.height * factor).toInt()) as java.awt.Rectangle
                            //val viewport = getOGLViewport.invoke(null, g, window.width.toInt(), window.height.toInt()) as java.awt.Rectangle
                            val scissorBox = getOGLScissorBox(null, g) as? java.awt.Rectangle?
                            //println("scissorBox: $scissorBox")
                            //println("viewport: $viewport")
                            //info.scissors?.setTo(scissorBox.x, scissorBox.y, scissorBox.width, scissorBox.height)
                            if (scissorBox != null) {
                                info.scissors?.setTo(scissorBox.x, scissorBox.y, scissorBox.width, scissorBox.height)
                                //info.viewport?.setTo(viewport.x, viewport.y, viewport.width, viewport.height)
                                info.viewport?.setTo(scissorBox.x, scissorBox.y, scissorBox.width, scissorBox.height)
                            } else {
                                System.err.println("ERROR !! scissorBox = $scissorBox, viewport = $viewport")
                            }
                            //info.viewport?.setTo(scissorBox.x, scissorBox.y)
                            //println("viewport: $viewport, $scissorBox")
                            //println(g.clipBounds)
                            action(g, info)

                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    })
                }
                override fun makeCurrent() = Unit
                override fun releaseCurrent() = Unit
                override fun swapBuffers() = Unit
            }
        }
        OS.isWindows -> Win32OpenglContext(c, doubleBuffered = true).init()
        else -> {
            try {
                val display = X.XOpenDisplay(null)
                val screen = X.XDefaultScreen(display)
                if (c is Frame) {
                    val contentWindow = c.awtGetPeer().reflective().dynamicInvoke("getContentWindow") as Long
                    X11OpenglContext(display, X11.Drawable(contentWindow), screen, doubleBuffered = true)
                } else {
                    val componentId = Native.getComponentID(c)
                    X11OpenglContext(display, X11.Drawable(componentId), screen, doubleBuffered = true)
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
    return if (getScaleFactorMethod != null) {
        val scale: Any = getScaleFactorMethod.invoke(device)
        ((scale as? Number)?.toDouble()) ?: 1.0
    } else {
        (component.graphicsConfiguration ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration).defaultTransform.scaleX
    }
}
