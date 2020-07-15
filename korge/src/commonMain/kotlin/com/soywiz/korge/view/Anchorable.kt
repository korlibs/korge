package com.soywiz.korge.view

import com.soywiz.korma.geom.*

interface Anchorable {
    var anchorX: Double
    var anchorY: Double
}

fun <T : Anchorable> T.anchor(ax: Double, ay: Double): T {
    this.anchorX = ax
    this.anchorY = ay
    return this
}

@Suppress("NOTHING_TO_INLINE")
@Deprecated("Kotlin/Native boxes inline+Number", ReplaceWith("anchor(ax.toDouble(), ay.toDouble())"))
inline fun <T : Anchorable> T.anchor(ax: Number, ay: Number): T = anchor(ax.toDouble(), ay.toDouble())
inline fun <T : Anchorable> T.anchor(anchor: Anchor): T = anchor(anchor.sx, anchor.sy)

fun <T : Anchorable> T.center(): T = anchor(0.5, 0.5)
val <T : Anchorable> T.centered: T get() = anchor(0.5, 0.5)
