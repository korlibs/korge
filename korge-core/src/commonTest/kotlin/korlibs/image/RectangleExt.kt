package korlibs.image

import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.Colors
import korlibs.math.geom.MRectangle
import korlibs.math.geom.bounds

fun Iterable<MRectangle>.render(): Bitmap32 {
    val colors = listOf(Colors.RED, Colors.GREEN, Colors.BLUE, Colors.BLACK)
    val bounds = this.bounds()
    val out = Bitmap32(bounds.width.toInt(), bounds.height.toInt(), premultiplied = false)
    for ((index, r) in this.withIndex()) {
        val color = colors[index % colors.size]
        out.fill(color, r.x.toInt(), r.y.toInt(), r.width.toInt(), r.height.toInt())
    }
    return out
}
