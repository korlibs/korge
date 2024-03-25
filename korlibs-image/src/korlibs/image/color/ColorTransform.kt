package korlibs.image.color

import korlibs.math.clamp
import korlibs.math.interpolation.*
import korlibs.encoding.shex
import korlibs.number.*

data class ColorTransformMul(
    private var _r: Float = 1f,
    private var _g: Float = 1f,
    private var _b: Float = 1f,
    private var _a: Float = 1f,
) {
    private var dirtyColorMul = true

    var r: Float get() = _r
        set(value) {
            _r = value
            dirtyColorMul = true
        }
    var g: Float get() = _g
        set(value) {
            _g = value
            dirtyColorMul = true
        }
    var b: Float
        get() = _b
        set(value) {
            _b = value
            dirtyColorMul = true
        }
    var a: Float
        get() = _a
        set(value) {
            _a = value
            dirtyColorMul = true
        }

    private var _colorMul: RGBA = Colors.WHITE

    var colorMul: RGBA
        set(v) {
            setTo(v.rf, v.gf, v.bf, v.af)
            _colorMul = v
            dirtyColorMul = false
        }
        get() {
            if (dirtyColorMul) {
                dirtyColorMul = false
                _colorMul = RGBA.float(_r, _g, _b, _a)
            }
            return _colorMul
        }

    fun setTo(r: Float, g: Float, b: Float, a: Float) {
        this._r = r
        this._g = g
        this._b = b
        this._a = a
        dirtyColorMul = true
    }
    fun copyFrom(other: ColorTransformMul) = setTo(other._r, other._g, other._b, other._a)
    fun setToConcat(a: ColorTransformMul, b: ColorTransformMul) = setTo(a.r * b.r, a.g * b.g, a.b * b.b, a.a * b.a)
}

