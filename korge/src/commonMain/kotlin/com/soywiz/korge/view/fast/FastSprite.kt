package com.soywiz.korge.view.fast

import com.soywiz.korge.view.Image
import com.soywiz.korim.bitmap.BitmapSlice
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korma.geom.Angle

open class FastSprite(var tex: BmpSlice) {
    var xf: Float = 0f
    var yf: Float = 0f
    var anchorXf: Float = .5f
    var anchorYf: Float = .5f
    var rotationRadiansf: Float = 0f
    var scalef: Float = 1f

    // @TODO: Set when changing tex
    var tx0: Float = tex.tl_x
    var ty0: Float = tex.tl_y
    var tx1: Float = tex.br_x
    var ty1: Float = tex.br_y
    var width: Float = tex.width.toFloat()
    var height: Float = tex.height.toFloat()

    var x: Double
        get() = xf.toDouble()
        set(value) { xf = value.toFloat() }

    var y: Double
        get() = yf.toDouble()
        set(value) { yf = value.toFloat() }

    var anchorX: Double
        get() = anchorXf.toDouble()
        set(value) { anchorXf = value.toFloat() }

    var anchorY: Double
        get() = anchorYf.toDouble()
        set(value) { anchorYf = value.toFloat() }

    var rotationRadians: Double
        get() = rotationRadiansf.toDouble()
        set(value) { rotationRadiansf = value.toFloat() }

    var scale: Double
        get() = scalef.toDouble()
        set(value) { scalef = value.toFloat() }
}
