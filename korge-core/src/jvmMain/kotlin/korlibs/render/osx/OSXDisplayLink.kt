package korlibs.render.osx

import com.sun.jna.*
import korlibs.platform.Platform

class OSXDisplayLink(
    var onCallback: () -> Unit = { }
) {
    val displayLinkData = Memory(16L).also { it.clear() }
    var displayLink: Pointer? = null

    // @TODO: by lazy // but maybe causing issues with the intellij plugin?
    val displayLinkCallback = DisplayLinkCallback { _, _, _, _, _, _ ->
        onCallback()
        0
    }.also {
        Native.setCallbackThreadInitializer(it, CallbackThreadInitializer(false, false, "DisplayLink"))
    }

    var running = false
        private set

    fun start() {
        if (!Platform.isMac) return
        running = true
        try {
            val displayID = CoreGraphics.CGMainDisplayID()
            val res = CoreVideo.CVDisplayLinkCreateWithCGDisplay(displayID, displayLinkData)

            if (res == 0) {
                displayLink = displayLinkData.getPointer(0L)
                if (CoreVideo.CVDisplayLinkSetOutputCallback(displayLink, displayLinkCallback, null) == 0) {
                    CoreVideo.CVDisplayLinkStart(displayLink)
                } else {
                    displayLink = null
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun stop() {
        running = false
        if (!Platform.isMac) return
        if (displayLink != null) {
            CoreVideo.CVDisplayLinkStop(displayLink)
        }
    }
}
