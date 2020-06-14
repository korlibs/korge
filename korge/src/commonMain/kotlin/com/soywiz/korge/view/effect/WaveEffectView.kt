package com.soywiz.korge.view.effect

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.view.*
import kotlin.math.*

@KorgeDeprecated
@Deprecated("Use View.filter instead")
class WaveEffectView : EffectView() {
	companion object {
		val u_Amplitude = Uniform("amplitude", VarType.Float2)
		val u_crestCount = Uniform("crestCount", VarType.Float2)
		val u_cyclesPerSecond = Uniform("cyclesPerSecond", VarType.Float2)
	}

	private val amplitude = uniforms.storageFor(u_Amplitude)
	private val crestCount = uniforms.storageFor(u_crestCount)
	private val cyclesPerSecond = uniforms.storageFor(u_cyclesPerSecond)

	var amplitudeX by amplitude.intDelegateX(default = 10) { updateBorderEffect() }
	var amplitudeY by amplitude.intDelegateY(default = 10) { updateBorderEffect() }

	var crestCountX by crestCount.floatDelegateX(default = 2f)
	var crestCountY by crestCount.floatDelegateY(default = 2f)

	var cyclesPerSecondX by cyclesPerSecond.floatDelegateX(default = 1f)
	var cyclesPerSecondY by cyclesPerSecond.floatDelegateY(default = 1f)

	private fun updateBorderEffect() {
		borderEffect = max(amplitudeX, amplitudeY)
	}

	init {
		updateBorderEffect()
		fragment = FragmentShader {
			DefaultShaders.apply {
				val tmpx = t_Temp0.x
				val tmpy = t_Temp0.y
				tmpx setTo sin(PI.lit * ((fragmentCoords01.x * u_crestCount.x) + u_Time * u_cyclesPerSecond.x))
				tmpy setTo sin(PI.lit * ((fragmentCoords01.y * u_crestCount.y) + u_Time * u_cyclesPerSecond.y))
				out setTo tex(fragmentCoords - vec2(tmpy * u_Amplitude.x, tmpx * u_Amplitude.y))
				//out["b"] setTo ((sin(u_Time * PI) + 1.0) / 2.0)
			}
		}
	}
}

@KorgeDeprecated
@Deprecated("Use View.filter instead")
inline fun Container.waveEffectView(callback: @ViewsDslMarker WaveEffectView.() -> Unit = {}) =
	WaveEffectView().addTo(this, callback)
