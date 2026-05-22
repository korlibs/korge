package korlibs.kgl

actual fun KmlGlContextDefault(window: Any?, parent: KmlGlContext?): KmlGlContext {
    return WatchosKmlGlContext(window, parent)
}

class WatchosKmlGlContext(
    window: Any?,
    parent: KmlGlContext?,
) : KmlGlContext(window, KmlGlNative(), parent) {
    override fun set() {
        error("OpenGL contexts are not supported on watchOS")
    }

    override fun unset() {
    }

    override fun swap() {
    }

    override fun close() {
    }
}
