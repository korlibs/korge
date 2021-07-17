package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*

open class ImageFrameLayer constructor(
    val layer: ImageLayer,
    val slice: BmpSlice,
    val targetX: Int = 0,
    val targetY: Int = 0,
    val main: Boolean = true,
    val includeInAtlas: Boolean = true,
    val linkedFrameLayer: ImageFrameLayer? = null,
) {
    val width get() = slice.width
    val height get() = slice.height
    val area: Int get() = slice.area
    val bitmap by lazy { slice.extract() }
}
