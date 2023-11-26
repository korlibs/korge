package korlibs.image.vector

import korlibs.image.bitmap.*
import korlibs.math.*
import korlibs.math.geom.*

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

//@Deprecated("Use .toShape(size)")
//fun Drawable.withSize(size: Size): SizedDrawable = object : SizedDrawable {
//    override val width: Int get() = size.width.toInt()
//    override val height: Int get() = size.height.toInt()
//    override fun draw(c: Context2d) = this@withSize.draw(c)
//}

interface BoundsDrawable : SizedDrawable {
    val bounds: Rectangle
    val left: Int get() = bounds.left.toInt()
    val top: Int get() = bounds.top.toInt()
    override val width: Int get() = bounds.width.toInt()
    override val height: Int get() = bounds.height.toInt()
}

fun BoundsDrawable.renderWithHotspot(scale: Double? = null, fit: Size? = null, native: Boolean = true): BitmapWithHotspot<Bitmap> {
    val bounds = this.bounds
    val rscale: Double = when {
        fit != null -> {
            val size2 = ScaleMode.FIT(bounds.size, fit)
            kotlin.math.min(size2.width / bounds.width, size2.height / bounds.height)
        }
        scale != null -> scale.toDouble()
        else -> 1.0
    }
    val image = NativeImageOrBitmap32((bounds.width * rscale).toIntCeil(), (bounds.height * rscale).toIntCeil(), premultiplied = true, native = native).context2d {
        scale(rscale)
        translate(-bounds.x, -bounds.y)
        draw(this@renderWithHotspot)
    }
    return BitmapWithHotspot(image, Vector2I(
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
