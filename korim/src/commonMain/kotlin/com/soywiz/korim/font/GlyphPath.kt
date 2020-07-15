package com.soywiz.korim.font

import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.*

data class GlyphPath(
    var path: GraphicsPath = GraphicsPath(),
    val transform: Matrix = Matrix()
) : Drawable {
    override fun draw(c: Context2d) {
        c.keepTransform {
            c.beginPath()
            c.transform(this.transform)
            c.draw(path)
        }
    }
}
