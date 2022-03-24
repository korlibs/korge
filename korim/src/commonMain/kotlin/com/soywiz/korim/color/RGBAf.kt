package com.soywiz.korim.color

import com.soywiz.kmem.*
import com.soywiz.korio.util.*

class RGBAf(
    private var _r: Float = 1f,
    private var _g: Float = 1f,
    private var _b: Float = 1f,
    private var _a: Float = 1f
) {
    init {
        // @TODO: We cannot do clamping here since we use this class for ParticleEmitter with variance (potential negative values)
        //clamp()
    }

    constructor(color: RGBAf) : this(color.r, color.g, color.b, color.a)
    constructor(color: RGBA) : this(color.rf, color.gf, color.bf, color.af)

    private var dirty = true

    fun readFrom(out: FloatArray, index: Int = 0) {
        r = out[index + 0]
        g = out[index + 1]
        b = out[index + 2]
        a = out[index + 3]
    }

    fun writeTo(out: FloatArray, index: Int = 0) {
        out[index + 0] = r
        out[index + 1] = g
        out[index + 2] = b
        out[index + 3] = a
    }

    var r: Float; get() = _r; set(v) { _r = v; makeDirty() }
    var g: Float; get() = _g; set(v) { _g = v; makeDirty() }
    var b: Float; get() = _b; set(v) { _b = v; makeDirty() }
    var a: Float; get() = _a; set(v) { _a = v; makeDirty() }

    var rd: Double; get() = _r.toDouble(); set(v) { _r = v.toFloat(); makeDirty() }
    var gd: Double; get() = _g.toDouble(); set(v) { _g = v.toFloat(); makeDirty() }
    var bd: Double; get() = _b.toDouble(); set(v) { _b = v.toFloat(); makeDirty() }
    var ad: Double; get() = _a.toDouble(); set(v) { _a = v.toFloat(); makeDirty() }

    val ri: Int get() = (r * 255).toInt() and 0xFF
    val gi: Int get() = (g * 255).toInt() and 0xFF
    val bi: Int get() = (b * 255).toInt() and 0xFF
    val ai: Int get() = (a * 255).toInt() and 0xFF

    fun clamp() = this.apply {
        r = r.clamp01()
        g = g.clamp01()
        b = b.clamp01()
        a = a.clamp01()
    }

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

    fun setTo(r: Float, g: Float, b: Float, a: Float): RGBAf {
        this._r = r
        this._g = g
        this._b = b
        this._a = a
        makeDirty()
        return this
    }

    fun setTo(color: RGBA): RGBAf = setTo(color.rf, color.gf, color.bf, color.af)

    fun setTo(color: RGBAf): RGBAf = setTo(color.r, color.g, color.b, color.a)

    fun add(r: Float, g: Float, b: Float, a: Float): RGBAf {
        this.r += r
        this.g += g
        this.b += b
        this.a += a
        return clamp()
    }

    fun copyFrom(that: RGBAf) = setTo(that.r, that.g, that.b, that.a)
    fun setToMultiply(that: RGBAf) = setToMultiply(that.r, that.g, that.b, that.a)
    fun setToMultiply(r: Float, g: Float, b: Float, a: Float) = setTo(this.r * r, this.g * g, this.b * b, this.a * a)

    fun toRGBA(): RGBA = rgba

    fun setToIdentity() = setTo(1f, 1f, 1f, 1f)

    override fun toString(): String = "RGBAf(${r.niceStr}, ${g.niceStr}, ${b.niceStr}, ${a.niceStr})"
    //override fun toString(): String = rgba.hexString

    companion object {
        fun valueOf(hex: String, color: RGBAf = RGBAf()): RGBAf = color.setTo(Colors[hex])
    }
}

inline fun RGBAf(r: Number, g: Number, b: Number, a: Number) = RGBAf(r.toFloat(), g.toFloat(), b.toFloat(), a.toFloat())

fun RGBA.writeFloat(out: FloatArray, index: Int = 0) {
    out[index + 0] = rf
    out[index + 1] = gf
    out[index + 2] = bf
    out[index + 3] = af
}
