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
     * Allows FastSpriteContainer to recalculate a FastSprite if added to a FastSpriteContainer using rotation
     */
    internal fun forceUpdate() {
        if (useRotation) {
            cr = cos(rotationRadiansf)
            sr = sin(rotationRadiansf)
        }
        updateSize()
    }

    /**
     * Updates based on rotation
     */
    private fun updateXY0123() {
        // top left
        var px = -ax * scaleXf
        var py = -ay * scaleYf
        x0 = px * cr - py * sr + xf
        y0 = py * cr + px * sr + yf

        // top right
        px = (-ax + width) * scaleXf
        py = -ay * scaleYf
        x1 = px * cr - py * sr + xf
        y1 = py * cr + px * sr + yf


        // bottom right
        px = (-ax + width) * scaleXf
        py = (-ay + height) * scaleYf
        x2 = px * cr - py * sr + xf
        y2 = py * cr + px * sr + yf

        // bottom left
        px = -ax * scaleXf
        py = (-ay + height) * scaleYf
        x3 = px * cr - py * sr + xf
        y3 = py * cr + px * sr + yf
    }

    /**
     * Updates x without rotation
     */
    private fun updateX01() {
        x0 = xf - ax * scaleXf
        x1 = x0 + width * scaleXf
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
        y0 = yf - ay * scaleYf
        y1 = y0 + height * scaleYf
    }

    private fun updateY() {
        if (useRotation) {
            updateXY0123()
        } else {
            updateY01()
        }
    }

    private fun updateXSize() {
        ax = width * anchorXf
        updateX()
    }

    private fun updateYSize() {
        ay = height * anchorYf
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

    override fun toString(): String {
        return "FastSprite(x0=$x0, y0=$y0, x1=$x1, y1=$y1, x2=$x2, y2=$y2, x3=$x3, y3=$y3, ax=$ax, ay=$ay, cr=$cr, sr=$sr, container=$container, useRotation=$useRotation, xf=$xf, yf=$yf, anchorXf=$anchorXf, anchorYf=$anchorYf, scaleXf=$scaleXf, scaleYf=$scaleYf, rotationRadiansf=$rotationRadiansf, color=$color, visible=$visible, tex=$tex)"
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
