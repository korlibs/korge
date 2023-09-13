package korlibs.render.osx

import korlibs.memory.dyn.*

class MyNSRect(pointer: KPointer? = null) : KStructure(pointer) {
    var x by nativeFloat()
    var y by nativeFloat()
    var width by nativeFloat()
    var height by nativeFloat()
    override fun toString(): String = "NSRect($x, $y, $width, $height)"
}
