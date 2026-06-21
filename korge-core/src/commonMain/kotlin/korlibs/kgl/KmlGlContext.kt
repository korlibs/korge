package korlibs.kgl

expect fun KmlGlContextDefault(window: Any? = null, parent: KmlGlContext? = null): KmlGlContext

class OffscreenKmlGlContext(
    val colorRenderbuffer: Int,
    val depthRenderbuffer: Int,
    val framebuffer: Int,
    val ctx: KmlGlContext,
    var width: Int = 0,
    var height: Int = 0,
) {
    val gl get() = ctx.gl

    fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height

        val GL_RGBA8 = 0x8058

        gl.bindRenderbuffer(KmlGl.RENDERBUFFER, colorRenderbuffer)
        gl.renderbufferStorage(KmlGl.RENDERBUFFER, GL_RGBA8, width, height)
        gl.bindRenderbuffer(KmlGl.RENDERBUFFER, 0)

        // Build the texture that will serve as the depth attachment for the framebuffer.
        gl.bindRenderbuffer(KmlGl.RENDERBUFFER, depthRenderbuffer)
        gl.renderbufferStorage(KmlGl.RENDERBUFFER, KmlGl.DEPTH_COMPONENT, width, height)
        gl.bindRenderbuffer(KmlGl.RENDERBUFFER, 0)
    }

    fun doClear() {
        gl.bindFramebuffer(KmlGl.FRAMEBUFFER, framebuffer)
        gl.clear(KmlGl.COLOR_BUFFER_BIT or KmlGl.DEPTH_BUFFER_BIT)
    }
}

fun OffsetKmlGlContext(fboWidth: Int, fboHeight: Int, doUnset: Boolean = true): KmlGlContext {
    return NewOffsetKmlGlContext(fboWidth, fboHeight, doUnset).ctx
}

fun NewOffsetKmlGlContext(fboWidth: Int, fboHeight: Int, doUnset: Boolean = true): OffscreenKmlGlContext {
    val ctx = KmlGlContextDefault()
    ctx.set()

    val gl = ctx.gl

    // Build the texture that will serve as the color attachment for the framebuffer.
    val colorRenderbuffer = gl.genRenderbuffer()
    val depthRenderbuffer = gl.genRenderbuffer()
    val framebuffer = gl.genFramebuffer()
    val out = OffscreenKmlGlContext(colorRenderbuffer, depthRenderbuffer, framebuffer, ctx)

    out.setSize(fboWidth, fboHeight)

    // Build the framebuffer.
    gl.bindFramebuffer(KmlGl.FRAMEBUFFER, framebuffer)
    gl.framebufferRenderbuffer(KmlGl.FRAMEBUFFER, KmlGl.COLOR_ATTACHMENT0, KmlGl.RENDERBUFFER, colorRenderbuffer)
    gl.framebufferRenderbuffer(KmlGl.FRAMEBUFFER, KmlGl.DEPTH_ATTACHMENT, KmlGl.RENDERBUFFER, depthRenderbuffer)

    val status = gl.checkFramebufferStatus(KmlGl.FRAMEBUFFER)
    //if (status != GL_FRAMEBUFFER_COMPLETE)
    // Error

    if (doUnset) ctx.unset()

    return out
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
