package com.soywiz.korge.view.fast

import com.soywiz.korim.bitmap.*

open class FastSprite(tex: BmpSlice) {
    var x0: Float = 0f
    var y0: Float = 0f
    var x1: Float = 0f
    var y1: Float = 0f

    var w: Float = 0f
    var h: Float = 0f

    var ax: Float = 0f
    var ay: Float = 0f

    private fun updateX01() {
        x0 = xf - ax
        x1 = x0 + w
    }

    private fun updateY01() {
        y0 = yf - ay
        y1 = y0 + h
    }

    private fun updateXY01() {
        updateX01()
        updateY01()
    }

    private fun updateXSize() {
        w = width * scalef
        ax = w * anchorXf
        updateX01()
    }

    private fun updateYSize() {
        h = height * scalef
        ay = h * anchorYf
        updateY01()
    }

    private fun updateSize() {
        updateXSize()
        updateYSize()
    }

    var xf: Float = 0f
        set(value) {
            field = value
            updateX01()
        }
    var yf: Float = 0f
        set(value) {
            field = value
            updateY01()
        }
    var anchorXf: Float = .5f
        set(value) {
            if (field != value) {
                field = value
                updateXSize()
            }
        }
    var anchorYf: Float = .5f
        set(value) {
            if (field != value) {
                field = value
                updateYSize()
            }
        }
    var scalef: Float = 1f
        set(value) {
            if (field != value) {
                field = value
                updateSize()
            }
        }

    val tx0: Float get() = tex.tl_x
    val ty0: Float get() = tex.tl_y
    val tx1: Float get() = tex.br_x
    val ty1: Float get() = tex.br_y
    val width: Float get() = tex.width.toFloat()
    val height: Float get() = tex.height.toFloat()

    var tex: BmpSlice = tex
        set(value) {
            if (field !== value) {
                field = value
                updateSize()
            }
        }

    init {
        updateSize()
        updateXY01()
    }
}

var FastSprite.x: Double
    get() = xf.toDouble()
    set(value) { xf = value.toFloat() }

var FastSprite.y: Double
    get() = yf.toDouble()
    set(value) { yf = value.toFloat() }

var FastSprite.anchorX: Double
    get() = anchorXf.toDouble()
    set(value) { anchorXf = value.toFloat() }

var FastSprite.anchorY: Double
    get() = anchorYf.toDouble()
    set(value) { anchorYf = value.toFloat() }

var FastSprite.scale: Double
    get() = scalef.toDouble()
    set(value) { scalef = value.toFloat() }
