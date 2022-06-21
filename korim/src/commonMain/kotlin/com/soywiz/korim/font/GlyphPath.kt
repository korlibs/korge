package com.soywiz.korim.font

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.Drawable
import com.soywiz.korim.vector.draw
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.vector.VectorPath

data class GlyphPath(
    var path: VectorPath = VectorPath(),
    var colorPaths: List<Drawable>? = null,
    var bitmap: Bitmap? = null,
    val bitmapOffset: Point = Point(0, 0),
    val bitmapScale: Point = Point(1, 1),
    val transform: Matrix = Matrix(),
    var scale: Double = 1.0
) : Drawable {
    override fun draw(c: Context2d) {
        c.keepTransform {
            c.beginPath()
            c.transform(this.transform)
            when {
                bitmap != null -> {
                    //println("scale = $scale")
                    c.drawImage(bitmap!!, bitmapOffset.x, bitmapOffset.y, bitmap!!.width * bitmapScale.x, bitmap!!.height * bitmapScale.y)
                }
                colorPaths != null -> {
                    colorPaths?.fastForEach { c.draw(it) }
                    c.beginPath() // to avoid filling/strokking later
                }
                else -> {
                    //println("this.transform=${this.transform}, path=$path")
                    c.draw(path)
                }
            }
        }
    }
}
