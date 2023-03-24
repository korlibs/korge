package korlibs.korge.view

import korlibs.korge.render.*
import korlibs.image.color.*
import korlibs.math.geom.*

inline fun Container.fastRoundRect(
    size: Size,
    corners: RectCorners = RectCorners(.1f, .1f, 1f, 1f),
    color: RGBA = Colors.WHITE,
    callback: @ViewDslMarker FastRoundRect.() -> Unit = {}
) = FastRoundRect(size.widthD, size.heightD, corners).colorMul(color).addTo(this, callback)

open class FastRoundRect(
    width: Double = 100.0,
    height: Double = 100.0,
    corners: RectCorners = RectCorners(.1f, .1f, 1f, 1f)
) : FastRoundRectBase(
    width, height, corners, doScale = true
) {
    var corners: RectCorners
        get() = cornersRatio
        set(value) { cornersRatio = value }
    override fun renderInternal(ctx: RenderContext) {
        cornersRatio = RectCorners(corners.topLeft)
        super.renderInternal(ctx)
    }
}
