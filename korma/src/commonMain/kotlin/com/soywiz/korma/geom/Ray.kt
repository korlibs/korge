package com.soywiz.korma.geom

import com.soywiz.korma.annotations.*

@KormaValueApi
data class Ray(val point: Point, val direction: Point)

@KormaMutableApi
interface IRay {
    val point: IPoint
    val direction: IVector2D
}

@KormaMutableApi
data class MRay(override val point: MPoint, override val direction: MVector2D) : IRay
