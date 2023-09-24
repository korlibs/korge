package korlibs.image.color

import korlibs.math.*
import korlibs.math.geom.*
import korlibs.number.*

//inline class RGBAf private constructor(
inline class RGBAf(val data: FloatArray) {
//data class RGBAf(val r: Float, val g: Float, val b: Float, val a: Float)
    constructor(r: Float = 1f, g: Float = 1f, b: Float = 1f, a: Float = 1f) : this(floatArrayOf(r, g, b, a))
    constructor(color: RGBA) : this(color.rf, color.gf, color.bf, color.af)

    companion object {
        operator fun invoke(color: RGBAf): RGBAf = RGBAf(color.r, color.g, color.b, color.a)
        fun valueOf(hex: String, color: RGBAf = RGBAf()): RGBAf = color.setTo(Colors[hex])
    }

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

    var r: Float; get() = data[0]; set(v) { data[0] = v }
    var g: Float; get() = data[1]; set(v) { data[1] = v }
    var b: Float; get() = data[2]; set(v) { data[2] = v }
    var a: Float; get() = data[3]; set(v) { data[3] = v }

    var rd: Double; get() = r.toDouble(); set(v) { r = v.toFloat() }
    var gd: Double; get() = g.toDouble(); set(v) { g = v.toFloat() }
    var bd: Double; get() = b.toDouble(); set(v) { b = v.toFloat() }
    var ad: Double; get() = a.toDouble(); set(v) { a = v.toFloat() }

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

    val rgba: RGBA
        get() = RGBA.float(r, g, b, a)

    fun setTo(r: Float, g: Float, b: Float, a: Float): RGBAf {
        this.r = r
        this.g = g
        this.b = b
        this.a = a
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
    fun toVector(): Vector4F = Vector4F(r, g, b, a)

    fun setToIdentity() = setTo(1f, 1f, 1f, 1f)

    override fun toString(): String = "RGBAf(${r.niceStr}, ${g.niceStr}, ${b.niceStr}, ${a.niceStr})"
    //override fun toString(): String = rgba.hexString
}

inline fun RGBAf(r: Number, g: Number, b: Number, a: Number): RGBAf = RGBAf(r.toFloat(), g.toFloat(), b.toFloat(), a.toFloat())

fun RGBA.writeFloat(out: FloatArray, index: Int = 0) {
    out[index + 0] = rf
    out[index + 1] = gf
    out[index + 2] = bf
    out[index + 3] = af
}
