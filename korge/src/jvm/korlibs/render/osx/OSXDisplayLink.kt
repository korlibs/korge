package korlibs.render.osx

import com.sun.jna.*
import korlibs.datastructure.lock.*
import korlibs.datastructure.thread.*
import korlibs.platform.Platform
import korlibs.time.*

class OSXDisplayLink(
    var onCallback: () -> Unit = { }
) {
    val displayLinkData = Memory(16L).also { it.clear() }
    var displayLinkLock: Lock = Lock()
    var displayLink: Pointer? = Pointer.NULL
    var thread: NativeThread? = null

    // @TODO: by lazy // but maybe causing issues with the intellij plugin?
    val displayLinkCallback = object : DisplayLinkCallback {
        override fun callback(
            displayLink: Pointer?,
            inNow: Pointer?,
            inOutputTime: Pointer?,
            flagsIn: Pointer?,
            flagsOut: Pointer?,
            userInfo: Pointer?
        ): Int {
            //println("DISPLAY LINK")
            synchronized(displayLinkLock) {
                displayLinkLock.notify()
            }
            return 0
        }
    }.also {
        Native.setCallbackThreadInitializer(it, CallbackThreadInitializer(false, false, "DisplayLink"))
    }

    var running = false

    fun start() {
        if (!Platform.isMac) return
        running = true
        thread = nativeThread {
            try {
                while (running) {
                    synchronized(displayLinkLock) {
                        displayLinkLock.wait(2.hz.timeSpan)
                    }
                    onCallback()
                }
            } catch (_: InterruptedException) {
            }
        }
        try {
            val displayID = CoreGraphics.CGMainDisplayID()
            val res = CoreVideo.CVDisplayLinkCreateWithCGDisplay(displayID, displayLinkData)

            if (res == 0) {
                displayLink = displayLinkData.getPointer(0L)
                if (CoreVideo.CVDisplayLinkSetOutputCallback(displayLink, displayLinkCallback, Pointer.NULL) == 0) {
                    CoreVideo.CVDisplayLinkStart(displayLink)
                } else {
                    displayLink = Pointer.NULL
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun stop() {
        running = false
        if (!Platform.isMac) return
        thread?.interrupt()
        thread = null
        if (displayLink != Pointer.NULL) {
            CoreVideo.CVDisplayLinkStop(displayLink)
        }
    }
}
