package com.soywiz.kgl

import com.soywiz.kmem.*
import kotlinx.cinterop.*
import platform.OpenGLCommon.*

actual fun KmlGlContextDefault(window: Any?, parent: KmlGlContext?): KmlGlContext = MacKmlGlContext(window, parent)

// http://renderingpipeline.com/2012/05/windowless-opengl-on-macos-x/
open class MacKmlGlContext(window: Any? = null, parent: KmlGlContext? = null) : KmlGlContext(window, KmlGlNative(), parent) {
    var ctx: CGLContextObj? = run {
        uintArrayOf(
            kCGLPFAAccelerated,
            kCGLPFAOpenGLProfile, kCGLOGLPVersion_GL4_Core,
            kCGLPFAColorSize, 24u,
            kCGLPFADepthSize, 16u,
            kCGLPFAStencilSize, 8u,
            kCGLPFADoubleBuffer,
            0u
        ).usePinned { attributesPin ->
            // @TODO: Create context for window
            memScoped {
                val ctx = alloc<CGLContextObjVar>()
                val num = alloc<GLintVar>()
                val pix = alloc<CGLPixelFormatObjVar>()
                //println("kCGLPFAAccelerated=$kCGLPFAAccelerated")
                //println("kCGLPFAOpenGLProfile=$kCGLPFAOpenGLProfile")
                //println("kCGLOGLPVersion_GL4_Core=$kCGLOGLPVersion_GL4_Core")
                //println(sizeOf<CGLContextObjVar>())
                //println(sizeOf<GLintVar>())
                //println(sizeOf<CGLPixelFormatObjVar>())
                checkError(CGLChoosePixelFormat(attributesPin.startAddressOf, pix.ptr, num.ptr))
                checkError(CGLCreateContext(pix.value, (parent as? MacKmlGlContext?)?.ctx, ctx.ptr))
                CGLDestroyPixelFormat(pix.value)
                ctx.value
            }
        }
    }

    private fun checkError(value: UInt) {
    }

    override fun set() {
        CGLSetCurrentContext(ctx)
    }

    override fun unset() {
        CGLSetCurrentContext(null)
    }

    override fun close() {
        if (ctx == null) return
        if (CGLGetCurrentContext() == ctx) {
            CGLSetCurrentContext(null)
        }
        CGLDestroyContext(ctx)
        ctx = null
    }
}
