package com.soywiz.korge.view.fast

import com.soywiz.korge.view.Image
import com.soywiz.korim.bitmap.BitmapSlice
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korma.geom.Angle

open class FastSprite(tex: BmpSlice) {
    var xf: Float = 0f
    var yf: Float = 0f
    var anchorXf: Float = .5f
    var anchorYf: Float = .5f
    //var rotationRadiansf: Float = 0f
    var scalef: Float = 1f

    var tx0: Float = 0f
    var ty0: Float = 0f
    var tx1: Float = 0f
    var ty1: Float = 0f
    var width: Float = 0f
    var height: Float = 0f

    private fun updateTexProps() {
        tx0 = tex.tl_x
        ty0 = tex.tl_y
        tx1 = tex.br_x
        ty1 = tex.br_y
        width = tex.width.toFloat()
        height = tex.height.toFloat()
    }

    var tex: BmpSlice = tex
        set(value) {
            if (field !== value) {
                field = value
                updateTexProps()
            }
        }

    init {
        updateTexProps()
    }

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

    //var rotationRadians: Double
    //    get() = rotationRadiansf.toDouble()
    //    set(value) { rotationRadiansf = value.toFloat() }

    var scale: Double
        get() = scalef.toDouble()
        set(value) { scalef = value.toFloat() }
}
