package korlibs.render.macos

import korlibs.render.*
import kotlinx.cinterop.*
import platform.AppKit.*
import platform.Foundation.*

object RenderViewProvider {

    fun create(myDefaultGameWindow: MyDefaultGameWindow) : OpenGLView {

        @Suppress("OPT_IN_USAGE")
        val attrs: UIntArray by lazy {
            val antialias = (myDefaultGameWindow.quality != GameWindow.Quality.PERFORMANCE)
            val antialiasArray = if (antialias) intArrayOf(
                NSOpenGLPFAMultisample.convert(),
                NSOpenGLPFASampleBuffers.convert(), 1.convert(),
                NSOpenGLPFASamples.convert(), 4.convert()
            ) else intArrayOf()
            intArrayOf(
                *antialiasArray,
                //NSOpenGLPFAOpenGLProfile,
                //NSOpenGLProfileVersion4_1Core,
                NSOpenGLPFADoubleBuffer.convert(),
                NSOpenGLPFAColorSize.convert(), 24.convert(),
                NSOpenGLPFAAlphaSize.convert(), 8.convert(),
                NSOpenGLPFADepthSize.convert(), 24.convert(),
                NSOpenGLPFAStencilSize.convert(), 8.convert(),
                NSOpenGLPFAAccumSize.convert(), 0.convert(),
                0.convert()
            ).asUIntArray()
        }

        val pixelFormat by lazy {
            attrs.usePinned {
                NSOpenGLPixelFormat(it.addressOf(0).reinterpret<NSOpenGLPixelFormatAttributeVar>())
            }
        }

        return OpenGLView(myDefaultGameWindow, NSMakeRect(0.0, 0.0, 16.0, 16.0), pixelFormat)
    }

}
