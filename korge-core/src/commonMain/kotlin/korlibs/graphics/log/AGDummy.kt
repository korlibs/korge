package korlibs.graphics.log

import korlibs.graphics.*
import korlibs.math.geom.*

open class AGDummy(size: Size = Size(640, 480)) : AG() {
    init {
        mainFrameBuffer.setSize(size.width.toInt(), size.height.toInt())
    }
    override val graphicExtensions: Set<String> get() = emptySet()
    override val isInstancedSupported: Boolean get() = true
    override val isFloatTextureSupported: Boolean get() = true
}
