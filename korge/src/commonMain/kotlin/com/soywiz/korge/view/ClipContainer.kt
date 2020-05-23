package com.soywiz.korge.view

@Deprecated("Kotlin/Native boxes inline+Number")
inline fun Container.clipContainer(width: Number, height: Number, callback: @ViewsDslMarker ClipContainer.() -> Unit = {}) =
    clipContainer(width.toDouble(), height.toDouble(), callback)

inline fun Container.clipContainer(width: Double, height: Double, callback: @ViewsDslMarker ClipContainer.() -> Unit = {}) =
    ClipContainer(width, height).addTo(this).apply(callback)

open class ClipContainer(
    width: Double = 100.0,
    height: Double = 100.0
) : FixedSizeContainer(width, height, clip = true)
