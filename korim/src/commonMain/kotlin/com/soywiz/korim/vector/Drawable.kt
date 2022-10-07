package com.soywiz.korim.vector

import com.soywiz.kmem.clamp
import com.soywiz.kmem.toIntCeil
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.BitmapWithHotspot
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.NativeImageOrBitmap32
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korma.geom.ISize
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.PointInt
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.ScaleMode
import com.soywiz.korma.geom.topLeft

interface Drawable {
    fun draw(c: Context2d)
}

fun <T : Bitmap> Drawable.draw(out: T): T {
    out.context2d {
        this@draw.draw(this)
    }
    return out
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

fun BoundsDrawable.renderWithHotspot(scale: Double? = null, fit: ISize? = null, native: Boolean = true): BitmapWithHotspot<Bitmap> {
    val bounds = this.bounds
    val rscale = when {
        fit != null -> {
            val size2 = ScaleMode.FIT(bounds.size, fit)
            kotlin.math.min(size2.width / bounds.width, size2.height / bounds.height)
        }
        scale != null ->  scale
        else -> 1.0
    }
    val image = NativeImageOrBitmap32((bounds.width * rscale).toIntCeil(), (bounds.height * rscale).toIntCeil(), premultiplied = true, native = native).context2d {
        scale(rscale)
        translate(-bounds.x, -bounds.y)
        draw(this@renderWithHotspot)
    }
    return BitmapWithHotspot(image, PointInt(
        (-bounds.left * rscale).toInt().clamp(0, image.width - 1),
        (-bounds.top * rscale).toInt().clamp(0, image.height - 1),
    ))
}

class FuncDrawable(val action: Context2d.() -> Unit) : Drawable {
    override fun draw(c: Context2d) {
        c.keep {
            action(c)
        }
    }
}
