package com.soywiz.korim.format

open class ImageAnimation(
    val frames: List<ImageFrame>,
    val direction: Direction,
    val name: String,
    val layers: List<ImageLayer> = frames.flatMap { it.layerData }.map { it.layer }.distinct().sortedBy { it.index }
) {
    enum class Direction { ONCE_FORWARD, ONCE_REVERSE, FORWARD, REVERSE, PING_PONG }
}
