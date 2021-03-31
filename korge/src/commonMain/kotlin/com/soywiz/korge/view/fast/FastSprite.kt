package com.soywiz.korge.view.fast

import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.color.Colors
import kotlin.math.cos
import kotlin.math.sin

open class FastSprite(tex: BmpSlice) {
    var x0: Float = 0f
    var y0: Float = 0f
    var x1: Float = 0f
    var y1: Float = 0f
    var x2: Float = 0f
    var y2: Float = 0f
    var x3: Float = 0f
    var y3: Float = 0f

    var w: Float = 0f
    var h: Float = 0f

    var ax: Float = 0f
    var ay: Float = 0f

    // cos(rotation) value
    var cr: Float = 1f

    // sin(rotation) value
    var sr: Float = 0f

    var container: FastSpriteContainer? = null
        internal set

    internal var useRotation = false

    /**
     * Updates based on rotation
     */
    private fun updateXY0123() {
        val px = xf - ax
        val py = yf - ay
        val px1 = px + w
        val py1 = py + h

        // top left
        x0 = px * cr - py * sr + xf
        y0 = py * cr + px * sr + yf

        // top right
        x1 = px1 * cr - py * sr + xf
        y1 = py * cr + px1 * sr + yf

        // bottom right
        x2 = px1 * cr - py1 * sr + xf
        y2 = py1 * cr + px1 * sr + yf

        // bottom left
        x3 = px * cr - py1 * sr + xf
        y3 = py1 * cr + px * sr + yf
    }

    /**
     * Updates x without rotation
     */
    private fun updateX01() {
        x0 = xf - ax
        x1 = x0 + w
    }

    private fun updateX() {
        if (useRotation) {
            updateXY0123()
        } else {
            updateX01()
        }
    }

    /**
     * Updates y without rotation
     */
    private fun updateY01() {
        y0 = yf - ay
        y1 = y0 + h
    }

    private fun updateY() {
        if (useRotation) {
            updateXY0123()
        } else {
            updateY01()
        }
    }

    private fun updateXSize() {
        w = width * scaleXf
        ax = w * anchorXf
        updateX()
    }

    private fun updateYSize() {
        h = height * scaleYf
        ay = h * anchorYf
        updateY()
    }


    private fun updateSize() {
        updateXSize()
        updateYSize()
    }

    var xf: Float = 0f
        set(value) {
            field = value
            updateX()
        }
    var yf: Float = 0f
        set(value) {
            field = value
            updateY()
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
    var scaleXf: Float = 1f
        set(value) {
            if (field != value) {
                field = value
                updateXSize()
            }
        }
    var scaleYf: Float = 1f
        set(value) {
            if (field != value) {
                field = value
                updateYSize()
            }
        }
    var rotationRadiansf: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                if (useRotation) {
                    cr = cos(field)
                    sr = sin(field)
                    updateXY0123()
                }
            }
        }

    var color = Colors.WHITE
    var visible: Boolean = true

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

    fun scale(value: Float) {
        scaleXf = value
        scaleYf = value
    }

    init {
        updateSize()
        updateXY0123()
    }
}

var FastSprite.x: Double
    get() = xf.toDouble()
    set(value) {
        xf = value.toFloat()
    }

var FastSprite.y: Double
    get() = yf.toDouble()
    set(value) {
        yf = value.toFloat()
    }

var FastSprite.anchorX: Double
    get() = anchorXf.toDouble()
    set(value) {
        anchorXf = value.toFloat()
    }

var FastSprite.anchorY: Double
    get() = anchorYf.toDouble()
    set(value) {
        anchorYf = value.toFloat()
    }

var FastSprite.scaleX: Double
    get() = scaleXf.toDouble()
    set(value) {
        scaleXf = value.toFloat()
    }
var FastSprite.scaleY: Double
    get() = scaleYf.toDouble()
    set(value) {
        scaleYf = value.toFloat()
    }

fun FastSprite.scale(value: Double) {
    scale(value.toFloat())
}

var FastSprite.rotation: Double
    get() = rotationRadiansf.toDouble()
    set(value) {
        rotationRadiansf = value.toFloat()
    }

var FastSprite.alpha
    get() = color.af
    set(value) {
        color = color.withAf(value)
    }
