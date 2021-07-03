package com.soywiz.korag.log

import com.soywiz.korag.*

open class DummyAG(width: Int = 1280, height: Int = 720) : AG() {
    override val nativeComponent: Any = Any()
    override var backWidth: Int = width; set(value) = run { field = value }
    override var backHeight: Int = height; set(value) = run { field = value }
}
