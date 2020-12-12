package com.soywiz.korma.geom.vector

enum class LineCap {
    BUTT,
    SQUARE,
    ROUND;

    companion object {
        operator fun get(str: String?): LineCap = when (str) {
            null -> BUTT
            "BUTT", "butt" -> BUTT
            "SQUARE", "square" -> SQUARE
            "ROUND", "round" -> ROUND
            else -> BUTT
        }
    }
}
