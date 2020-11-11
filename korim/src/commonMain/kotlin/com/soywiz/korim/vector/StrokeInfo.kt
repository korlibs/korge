package com.soywiz.korim.vector

import com.soywiz.korma.geom.vector.*

data class StrokeInfo(
    val thickness: Double = 1.0, val pixelHinting: Boolean = false,
    val scaleMode: LineScaleMode = LineScaleMode.NORMAL,
    val startCap: LineCap = LineCap.BUTT,
    val endCap: LineCap = LineCap.BUTT,
    val lineJoin: LineJoin = LineJoin.MITER,
    val miterLimit: Double = 20.0
)
