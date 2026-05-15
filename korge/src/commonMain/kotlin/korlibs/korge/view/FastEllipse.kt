package korlibs.korge.view

import korlibs.image.color.*
import korlibs.math.geom.*

inline fun Container.fastEllipse(
    size: Size,
    color: RGBA = Colors.WHITE,
    callback: @ViewDslMarker FastEllipse.() -> Unit = {}
) = FastEllipse(size).colorMul(color).addTo(this, callback)

open class FastEllipse(size: Size = Size(100f, 100f)) : FastRoundRectBase(size, RectCorners(1f, 1f, 1f, 1f), doScale = false) {
    var radius: Size
        get() = Size(width, height) / 2f
        set(value) { size(value * 2f) }
    var radiusAvg: Double
        get() = radius.avgComponent()
        set(value) { radius = Size(value, value) }
}
