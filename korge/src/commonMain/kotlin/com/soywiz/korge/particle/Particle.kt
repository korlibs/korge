package com.soywiz.korge.particle

import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*

data class Particle(
    val index: Int,

    var x: Double = 0.0,
    var y: Double = 0.0,
    var scale: Double = 1.0,
    var rotation: Angle = 0.0.degrees,
    var currentTime: Double = 0.0,
    var totalTime: Double = 0.0,

    //val colorArgb: RGBAf = RGBAf(),
    //val colorArgbDelta: RGBAf = RGBAf(),

    var colorR: Double = 1.0,
    var colorG: Double = 1.0,
    var colorB: Double = 1.0,
    var colorA: Double = 1.0,

    var colorRdelta: Double = 0.0,
    var colorGdelta: Double = 0.0,
    var colorBdelta: Double = 0.0,
    var colorAdelta: Double = 0.0,

    var startX: Double = 0.0,
    var startY: Double = 0.0,
    var velocityX: Double = 0.0,
    var velocityY: Double = 0.0,
    var radialAcceleration: Double = 0.0,
    var tangentialAcceleration: Double = 0.0,
    var emitRadius: Double = 0.0,
    var emitRadiusDelta: Double = 0.0,
    var emitRotation: Angle = 0.0.degrees,
    var emitRotationDelta: Angle = 0.0.degrees,
    var rotationDelta: Angle = 0.0.degrees,
    var scaleDelta: Double = 0.0
) {
    val color: RGBA get() = RGBA.float(colorR.toFloat(), colorG.toFloat(), colorB.toFloat(), colorA.toFloat())
    val alive: Boolean get() = this.currentTime >= 0.0 && this.currentTime < this.totalTime
}
