package com.soywiz.korma.geom

import com.soywiz.korma.annotations.*

@KormaValueApi
data class Scale(val scaleX: Double, val scaleY: Double)

@KormaMutableApi
sealed interface IScale {
    val scaleX: Double
    val scaleY: Double
}

@KormaMutableApi
data class MScale(
    override var scaleX: Double,
    override var scaleY: Double,
) : IScale

