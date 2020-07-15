package com.soywiz.korgw.osx

import com.soywiz.korgw.platform.KStructure
import com.sun.jna.Pointer

class MyNSRect(pointer: Pointer? = null) : KStructure(pointer) {
    var x by nativeFloat()
    var y by nativeFloat()
    var width by nativeFloat()
    var height by nativeFloat()
    override fun toString(): String = "NSRect($x, $y, $width, $height)"
}
