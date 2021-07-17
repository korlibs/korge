package com.soywiz.korim.format

open class ImageAnimation(
    val frames: List<ImageFrame>,
    val direction: Direction,
    val name: String,
) {
    enum class Direction { FORWARD, REVERSE, PING_PONG }
}
