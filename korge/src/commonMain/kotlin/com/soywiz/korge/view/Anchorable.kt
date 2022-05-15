package com.soywiz.korge.view

import com.soywiz.korma.geom.Anchor

interface Anchorable {
    /** Normally in the range [0.0, 1.0] */
    var anchorX: Double
    /** Normally in the range [0.0, 1.0] */
    var anchorY: Double
}

fun <T : Anchorable> T.anchor(ax: Double, ay: Double): T {
    this.anchorX = ax
    this.anchorY = ay
    return this
}

fun <T : Anchorable> T.anchor(ax: Float, ay: Float): T = anchor(ax.toDouble(), ay.toDouble())
fun <T : Anchorable> T.anchor(ax: Int, ay: Int): T = anchor(ax.toDouble(), ay.toDouble())

fun <T : Anchorable> T.anchor(anchor: Anchor): T = anchor(anchor.sx, anchor.sy)

fun <T : Anchorable> T.center(): T = anchor(0.5, 0.5)
val <T : Anchorable> T.centered: T get() = anchor(0.5, 0.5)
