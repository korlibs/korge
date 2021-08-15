package com.soywiz.korim.font

import com.soywiz.kds.iterators.*
import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.*

data class GlyphPath(
    var path: GraphicsPath = GraphicsPath(),
    var colorPaths: List<Drawable>? = null,
    val transform: Matrix = Matrix()
) : Drawable {
    override fun draw(c: Context2d) {
        c.keepTransform {
            c.beginPath()
            c.transform(this.transform)
            if (colorPaths != null) {
                colorPaths?.fastForEach {
                    c.draw(it)
                }
            } else{
                c.draw(path)
            }
        }
    }
}