data class ColorTransform(
    private var _mR: Float,
    private var _mG: Float,
    private var _mB: Float,
    private var _mA: Float,
    private var _aR: Int,
    private var _aG: Int,
    private var _aB: Int,
    private var _aA: Int
) : MutableInterpolable<ColorTransform>, Interpolable<ColorTransform> {
    companion object {
        inline fun Multiply(r: Double, g: Double, b: Double, a: Double) = ColorTransform(r, g, b, a, 0, 0, 0, 0)
        inline fun Add(r: Int, g: Int, b: Int, a: Int) = ColorTransform(1, 1, 1, 1, r, g, b, a)
    }

    override fun setToInterpolated(ratio: Ratio, l: ColorTransform, r: ColorTransform): ColorTransform = setTo(
        ratio.interpolate(l.mR, r.mR),
        ratio.interpolate(l.mG, r.mG),
        ratio.interpolate(l.mB, r.mB),
        ratio.interpolate(l.mA, r.mA),
        ratio.interpolate(l.aR, r.aR),
        ratio.interpolate(l.aG, r.aG),
        ratio.interpolate(l.aB, r.aB),
        ratio.interpolate(l.aA, r.aA)
    )

    override fun interpolateWith(ratio: Ratio, other: ColorTransform): ColorTransform =
        ColorTransform().setToInterpolated(ratio, this, other)

    private var dirtyColorMul = true
    private var dirtyColorAdd = true

    private var _colorMul: RGBA = Colors.WHITE
    private var _colorAdd: ColorAdd = ColorAdd(0)

    private fun computeColorMul() {
        if (!dirtyColorMul) return
        dirtyColorMul = false
        _colorMul = RGBA.float(_mR, _mG, _mB, _mA)
    }

    private fun computeColorAdd() {
        if (!dirtyColorAdd) return
        dirtyColorAdd = false
        _colorAdd = ColorAdd(_aR, _aG, _aB, _aA)
    }

    var colorMul: RGBA
        get() {
            computeColorMul()
            return _colorMul
        }
        set(v) {
            val mR = v.rf
            val mG = v.gf
            val mB = v.bf
            val mA = v.af
            if (_mR != mR || _mG != mG || _mB != mB || _mA != mA) {
                _mR = mR
                _mG = mG
                _mB = mB
                _mA = mA
                dirtyColorMul = true
            }
        }

    var colorAdd: ColorAdd
        get() {
            //println("%08X".format(computeColors()._colorAdd))
            computeColorAdd()
            return _colorAdd
        }
        set(v) {
            aR = v.r
            aG = v.g
            aB = v.b
            aA = v.a
            if (_aR != aR || _aG != aG || _aB != aB || _aA != aA) {
                _aR = aR
                _aG = aG
                _aB = aB
                _aA = aA
                dirtyColorAdd = true
            }
        }

    var mRd: Double get() = _mR.toDouble(); set(v) { _mR = v.toFloat(); dirtyColorMul = true }
    var mGd: Double get() = _mG.toDouble(); set(v) { _mG = v.toFloat(); dirtyColorMul = true }
    var mBd: Double get() = _mB.toDouble(); set(v) { _mB = v.toFloat(); dirtyColorMul = true }
    var mAd: Double get() = _mA.toDouble(); set(v) { _mA = v.toFloat(); dirtyColorMul = true }

    var mR: Float get() = _mR; set(v) { _mR = v; dirtyColorMul = true }
    var mG: Float get() = _mG; set(v) { _mG = v; dirtyColorMul = true }
    var mB: Float get() = _mB; set(v) { _mB = v; dirtyColorMul = true }
    var mA: Float get() = _mA; set(v) { _mA = v; dirtyColorMul = true }

    var aR: Int get() = _aR; set(v) { _aR = v; dirtyColorAdd = true }
    var aG: Int get() = _aG; set(v) { _aG = v; dirtyColorAdd = true }
    var aB: Int get() = _aB; set(v) { _aB = v; dirtyColorAdd = true }
    var aA: Int get() = _aA; set(v) { _aA = v; dirtyColorAdd = true }

    var alphaMultiplier: Float
        get() = mA
        set(value) {
            mA = value
        }

    var redMultiplier: Float
        get() = mR
        set(value) {
            mR = value
        }

    var greenMultiplier: Float
        get() = mG
        set(value) {
            mG = value
        }

    var blueMultiplier: Float
        get() = mB
        set(value) {
            mB = value
        }

    var alphaOffset: Int
        get() = aA
        set(value) {
            aA = value
        }

    var redOffset: Int
        get() = aR
        set(value) {
            aR = value
        }

    var greenOffset: Int
        get() = aG
        set(value) {
            aG = value
        }

    var blueOffset: Int
        get() = aB
        set(value) {
            aB = value
        }

    fun setMultiplyTo(
        mR: Float = 1f,
        mG: Float = 1f,
        mB: Float = 1f,
        mA: Float = 1f,
    ): ColorTransform {
        this._mR = mR
        this._mG = mG
        this._mB = mB
        this._mA = mA
        dirtyColorMul = true

        return this
    }

    fun setAddTo(
        aR: Int = 0,
        aG: Int = 0,
        aB: Int = 0,
        aA: Int = 0
    ): ColorTransform {
        this._aR = aR
        this._aG = aG
        this._aB = aB
        this._aA = aA
        dirtyColorAdd = true

        return this
    }

    fun setTo(
        mR: Float = 1f,
        mG: Float = 1f,
        mB: Float = 1f,
        mA: Float = 1f,
        aR: Int = 0,
        aG: Int = 0,
        aB: Int = 0,
        aA: Int = 0
    ): ColorTransform = setMultiplyTo(mR, mG, mB, mA).setAddTo(aR, aG, aB, aA)

    fun copyFrom(t: ColorTransform): ColorTransform {
        this._mR = t._mR
        this._mG = t._mG
        this._mB = t._mB
        this._mA = t._mA

        this._aR = t._aR
        this._aG = t._aG
        this._aB = t._aB
        this._aA = t._aA

        this.dirtyColorMul = t.dirtyColorMul
        this.dirtyColorAdd = t.dirtyColorAdd
        this._colorAdd = t._colorAdd
        this._colorMul = t._colorMul

        return this
    }

    fun identity() {
        setTo(1f, 1f, 1f, 1f, 0, 0, 0, 0)
    }

    fun setToConcat(l: ColorTransform, r: ColorTransform) = this.setTo(
        l.mR * r.mR,
        l.mG * r.mG,
        l.mB * r.mB,
        l.mA * r.mA,
        l.aR + r.aR,
        l.aG + r.aG,
        l.aB + r.aB,
        l.aA + r.aA
    )

    override fun toString(): String =
        "ColorTransform(*[${mR.niceStr}, ${mG.niceStr}, ${mB.niceStr}, ${mA.niceStr}]+[$aR, $aG, $aB, $aA])"

    fun isIdentity(): Boolean =
        (mR == 1f) && (mG == 1f) && (mB == 1f) && (mA == 1f) && (aR == 0) && (aG == 0) && (aB == 0) && (aA == 0)

    fun hasJustAlpha(): Boolean =
        (mR == 1f) && (mG == 1f) && (mB == 1f) && (aR == 0) && (aG == 0) && (aB == 0) && (aA == 0)

    fun setToIdentity() = setTo(1f, 1f, 1f, 1f, 0, 0, 0, 0)

    fun applyToColor(color: Int): Int {
        val r = ((RGBA(color).r * mR) + aR).toInt()
        val g = ((RGBA(color).g * mG) + aG).toInt()
        val b = ((RGBA(color).b * mB) + aB).toInt()
        val a = ((RGBA(color).a * mA) + aA).toInt()
        return RGBA.pack(r, g, b, a)
    }

    fun applyToRGBA(color: RGBA): RGBA = RGBA(applyToColor(color.value))
}

