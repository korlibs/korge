package korlibs.image.bitmap.effect

import korlibs.image.bitmap.Bitmap32
import korlibs.image.bitmap.distanceMap
import korlibs.image.color.Colors
import korlibs.image.color.RGBA

fun Bitmap32.border(r: Int, color: RGBA = Colors.BLACK): Bitmap32 {
    val out = Bitmap32(width + (r * 2), height + (r * 2), premultiplied = true)
    out.put(this, r, r)
    out.borderInline(r, color)
    return out
}

fun Bitmap32.borderInline(r: Int, color: RGBA = Colors.BLACK) {
    val out = this
    val distance = out.distanceMap()
    for (y in 0 until out.height) {
        for (x in 0 until out.width) {
            val dist = distance.getDist(x, y)
            if (dist < r + 1) {
                val alpha = if (dist >= r) 1f - dist % 1f else 1f
                val rcolor = if (alpha == 1f) color else color.withAf(color.af * alpha)
                out[x, y] = RGBA.mix(rcolor, out[x, y])
            }
        }
    }
}
