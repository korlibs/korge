package com.soywiz.korim.vector

import com.soywiz.kds.IDoubleArrayList
import com.soywiz.korma.geom.vector.LineCap
import com.soywiz.korma.geom.vector.LineJoin

data class StrokeInfo(
    val thickness: Double = 1.0,
    val pixelHinting: Boolean = false,
    val scaleMode: LineScaleMode = LineScaleMode.NORMAL,
    val startCap: LineCap = LineCap.BUTT,
    val endCap: LineCap = LineCap.BUTT,
    val join: LineJoin = LineJoin.MITER,
    val miterLimit: Double = 20.0,
    val dash: IDoubleArrayList? = null,
    val dashOffset: Double = 0.0
)
