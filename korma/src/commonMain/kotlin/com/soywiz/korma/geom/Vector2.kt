package com.soywiz.korma.geom

import com.soywiz.korma.annotations.*

typealias PointF = Vector2

@KormaValueApi
data class Vector2(val x: Float, val y: Float)

@KormaMutableApi
interface IVector2 {
    var x: Float
    var y: Float
}

@KormaMutableApi
data class MVector2(override var x: Float, override var y: Float) : IVector2
