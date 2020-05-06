package com.soywiz.korge.view

interface Anchorable {
    var anchorX: Double
    var anchorY: Double
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T : Anchorable> T.anchor(ax: Number, ay: Number): T =
    this.apply { this.anchorX = ax.toDouble() }.apply { this.anchorY = ay.toDouble() }

fun <T : Anchorable> T.center(): T = anchor(0.5, 0.5)
val <T : Anchorable> T.centered: T get() = anchor(0.5, 0.5)
