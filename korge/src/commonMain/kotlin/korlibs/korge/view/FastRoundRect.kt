package korlibs.korge.view

import korlibs.image.color.*
import korlibs.korge.render.*
import korlibs.math.geom.*

inline fun Container.fastRoundRect(
    size: Size,
    corners: RectCorners = RectCorners(.1f, .1f, 1f, 1f),
    color: RGBA = Colors.WHITE,
    callback: @ViewDslMarker FastRoundRect.() -> Unit = {}
) = FastRoundRect(size, corners).colorMul(color).addTo(this, callback)

open class FastRoundRect(
    size: Size = Size(100f, 100f),
    corners: RectCorners = RectCorners(.1f, .1f, 1f, 1f)
) : FastRoundRectBase(size, corners, doScale = true) {
    var corners: RectCorners
        get() = cornersRatio
        set(value) { cornersRatio = value }
    override fun renderInternal(ctx: RenderContext) {
        cornersRatio = RectCorners(corners.topLeft)
        super.renderInternal(ctx)
    }
}
