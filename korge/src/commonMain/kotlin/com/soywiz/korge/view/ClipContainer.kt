package com.soywiz.korge.view

inline fun Container.clipContainer(width: Int, height: Int, callback: @ViewDslMarker ClipContainer.() -> Unit = {}) =
    clipContainer(width.toDouble(), height.toDouble(), callback)
inline fun Container.clipContainer(width: Double, height: Double, callback: @ViewDslMarker ClipContainer.() -> Unit = {}) =
    ClipContainer(width, height).addTo(this, callback)

open class ClipContainer(
    width: Double = 100.0,
    height: Double = 100.0
) : FixedSizeContainer(width, height, clip = true)
