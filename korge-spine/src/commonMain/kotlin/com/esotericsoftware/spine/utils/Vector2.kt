package com.esotericsoftware.spine.utils

data class Vector2(
    var x: Float = 0f,
    var y: Float = 0f
) {
    operator fun set(x: Float, y: Float): Vector2 {
        this.x = x
        this.y = y
        return this
    }
    override fun toString(): String = "($x,$y)"
}
