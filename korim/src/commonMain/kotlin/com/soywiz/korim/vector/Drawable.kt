package com.soywiz.korim.vector

import com.soywiz.korma.geom.Rectangle

interface Drawable {
    fun draw(c: Context2d)
}

interface SizedDrawable : Drawable {
    val width: Int
    val height: Int
}

interface BoundsDrawable : SizedDrawable {
    val bounds: Rectangle
    val left: Int get() = bounds.left.toInt()
    val top: Int get() = bounds.top.toInt()
    override val width: Int get() = bounds.width.toInt()
    override val height: Int get() = bounds.height.toInt()
}

class FuncDrawable(val action: Context2d.() -> Unit) : Drawable {
    override fun draw(c: Context2d) {
        c.keep {
            action(c)
        }
    }
}
