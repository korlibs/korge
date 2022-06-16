package com.soywiz.korma.geom.vector

enum class LineScaleMode(val hScale: Boolean, val vScale: Boolean) {
    NONE(false, false),
    HORIZONTAL(true, false),
    VERTICAL(false, true),
    NORMAL(true, true);

    val anyScale: Boolean = hScale || vScale
    val allScale: Boolean = hScale && vScale
}
