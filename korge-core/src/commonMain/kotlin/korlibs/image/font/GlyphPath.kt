package korlibs.image.font

import korlibs.image.bitmap.*
import korlibs.image.vector.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*

data class GlyphPath(
    var path: VectorPath = VectorPath(),
    var colorShape: Shape? = null,
    var bitmap: Bitmap? = null,
    var bitmapOffset: Point = Point(0, 0),
    var bitmapScale: Scale = Scale(1, 1),
    var transform: Matrix = Matrix(),
    var scale: Double = 1.0
) : Drawable {
    val isOnlyPath get() = bitmap == null && colorShape == null

    override fun draw(c: Context2d) {
        c.keepTransform {
            //c.beginPath()
            c.transform(this.transform)
            when {
                bitmap != null -> {
                    //println("scale = $scale")
                    c.drawImage(bitmap!!, bitmapOffset, bitmap!!.size.toFloat() * bitmapScale)
                }
                colorShape != null -> {
                    c.draw(colorShape!!)
                    c.beginPath() // to avoid filling/stroking later
                }
                else -> {
                    //println("this.transform=${this.transform}, path=$path")
                    c.draw(path)
                }
            }
        }
    }
}
