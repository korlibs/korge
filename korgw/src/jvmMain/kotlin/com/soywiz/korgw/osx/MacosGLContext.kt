package com.soywiz.korgw.osx

import com.soywiz.kgl.*
import com.soywiz.korag.*
import com.soywiz.korag.gl.*
import com.soywiz.korgw.*
import com.soywiz.korgw.platform.*
import com.soywiz.korio.dynamic.*
import com.soywiz.korma.geom.*
import java.awt.*
import java.security.*
import javax.swing.SwingUtilities

class MacosGLContext(
    var contentView: Long = 0L,
    val window: Long = 0L,
    val quality: GameWindow.Quality = GameWindow.Quality.AUTOMATIC,
    val sharedContext: Long = 0L,
) : BaseOpenglContext {
    companion object {
        const val NSOpenGLPFAMultisample = 59
        const val NSOpenGLPFASampleBuffers = 55
        const val NSOpenGLPFASamples = 56
        const val NSOpenGLPFADoubleBuffer = 5
        const val NSOpenGLPFAColorSize = 8
        const val NSOpenGLPFAAlphaSize = 11
        const val NSOpenGLPFADepthSize = 12
        const val NSOpenGLPFAStencilSize = 13
        const val NSOpenGLPFAAccumSize = 14
    }

    val attrs: IntArray by lazy {
        val antialias = (this.quality != GameWindow.Quality.PERFORMANCE)
        val antialiasArray = if (antialias) intArrayOf(
            NSOpenGLPFAMultisample,
            NSOpenGLPFASampleBuffers, 1,
            NSOpenGLPFASamples, 4
        ) else intArrayOf()
        intArrayOf(
            *antialiasArray,
            //NSOpenGLPFAOpenGLProfile,
            //NSOpenGLProfileVersion4_1Core,
            NSOpenGLPFADoubleBuffer,
            NSOpenGLPFAColorSize, 24,
            NSOpenGLPFAAlphaSize, 8,
            NSOpenGLPFADepthSize, 24,
            NSOpenGLPFAStencilSize, 8,
            NSOpenGLPFAAccumSize, 0,
            0
        )
    }

    val NSThread = NSClass("NSThread")
    val NSObject = NSClass("NSObject")
    val pixelFormat = NSClass("NSOpenGLPixelFormat").alloc().msgSend("initWithAttributes:", attrs)
    val openGLContext = NSClass("NSOpenGLContext").alloc().msgSend("initWithFormat:shareContext:", pixelFormat, sharedContext)

    init {
        //println("pixelFormat: $pixelFormat")
        //println("openGLContext: $openGLContext")
        if (contentView != 0L) setView(contentView)
    }

    override val scaleFactor: Double get() = if (window != 0L) window.msgSendCGFloat("backingScaleFactor").toDouble() else 1.0

    override fun makeCurrent() {
        openGLContext.msgSend("makeCurrentContext")
        //println("MacosGLContext.makeCurrentContext: ${openGLContext.msgSend("view")}")
    }

    override fun swapBuffers() {
        GL.glFlush()
        openGLContext.msgSend("flushBuffer")
    }

    fun clearDrawable() {
        openGLContext.msgSend("clearDrawable")
    }



    fun setView(contentView: Long) {
        runOnMainThread {
            println("MacosGLContext.setView: $contentView")
            openGLContext.msgSend("setView:", contentView)

            this.contentView = openGLContext.msgSend("view")

            //println(contentView.msgSendNSRect("frame"))
            //println(contentView.msgSend("window").msgSendNSRect("frame"))
        }
    }

    var callback: (() -> Unit)? = null
    val isMainThread: Boolean get() = NSThread.msgSend("isMainThread") != 0L
    val MyThreadExecutor = AllocateClassAndRegister("MyThreadExecutor", "NSObject") {
        addMethod("main:", ObjcCallbackVoid { self, _sel, sender ->
            //println("MyThreadExecutor")
            callback?.invoke()
            callback = null
        }, "v@:@")
    }
    val myThreadExecutorInstance = MyThreadExecutor.alloc().msgSend("init")

    fun runOnMainThread(block: () -> Unit) {
        if (isMainThread) {
            block()
        } else {
            synchronized(this) {
                //println("isMainThread: $isMainThread")
                callback = block
                myThreadExecutorInstance.msgSend(
                    "performSelectorOnMainThread:withObject:waitUntilDone:",
                    sel("main:"),
                    null,
                    1
                )
            }
        }
    }

    override fun useContext(g: Graphics, ag: AG, action: (Graphics, BaseOpenglContext.ContextInfo) -> Unit) {
        //runOnMainThread {
        run {
            makeCurrent()
            try {
                val factor = 2.0 // @TODO:
                val frame = contentView.msgSendNSRect("frame")
                val fx = (frame.x * factor).toInt()
                val fy = (frame.y * factor).toInt()
                val fw = (frame.width * factor).toInt()
                val fh = (frame.height * factor).toInt()
                // factor=2.0, scissorBox: java.awt.Rectangle[x=0,y=0,width=2560,height=1496], viewport: java.awt.Rectangle[x=0,y=0,width=2560,height=1496]
                val info = BaseOpenglContext.ContextInfo(RectangleInt(), RectangleInt(),)
                info.scissors?.setTo(fx, fy, fw, fh)
                info.viewport?.setTo(fx, fy, fw, fh)
                println("info=$info")
                action(g, info)
            } finally {
                swapBuffers()
                releaseCurrent()
            }
        }
    }

    fun setParameters() {
        val dims = intArrayOf(720, 480)
        GL.CGLSetParameter(openGLContext, 304, dims)
        GL.CGLEnable(openGLContext, 304)
    }
}

