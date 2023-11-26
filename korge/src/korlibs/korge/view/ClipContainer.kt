package korlibs.korge.view

import korlibs.math.geom.*

inline fun Container.clipContainer(size: Size, callback: @ViewDslMarker ClipContainer.() -> Unit = {}) =
    ClipContainer(size).addTo(this, callback)

open class ClipContainer(
    size: Size = Size(100, 100),
) : FixedSizeContainer(size, clip = true)
