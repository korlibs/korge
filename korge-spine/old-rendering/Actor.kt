package com.esotericsoftware.spine.rendering

open class Actor {

    val y: Float
        get() = 0f

    val x: Float
        get() = 0f

    open fun act(delta: Float) {}

    fun remove() {}
}
