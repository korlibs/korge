package korlibs.korge.view

inline fun Container.clipContainer(width: Int, height: Int, callback: @ViewDslMarker ClipContainer.() -> Unit = {}) =
    clipContainer(width.toFloat(), height.toFloat(), callback)
inline fun Container.clipContainer(width: Double, height: Double, callback: @ViewDslMarker ClipContainer.() -> Unit = {}) =
    ClipContainer(width.toFloat(), height.toFloat()).addTo(this, callback)
inline fun Container.clipContainer(width: Float, height: Float, callback: @ViewDslMarker ClipContainer.() -> Unit = {}) =
    ClipContainer(width, height).addTo(this, callback)

open class ClipContainer(
    width: Float = 100f,
    height: Float = 100f
) : FixedSizeContainer(width, height, clip = true)
