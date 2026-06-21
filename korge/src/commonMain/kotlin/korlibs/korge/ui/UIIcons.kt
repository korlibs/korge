package korlibs.korge.ui

import korlibs.image.atlas.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.vector.*
import korlibs.math.geom.*

object UIIcons {
    val atlas = MutableAtlasUnit()

    fun createIcon(size: Size = Size(32, 32), block: Context2d.() -> Unit): BmpSlice =
        atlas.add(Bitmap32(size.width.toInt(), size.height.toInt()).context2d { block() }).slice

    val CROSS = createIcon {
        val padding = 8
        stroke(Colors.WHITE, lineWidth = 4.0) {
            line(Point(padding, padding), Point(32 - padding, 32 - padding))
            line(Point(32 - padding, padding), Point(padding, 32 - padding))
        }
    }
}