class MacAWTOpenglContext(val gwconfig: GameWindowConfig, val c: Component, var other: MacosGLContext? = null) : BaseOpenglContext {
    companion object {
        private inline fun <T : Any> privilegedAction(crossinline block: () -> T): T {
            var result: T? = null
            AccessController.doPrivileged(PrivilegedAction {
                result = block()
            })
            return result!!
        }
    }

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
                ///println("factor=$factor, scissorBox: $scissorBox, viewport: $viewport")
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
                if (other != null) {
                    val oldContext = NSClass("NSOpenGLContext").msgSend("currentContext")
                    other!!.useContext(g, ag) { g, info -> action(g, info) }
                    oldContext.msgSend("makeCurrentContext")
                } else {
                    action(g, info)
                }

            } catch (e: Throwable) {
                e.printStackTrace()
            }
        })
    }

    override fun makeCurrent() = Unit
    override fun releaseCurrent() = Unit
    override fun swapBuffers() = Unit
}

class ProxiedMacAWTOpenglContext(val c: Component, val gwconfig: GameWindowConfig) : BaseOpenglContext {
    override fun makeCurrent() {
    }

    val cctx = MacAWTOpenglContext(gwconfig = gwconfig, c)

    override fun useContext(g: Graphics, ag: AG, action: (Graphics, BaseOpenglContext.ContextInfo) -> Unit) {
        val gl = (ag as AGOpengl).gl
        cctx.useContext(g, ag) { g, info ->
            if (cctx.other == null) {
                val peer = getComponentPeer(if (c is Window) c else SwingUtilities.getWindowAncestor(c))
                val platformWindow = peer.getOrThrow("platformWindow")
                val nsWindowPtr = platformWindow.getOrThrow("ptr").long
                val contentView = nsWindowPtr.msgSend("contentView")

                cctx.other = MacosGLContext(contentView, nsWindowPtr, sharedContext = NSClass("NSOpenGLContext").msgSend("currentContext"))
            }

            val backBufferTextureBinding2d = gl.getIntegerv(gl.TEXTURE_BINDING_2D)
            val backBufferRenderBufferBinding = gl.getIntegerv(gl.RENDERBUFFER_BINDING)
            val backBufferFrameBufferBinding = gl.getIntegerv(gl.FRAMEBUFFER_BINDING)

            println("backBufferTextureBinding2d=$backBufferTextureBinding2d, $backBufferRenderBufferBinding, $backBufferFrameBufferBinding")

            /*
            gl.bindTexture(gl.TEXTURE_2D, backBufferTextureBinding2d)
            gl.bindRenderbuffer(gl.RENDERBUFFER, backBufferRenderBufferBinding)
            gl.bindFramebuffer(gl.FRAMEBUFFER, backBufferFrameBufferBinding)
             */

            cctx.other!!.useContext(g, ag) { g, info -> action(g, info) }
        }
    }
}

fun getComponentPeer(component: Component?): Dyn {
    return Dyn.global["sun.awt.AWTAccessor"]
        .dynamicInvokeOrThrow("getComponentAccessor")
        .dynamicInvokeOrThrow("getPeer", component)
}
