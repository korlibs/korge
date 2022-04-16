package com.soywiz.korgw.osx

import com.soywiz.kmem.dyn.*

class MyNSRect(pointer: KPointer? = null) : KStructure(pointer) {
    var x by nativeFloat()
    var y by nativeFloat()
    var width by nativeFloat()
    var height by nativeFloat()
    override fun toString(): String = "NSRect($x, $y, $width, $height)"
}
