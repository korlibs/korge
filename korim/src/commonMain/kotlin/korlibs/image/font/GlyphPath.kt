package korlibs.image.font

import korlibs.image.bitmap.Bitmap
import korlibs.image.vector.Context2d
import korlibs.image.vector.Drawable
import korlibs.image.vector.Shape
import korlibs.image.vector.draw
import korlibs.math.geom.MMatrix
import korlibs.math.geom.MPoint
import korlibs.math.geom.vector.VectorPath

data class GlyphPath(
    var path: VectorPath = VectorPath(),
    var colorShape: Shape? = null,
    var bitmap: Bitmap? = null,
    val bitmapOffset: MPoint = MPoint(0, 0),
    val bitmapScale: MPoint = MPoint(1, 1),
    val transform: MMatrix = MMatrix(),
    var scale: Double = 1.0
) : Drawable {
    val isOnlyPath get() = bitmap == null && colorShape == null

    override fun draw(c: Context2d) {
        c.keepTransform {
            //c.beginPath()
            c.transform(this.transform.immutable)
            when {
                bitmap != null -> {
                    //println("scale = $scale")
                    c.drawImage(bitmap!!, bitmapOffset.x, bitmapOffset.y, bitmap!!.width * bitmapScale.x, bitmap!!.height * bitmapScale.y)
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