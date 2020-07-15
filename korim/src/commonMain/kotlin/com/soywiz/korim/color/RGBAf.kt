package com.soywiz.korim.color

import com.soywiz.korio.util.*

class RGBAf(
    private var _r: Float = 1f,
    private var _g: Float = 1f,
    private var _b: Float = 1f,
    private var _a: Float = 1f
) {
    private var dirty = true

    var r: Float; set(v) = run { _r = v; makeDirty() }; get() = _r
    var g: Float; set(v) = run { _g = v; makeDirty() }; get() = _g
    var b: Float; set(v) = run { _b = v; makeDirty() }; get() = _b
    var a: Float; set(v) = run { _a = v; makeDirty() }; get() = _a

    var rd: Double; set(v) = run { _r = v.toFloat(); makeDirty() }; get() = _r.toDouble()
    var gd: Double; set(v) = run { _g = v.toFloat(); makeDirty() }; get() = _g.toDouble()
    var bd: Double; set(v) = run { _b = v.toFloat(); makeDirty() }; get() = _b.toDouble()
    var ad: Double; set(v) = run { _a = v.toFloat(); makeDirty() }; get() = _a.toDouble()

    private fun makeDirty() {
        dirty = true
    }

    private var _rgba: RGBA = RGBA(-1)
    val rgba: RGBA
        get() {
            if (dirty) {
                dirty = false
                _rgba = RGBA.float(_r, _g, _b, _a)
            }
            return _rgba
        }

    fun setTo(r: Float, g: Float, b: Float, a: Float) {
        this._r = r
        this._g = g
        this._b = b
        this._a = a
        makeDirty()
    }

    fun copyFrom(that: RGBAf) = setTo(that.r, that.g, that.b, that.a)
    fun setToMultiply(that: RGBAf) = setToMultiply(that.r, that.g, that.b, that.a)
    fun setToMultiply(r: Float, g: Float, b: Float, a: Float) = setTo(this.r * r, this.g * g, this.b * b, this.a * a)

    fun toRGBA(): RGBA = RGBA(
        (r * 255).toInt() and 0xFF,
        (g * 255).toInt() and 0xFF,
        (b * 255).toInt() and 0xFF,
        (a * 255).toInt() and 0xFF
    )

    fun setToIdentity() = setTo(1f, 1f, 1f, 1f)

    override fun toString(): String = "RGBAf(${r.niceStr}, ${g.niceStr}, ${b.niceStr}, ${a.niceStr})"
}

inline fun RGBAf(r: Number, g: Number, b: Number, a: Number) = RGBAf(r.toFloat(), g.toFloat(), b.toFloat(), a.toFloat())
