package com.soywiz.korge.view

@Deprecated("Kotlin/Native boxes inline+Number")
inline fun Container.clipContainer(width: Number, height: Number, callback: ClipContainer.() -> Unit = {}) =
    clipContainer(width.toDouble(), height.toDouble(), callback)

inline fun Container.clipContainer(width: Double, height: Double, callback: ClipContainer.() -> Unit = {}) =
    ClipContainer(width, height).addTo(this, callback)

open class ClipContainer(
    width: Double = 100.0,
    height: Double = 100.0
) : FixedSizeContainer(width, height, clip = true)
