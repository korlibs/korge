package com.soywiz.korma.geom

import com.soywiz.kmem.*
import com.soywiz.korma.annotations.*

typealias PointF = Vector2

@KormaValueApi
//data class Vector2(val x: Float, val y: Float)
inline class Vector2(val data: Long) {
    val x: Float get() = data.toInt().reinterpretAsFloat()
    val y: Float get() = (data ushr 32).toInt().reinterpretAsFloat()
    constructor(x: Float, y: Float) : this(
        (x.reinterpretAsInt().toLong() and 0xFFFFFFFFL)
    )
}

@KormaMutableApi
interface IVector2 {
    var x: Float
    var y: Float
}

@KormaMutableApi
data class MVector2(override var x: Float, override var y: Float) : IVector2
