package com.soywiz.korma.geom.vector

enum class LineJoin {
    BEVEL,
    ROUND,
    MITER;

    companion object {
        val SQUARE = BEVEL
    }
}
