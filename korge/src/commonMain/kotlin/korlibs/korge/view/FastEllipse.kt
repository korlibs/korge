package korlibs.korge.view

import korlibs.image.color.*
import korlibs.math.geom.*

inline fun Container.fastEllipse(
    size: Size,
    color: RGBA = Colors.WHITE,
    callback: @ViewDslMarker FastEllipse.() -> Unit = {}
) = FastEllipse(size.widthD, size.heightD).colorMul(color).addTo(this, callback)

open class FastEllipse(width: Double = 100.0, height: Double = 100.0) : FastRoundRectBase(
    width, height, RectCorners(1f, 1f, 1f, 1f), doScale = false
) {
    var radius: Size
        get() = Size(width, height) / 2f
        set(value) { setSize(value * 2f) }
    var radiusAvg: Float
        get() = radius.avgComponent()
        set(value) { radius = Size(value, value) }
}
