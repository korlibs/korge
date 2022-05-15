package com.soywiz.korim.color

import com.soywiz.kmem.clamp
import com.soywiz.korio.util.niceStr
import com.soywiz.korma.interpolation.Interpolable
import com.soywiz.korma.interpolation.MutableInterpolable
import com.soywiz.korma.interpolation.interpolate
import com.soywiz.krypto.encoding.shex

data class ColorTransform(
    private var _mR: Double,
    private var _mG: Double,
    private var _mB: Double,
    private var _mA: Double,
    private var _aR: Int,
    private var _aG: Int,
    private var _aB: Int,
    private var _aA: Int
) : MutableInterpolable<ColorTransform>, Interpolable<ColorTransform> {
    companion object {
        @Deprecated("This being mutable is dangeour", level = DeprecationLevel.ERROR)
        inline val identity: ColorTransform get() = TODO()

        inline fun Multiply(r: Double, g: Double, b: Double, a: Double) = ColorTransform(r, g, b, a, 0, 0, 0, 0)
        inline fun Add(r: Int, g: Int, b: Int, a: Int) = ColorTransform(1, 1, 1, 1, r, g, b, a)
    }

    override fun setToInterpolated(ratio: Double, l: ColorTransform, r: ColorTransform): ColorTransform = setTo(
        ratio.interpolate(l.mR, r.mR),
        ratio.interpolate(l.mG, r.mG),
        ratio.interpolate(l.mB, r.mB),
        ratio.interpolate(l.mA, r.mA),
        ratio.interpolate(l.aR, r.aR),
        ratio.interpolate(l.aG, r.aG),
        ratio.interpolate(l.aB, r.aB),
        ratio.interpolate(l.aA, r.aA)
    )

    override fun interpolateWith(ratio: Double, other: ColorTransform): ColorTransform =
        ColorTransform().setToInterpolated(ratio, this, other)

    private var dirtyColorMul = true
    private var dirtyColorAdd = true

    private var _colorMul: RGBA = Colors.WHITE
    private var _colorAdd: ColorAdd = ColorAdd(0)

    private fun computeColorMul() {
        if (!dirtyColorMul) return
        dirtyColorMul = false
        _colorMul = RGBA.float(_mR.toFloat(), _mG.toFloat(), _mB.toFloat(), _mA.toFloat())
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
            val mR = v.rd
            val mG = v.gd
            val mB = v.bd
            val mA = v.ad
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

    var mR: Double get() = _mR; set(v) { _mR = v; dirtyColorMul = true }
    var mG: Double get() = _mG; set(v) { _mG = v; dirtyColorMul = true }
    var mB: Double get() = _mB; set(v) { _mB = v; dirtyColorMul = true }
    var mA: Double get() = _mA; set(v) { _mA = v; dirtyColorMul = true }

    var mRf: Float get() = _mR.toFloat(); set(v) { _mR = v.toDouble(); dirtyColorMul = true }
    var mGf: Float get() = _mG.toFloat(); set(v) { _mG = v.toDouble(); dirtyColorMul = true }
    var mBf: Float get() = _mB.toFloat(); set(v) { _mB = v.toDouble(); dirtyColorMul = true }
    var mAf: Float get() = _mA.toFloat(); set(v) { _mA = v.toDouble(); dirtyColorMul = true }

    var aR: Int get() = _aR; set(v) { _aR = v; dirtyColorAdd = true }
    var aG: Int get() = _aG; set(v) { _aG = v; dirtyColorAdd = true }
    var aB: Int get() = _aB; set(v) { _aB = v; dirtyColorAdd = true }
    var aA: Int get() = _aA; set(v) { _aA = v; dirtyColorAdd = true }

    var alphaMultiplier: Double
        get() = mA
        set(value) {
            mA = value
        }

    var redMultiplier: Double
        get() = mR
        set(value) {
            mR = value
        }

    var greenMultiplier: Double
        get() = mG
        set(value) {
            mG = value
        }

    var blueMultiplier: Double
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
        mR: Double = 1.0,
        mG: Double = 1.0,
        mB: Double = 1.0,
        mA: Double = 1.0
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
        mR: Double = 1.0,
        mG: Double = 1.0,
        mB: Double = 1.0,
        mA: Double = 1.0,
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
        setTo(1.0, 1.0, 1.0, 1.0, 0, 0, 0, 0)
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
        (mR == 1.0) && (mG == 1.0) && (mB == 1.0) && (mA == 1.0) && (aR == 0) && (aG == 0) && (aB == 0) && (aA == 0)

    fun hasJustAlpha(): Boolean =
        (mR == 1.0) && (mG == 1.0) && (mB == 1.0) && (aR == 0) && (aG == 0) && (aB == 0) && (aA == 0)

    fun setToIdentity() = setTo(1.0, 1.0, 1.0, 1.0, 0, 0, 0, 0)

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
    mR: Number = 1,
    mG: Number = 1,
    mB: Number = 1,
    mA: Number = 1,
    aR: Number = 0,
    aG: Number = 0,
    aB: Number = 0,
    aA: Number = 0
) = ColorTransform(
    mR.toDouble(),
    mG.toDouble(),
    mB.toDouble(),
    mA.toDouble(),
    aR.toInt(),
    aG.toInt(),
    aB.toInt(),
    aA.toInt()
)

fun RGBA.transform(transform: ColorTransform): RGBA = transform.applyToRGBA(this)
