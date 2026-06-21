package korlibs.kgl

actual fun KmlGlContextDefault(window: Any?, parent: KmlGlContext?): KmlGlContext {
    return MacosKmlGlContext(window, parent)
}

class MacosKmlGlContext(
    window: Any?,
    parent: KmlGlContext?,
) : KmlGlContext(window, KmlGlNative(), parent) {
    override fun set() {
        error("macOS OpenGL context creation is not implemented yet")
    }

    override fun unset() {
    }

    override fun swap() {
    }

    override fun close() {
    }
}