inline class ColorAdd(val value: Int) {
    // Alias
    val rgba: Int get() = value

    /** [-255, +255] */
    val r: Int get() = ColorAdd_unpackComponent((value ushr 0) and 0xFF)
    /** [-255, +255] */
    val g: Int get() = ColorAdd_unpackComponent((value ushr 8) and 0xFF)
    /** [-255, +255] */
    val b: Int get() = ColorAdd_unpackComponent((value ushr 16) and 0xFF)
    /** [-255, +255] */
    val a: Int get() = ColorAdd_unpackComponent((value ushr 24) and 0xFF)

    /** [-1f, +1f] */
    val rf: Float get() = r.toFloat() / 0xFF
    /** [-1f, +1f] */
    val gf: Float get() = g.toFloat() / 0xFF
    /** [-1f, +1f] */
    val bf: Float get() = b.toFloat() / 0xFF
    /** [-1f, +1f] */
    val af: Float get() = a.toFloat() / 0xFF

    fun readFloat(out: FloatArray, index: Int = 0) {
        out[index + 0] = rf
        out[index + 1] = gf
        out[index + 2] = bf
        out[index + 3] = af
    }

    fun withR(r: Int) = ColorAdd(r, g, b, a)
    fun withG(g: Int) = ColorAdd(r, g, b, a)
    fun withB(b: Int) = ColorAdd(r, g, b, a)
    fun withA(a: Int) = ColorAdd(r, g, b, a)

    fun toInt() = value

    val shex get() = value.shex

    companion object {
        inline val NEUTRAL get() = ColorAdd_NEUTRAL
        inline operator fun invoke(r: Int, g: Int, b: Int, a: Int): ColorAdd = ColorAdd(ColorAdd_pack(r, g, b, a))
        fun fromFloat(array: FloatArray, index: Int = 0): ColorAdd = fromFloat(
            array[index + 0],
            array[index + 1],
            array[index + 2],
            array[index + 3],
        )
        fun fromFloat(rf: Float, gf: Float, bf: Float, af: Float): ColorAdd = ColorAdd(
            (rf * 255).toInt(),
            (gf * 255).toInt(),
            (bf * 255).toInt(),
            (af * 255).toInt(),
        )
    }
}

@PublishedApi internal val ColorAdd_NEUTRAL = ColorAdd(0x7f7f7f7f)
@PublishedApi internal fun ColorAdd_packComponent(v: Int) = (0x7f + (v shr 1)).clamp(0, 0xFF)
@PublishedApi internal fun ColorAdd_unpackComponent(v: Int): Int = (v - 0x7F) * 2
@PublishedApi internal fun ColorAdd_pack(r: Int, g: Int, b: Int, a: Int) = (ColorAdd_packComponent(r) shl 0) or (ColorAdd_packComponent(g) shl 8) or (ColorAdd_packComponent(b) shl 16) or (ColorAdd_packComponent(a) shl 24)

fun RGBA.toColorAdd() = ColorAdd(r, g, b, a)

inline fun ColorTransform(multiply: RGBA = Colors.WHITE, add: ColorAdd = ColorAdd(0, 0, 0, 0)) =
    ColorTransform(multiply.rf, multiply.gf, multiply.bf, multiply.af, add.r, add.g, add.b, add.a)

@Suppress("NOTHING_TO_INLINE")
inline fun ColorTransform(
    mR: Number = 1f,
    mG: Number = 1f,
    mB: Number = 1f,
    mA: Number = 1f,
    aR: Number = 0,
    aG: Number = 0,
    aB: Number = 0,
    aA: Number = 0
) = ColorTransform(
    mR.toFloat(),
    mG.toFloat(),
    mB.toFloat(),
    mA.toFloat(),
    aR.toInt(),
    aG.toInt(),
    aB.toInt(),
    aA.toInt()
)

fun RGBA.transform(transform: ColorTransform): RGBA = transform.applyToRGBA(this)
