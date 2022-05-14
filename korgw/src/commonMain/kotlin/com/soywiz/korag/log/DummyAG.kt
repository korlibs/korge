package com.soywiz.korag.log

import com.soywiz.korag.AG

open class DummyAG(width: Int = 640, height: Int = 480) : AG() {
    override val nativeComponent: Any = Any()
    override var backWidth: Int = width
    override var backHeight: Int = height

    override val graphicExtensions: Set<String> get() = emptySet()
    override val isInstancedSupported: Boolean get() = true
    override val isFloatTextureSupported: Boolean get() = true
}
