package korlibs.kgl

import korlibs.io.lang.*

expect fun KmlGlContextDefault(window: Any? = null, parent: KmlGlContext? = null): KmlGlContext

fun OffsetKmlGlContext(fboWidth: Int, fboHeight: Int, doUnset: Boolean = true): KmlGlContext {
    val ctx = KmlGlContextDefault()
    ctx.set()

    val gl = ctx.gl

    val GL_RGBA8 = 0x8058

    // Build the texture that will serve as the color attachment for the framebuffer.
    val colorRenderbuffer = gl.genRenderbuffer()
    gl.bindRenderbuffer(KmlGl.RENDERBUFFER, colorRenderbuffer)
    gl.renderbufferStorage(KmlGl.RENDERBUFFER, GL_RGBA8, fboWidth, fboHeight)
    gl.bindRenderbuffer(KmlGl.RENDERBUFFER, 0)

    // Build the texture that will serve as the depth attachment for the framebuffer.
    val depthRenderbuffer = gl.genRenderbuffer()
    gl.bindRenderbuffer(KmlGl.RENDERBUFFER, depthRenderbuffer)
    gl.renderbufferStorage(KmlGl.RENDERBUFFER, KmlGl.DEPTH_COMPONENT, fboWidth, fboHeight)
    gl.bindRenderbuffer(KmlGl.RENDERBUFFER, 0)

    // Build the framebuffer.
    val framebuffer = gl.genFramebuffer()
    gl.bindFramebuffer(KmlGl.FRAMEBUFFER, framebuffer)
    gl.framebufferRenderbuffer(KmlGl.FRAMEBUFFER, KmlGl.COLOR_ATTACHMENT0, KmlGl.RENDERBUFFER, colorRenderbuffer)
    gl.framebufferRenderbuffer(KmlGl.FRAMEBUFFER, KmlGl.DEPTH_ATTACHMENT, KmlGl.RENDERBUFFER, depthRenderbuffer)

    val status = gl.checkFramebufferStatus(KmlGl.FRAMEBUFFER)
    //if (status != GL_FRAMEBUFFER_COMPLETE)
    // Error

    if (doUnset) ctx.unset()

    return ctx
}

inline fun KmlGlContextDefaultTemp(block: (KmlGl) -> Unit) {
    KmlGlContextDefault().use {
        it.set()
        try {
            block(it.gl)
        } finally {
            it.unset()
        }
    }
}

abstract class KmlGlContext(val window: Any?, val gl: KmlGl, val parent: KmlGlContext? = null) : AutoCloseable {
    open fun set() {
    }
    open fun unset() {
    }
    open fun swap() {
    }
    override fun close() {
    }
}
