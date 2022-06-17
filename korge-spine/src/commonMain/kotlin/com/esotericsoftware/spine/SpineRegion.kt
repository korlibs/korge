package com.esotericsoftware.spine

import com.soywiz.korim.atlas.*
import com.soywiz.korim.format.ImageOrientation

class SpineRegion(val entry: Atlas.Entry) {
    val bmpSlice = entry.slice
    val bmp = bmpSlice.bmpBase
    val texture = bmp
    var rotate = entry.info.imageOrientation != ImageOrientation.ORIGINAL
    val u: Float = bmpSlice.tl_x
    val u2: Float = bmpSlice.br_x
    val v: Float = if (rotate) bmpSlice.br_y else bmpSlice.tl_y
    val v2: Float = if (rotate) bmpSlice.tl_y else bmpSlice.br_y
    var offsetX = (entry.info.virtFrame?.x ?: 0).toFloat()
    var offsetY = (entry.info.virtFrame?.y ?: 0).toFloat()
    var originalWidth = (entry.info.virtFrame?.w ?: entry.info.srcWidth).toFloat()
    var originalHeight = (entry.info.virtFrame?.h ?: entry.info.srcHeight).toFloat()
    var packedWidth = entry.info.srcWidth.toFloat()
    var packedHeight = entry.info.srcHeight.toFloat()
    var degrees = if (rotate) 90 else 0
}
