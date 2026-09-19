package korlibs.kgl

import platform.EAGL.*

actual fun KmlGlContextDefault(window: Any?, parent: KmlGlContext?): KmlGlContext {
    return DarwinGlContext(window, parent)
}

class DarwinGlContext(window: Any?, parent: KmlGlContext?) : KmlGlContext(window, KmlGlNative(), parent) {
    //val shareGroup: EAGLSharegroup = (parent as? DarwinGlContext?)?.shareGroup ?: EAGLSharegroup()
    //val shareGroup: EAGLSharegroup = EAGLSharegroup()
    //val context = EAGLContext(kEAGLRenderingAPIOpenGLES3, shareGroup)
    val shareGroup = (parent as? DarwinGlContext?)?.context?.sharegroup
    val context: EAGLContext = if (shareGroup != null) EAGLContext(kEAGLRenderingAPIOpenGLES2, shareGroup) else EAGLContext(kEAGLRenderingAPIOpenGLES2)

    override fun set() {
        if (!EAGLContext.setCurrentContext(EAGLContext.currentContext())) error("Couldn't set EAGLContext")
    }

    override fun unset() {
        EAGLContext.setCurrentContext(null)
    }

    override fun swap() {
    }

    override fun close() {
    }
}
