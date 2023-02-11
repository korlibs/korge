package com.soywiz.kgl

import com.soywiz.korio.lang.*

expect fun KmlGlContextDefault(window: Any? = null, parent: KmlGlContext? = null): KmlGlContext

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

abstract class KmlGlContext(val window: Any?, val gl: KmlGl, val parent: KmlGlContext? = null) : Closeable {
    open fun set() {
    }
    open fun unset() {
    }
    open fun swap() {
    }
    override fun close() {
    }
}
