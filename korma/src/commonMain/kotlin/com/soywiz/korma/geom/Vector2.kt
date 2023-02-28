package com.soywiz.korma.geom

import com.soywiz.kmem.*
import com.soywiz.korma.annotations.*

typealias PointF = Vector2

typealias Vector2 = Point

@KormaMutableApi
interface IVector2 {
    var x: Float
    var y: Float
}

@KormaMutableApi
data class MVector2(override var x: Float, override var y: Float) : IVector2
