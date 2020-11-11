package com.soywiz.korim.color

import com.soywiz.kmem.*
import com.soywiz.korio.util.*
import com.soywiz.korma.interpolation.*
import com.soywiz.krypto.encoding.*

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
		val identity = ColorTransform()

		fun Multiply(r: Double, g: Double, b: Double, a: Double) = ColorTransform(r, g, b, a, 0, 0, 0, 0)
		fun Add(r: Int, g: Int, b: Int, a: Int) = ColorTransform(1, 1, 1, 1, r, g, b, a)
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

	private var dirty = true

	private var _colorMul: RGBA = Colors.WHITE
	private var _colorAdd: ColorAdd = ColorAdd(0)

	private fun computeColors() = this.apply {
		if (dirty) {
			dirty = false
			_colorMul = RGBA.float(_mR.toFloat(), _mG.toFloat(), _mB.toFloat(), _mA.toFloat())
			_colorAdd = ColorAdd(_aR, _aG, _aB, _aA)
		}
	}

	var colorMul: RGBA
		get() = computeColors()._colorMul
		set(v) = run {
            val mR = v.rd
            val mG = v.gd
            val mB = v.bd
            val mA = v.ad
            if (_mR != mR || _mG != mG || _mB != mB || _mA != mA) {
                _mR = mR
                _mG = mG
                _mB = mB
                _mA = mA
                dirty = true
            }
        }

	var colorAdd: ColorAdd
		get() {
			//println("%08X".format(computeColors()._colorAdd))
			return computeColors()._colorAdd
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
                dirty = true
            }
		}

	var mR: Double get() = _mR; set(v) = run { _mR = v; dirty = true }
	var mG: Double get() = _mG; set(v) = run { _mG = v; dirty = true }
	var mB: Double get() = _mB; set(v) = run { _mB = v; dirty = true }
	var mA: Double get() = _mA; set(v) = run { _mA = v; dirty = true }

	var mRf: Float get() = _mR.toFloat(); set(v) = run { _mR = v.toDouble(); dirty = true }
	var mGf: Float get() = _mG.toFloat(); set(v) = run { _mG = v.toDouble(); dirty = true }
	var mBf: Float get() = _mB.toFloat(); set(v) = run { _mB = v.toDouble(); dirty = true }
	var mAf: Float get() = _mA.toFloat(); set(v) = run { _mA = v.toDouble(); dirty = true }

	var aR: Int get() = _aR; set(v) = run { _aR = v; dirty = true }
	var aG: Int get() = _aG; set(v) = run { _aG = v; dirty = true }
	var aB: Int get() = _aB; set(v) = run { _aB = v; dirty = true }
	var aA: Int get() = _aA; set(v) = run { _aA = v; dirty = true }

    var alphaMultiplier: Double
        get() = mA
        set(value) { mA = value }

    var redMultiplier: Double
        get() = mR
        set(value) { mR = value }

    var greenMultiplier: Double
        get() = mG
        set(value) { mG = value }

    var blueMultiplier: Double
        get() = mB
        set(value) { mB = value }

    var alphaOffset: Int
        get() = aA
        set(value) { aA = value }

    var redOffset: Int
        get() = aR
        set(value) { aR = value }

    var greenOffset: Int
        get() = aG
        set(value) { aG = value }

    var blueOffset: Int
        get() = aB
        set(value) { aB = value }

	fun setMultiplyTo(
		mR: Double = 1.0,
		mG: Double = 1.0,
		mB: Double = 1.0,
		mA: Double = 1.0
	): ColorTransform = this.apply {
		this._mR = mR
		this._mG = mG
		this._mB = mB
		this._mA = mA
		dirty = true
	}

	fun setAddTo(
		aR: Int = 0,
		aG: Int = 0,
		aB: Int = 0,
		aA: Int = 0
	): ColorTransform = this.apply {
		this._aR = aR
		this._aG = aG
		this._aB = aB
		this._aA = aA
		dirty = true
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
	): ColorTransform = this.apply {
		this._mR = mR
		this._mG = mG
		this._mB = mB
		this._mA = mA
		this._aR = aR
		this._aG = aG
		this._aB = aB
		this._aA = aA
		dirty = true
	}

	fun copyFrom(t: ColorTransform): ColorTransform {
		this._mR = t._mR
		this._mG = t._mG
		this._mB = t._mB
		this._mA = t._mA

		this._aR = t._aR
		this._aG = t._aG
		this._aB = t._aB
		this._aA = t._aA

		this.dirty = t.dirty
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
	val r get() = unpackComponent((value ushr 0) and 0xFF)
	val g get() = unpackComponent((value ushr 8) and 0xFF)
	val b get() = unpackComponent((value ushr 16) and 0xFF)
	val a get() = unpackComponent((value ushr 24) and 0xFF)

	fun withR(r: Int) = ColorAdd(r, g, b, a)
	fun withG(g: Int) = ColorAdd(r, g, b, a)
	fun withB(b: Int) = ColorAdd(r, g, b, a)
	fun withA(a: Int) = ColorAdd(r, g, b, a)

	fun toInt() = value

    val shex get() = value.shex

	companion object {
        val NEUTRAL = ColorAdd(0x7f7f7f7f)

		operator fun invoke(r: Int, g: Int, b: Int, a: Int) = ColorAdd(pack(r, g, b, a))

		fun packComponent(v: Int) = (0x7f + (v shr 1)).clamp(0, 0xFF)
		fun unpackComponent(v: Int): Int = (v - 0x7F) * 2
		fun pack(r: Int, g: Int, b: Int, a: Int) =
			(ColorAdd.packComponent(r) shl 0) or
					(ColorAdd.packComponent(g) shl 8) or
					(ColorAdd.packComponent(b) shl 16) or
					(ColorAdd.packComponent(a) shl 24)
	}
}

fun RGBA.toColorAdd() = ColorAdd(r, g, b, a)

inline fun ColorTransform(multiply: RGBA, add: ColorAdd = ColorAdd(0, 0, 0, 0)) =
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
