package com.soywiz.korge.view

import com.soywiz.korge.tween.interpolate
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.util.clamp
import com.soywiz.korma.interpolation.MutableInterpolable
import com.soywiz.korma.numeric.niceStr

// @TODO: Move to Korim
data class ColorTransform(
	private var _mR: Double = 1.0,
	private var _mG: Double = 1.0,
	private var _mB: Double = 1.0,
	private var _mA: Double = 1.0,
	private var _aR: Int = 0,
	private var _aG: Int = 0,
	private var _aB: Int = 0,
	private var _aA: Int = 0
) : MutableInterpolable<ColorTransform> {
	override fun setToInterpolated(l: ColorTransform, r: ColorTransform, ratio: Double): ColorTransform = setTo(
		interpolate(l.mR, r.mR, ratio),
		interpolate(l.mG, r.mG, ratio),
		interpolate(l.mB, r.mB, ratio),
		interpolate(l.mA, r.mA, ratio),
		interpolate(l.aR, r.aR, ratio),
		interpolate(l.aG, r.aG, ratio),
		interpolate(l.aB, r.aB, ratio),
		interpolate(l.aA, r.aA, ratio)
	)

	@Transient private var dirty = true

	private var _colorMul: Int = 0
	private var _colorAdd: Int = 0

	private fun computeColors() = this.apply {
		if (dirty) {
			dirty = false
			_colorMul = RGBA.packf(_mR.toFloat(), _mG.toFloat(), _mB.toFloat(), _mA.toFloat())
			_colorAdd = packAdd(_aR, _aG, _aB, _aA)
		}
	}

	private fun packAdd(r: Int, g: Int, b: Int, a: Int) = (packAddComponent(r) shl 0) or (packAddComponent(g) shl 8) or (packAddComponent(b) shl 16) or (packAddComponent(a) shl 24)
	private fun packAddComponent(v: Int) = (0x7f + (v shr 1)).clamp(0, 0xFF)
	private fun unpackAddComponent(v: Int): Int = (v - 0x7F) * 2

	var colorMul: Int
		get() = computeColors()._colorMul
		set(v) {
			_mR = RGBA.getFastRf(v).toDouble()
			_mG = RGBA.getFastGf(v).toDouble()
			_mB = RGBA.getFastBf(v).toDouble()
			_mA = RGBA.getFastAf(v).toDouble()
			dirty = true
		}

	var colorAdd: Int
		get() = computeColors()._colorAdd
		set(v) {
			_aR = unpackAddComponent(RGBA.getFastR(v))
			_aG = unpackAddComponent(RGBA.getFastG(v))
			_aB = unpackAddComponent(RGBA.getFastB(v))
			_aA = unpackAddComponent(RGBA.getFastA(v))
			dirty = true
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

	fun setMultiply(
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

	fun setAdd(
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

	companion object {
		val identity = ColorTransform()
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

	override fun toString(): String = "ColorTransform(*[${mR.niceStr}, ${mG.niceStr}, ${mB.niceStr}, ${mA.niceStr}]+[$aR, $aG, $aB, $aA])"

	fun isIdentity(): Boolean = (mR == 1.0) && (mG == 1.0) && (mB == 1.0) && (mA == 1.0) && (aR == 0) && (aG == 0) && (aB == 0) && (aA == 0)

	fun hasJustAlpha(): Boolean = (mR == 1.0) && (mG == 1.0) && (mB == 1.0) && (aR == 0) && (aG == 0) && (aB == 0) && (aA == 0)

	fun setToIdentity() = setTo(1.0, 1.0, 1.0, 1.0, 0, 0, 0, 0)
}
