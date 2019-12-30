package com.soywiz.korge.view

inline fun Container.clipContainer(width: Number, height: Number, callback: @ViewsDslMarker ClipContainer.() -> Unit = {}) =
    ClipContainer(width.toDouble(), height.toDouble()).addTo(this).apply(callback)

open class ClipContainer(
    width: Double = 100.0,
    height: Double = 100.0
) : FixedSizeContainer(width, height, clip = true)
