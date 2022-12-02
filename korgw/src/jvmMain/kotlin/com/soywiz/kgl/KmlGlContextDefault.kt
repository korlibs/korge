package com.soywiz.kgl

import com.soywiz.kmem.*
import com.soywiz.korgw.osx.*
import com.soywiz.korgw.platform.*
import com.sun.jna.Memory

actual fun KmlGlContextDefault(window: Any?, parent: KmlGlContext?): KmlGlContext {
    return when {
        Platform.isMac -> MacKmlGlContext(window, parent)
        else -> error("Unsupported OS ${Platform.os}")
    }
}

open class MacKmlGlContext(window: Any? = null, parent: KmlGlContext? = null) : KmlGlContext(window, MacKmlGL(), parent) {
    init {
        MacGL.CGLEnable()
    }
    var ctx: com.sun.jna.Pointer? = run {
        val attributes = Memory(16L)
        attributes.setInt(0, kCGLPFAAccelerated)
        attributes.setInt(4, kCGLPFAOpenGLProfile)
        attributes.setInt(8, kCGLOGLPVersion_GL4_Core)
        attributes.setInt(12, 0)
        val num = Memory(4L)
        val ctx = Memory(8L) // void**
        val pix = Memory(8L) // void**
        checkError(MacGL.CGLChoosePixelFormat(attributes, pix, num))
        checkError(MacGL.CGLCreateContext(pix.getPointer(0L), (parent as? MacKmlGlContext)?.ctx, ctx))
        MacGL.CGLDestroyPixelFormat(pix.getPointer(0L))
        ctx.getPointer(0L)
    }

    private fun <T> checkError(value: T): T {
        return value
    }

    override fun set() {
        MacGL.CGLSetCurrentContext(ctx)
    }

    override fun unset() {
        MacGL.CGLSetCurrentContext(null)
    }

    override fun close() {
        if (ctx == null) return
        if (MacGL.CGLGetCurrentContext() == ctx) {
            MacGL.CGLSetCurrentContext(null)
        }
        MacGL.CGLDestroyContext(ctx)
        ctx = null
    }

    companion object {
        const val kCGLPFAAccelerated = 73
        const val kCGLPFAOpenGLProfile = 99
        const val kCGLOGLPVersion_GL4_Core = 16640
    }
}
