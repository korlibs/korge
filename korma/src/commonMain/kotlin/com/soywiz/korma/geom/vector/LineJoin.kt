package com.soywiz.korma.geom.vector

enum class LineJoin {
    BEVEL,
    ROUND,
    MITER;

    companion object {
        val SQUARE = BEVEL

        operator fun get(str: String?): LineJoin = when (str) {
            null -> MITER
            "MITER", "miter" -> MITER
            "BEVEL", "bevel" -> BEVEL
            "SQUARE", "square" -> SQUARE
            "ROUND", "round" -> ROUND
            else -> MITER
        }
    }
}
