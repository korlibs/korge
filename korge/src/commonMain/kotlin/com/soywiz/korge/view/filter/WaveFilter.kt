package com.soywiz.korge.view.filter

import com.soywiz.korag.DefaultShaders.t_Temp0
import com.soywiz.korag.shader.*
import kotlin.math.*

class WaveFilter(
	amplitudeX: Int = 10,
	amplitudeY: Int = 10,
	crestCountX: Double = 2.0,
	crestCountY: Double = 2.0,
	cyclesPerSecondX: Double = 1.0,
	cyclesPerSecondY: Double = 1.0,
	time: Double = 0.0
) : Filter() {
	companion object {
		val u_Time = Uniform("time", VarType.Float1)
		val u_Amplitude = Uniform("amplitude", VarType.Float2)
		val u_crestCount = Uniform("crestCount", VarType.Float2)
		val u_cyclesPerSecond = Uniform("cyclesPerSecond", VarType.Float2)
	}

	private val amplitude = uniforms.storageFor(u_Amplitude)
	private val crestCount = uniforms.storageFor(u_crestCount)
	private val cyclesPerSecond = uniforms.storageFor(u_cyclesPerSecond)

	var amplitudeX by amplitude.intDelegateX(default = amplitudeX)
	var amplitudeY by amplitude.intDelegateY(default = amplitudeY)

	var crestCountX by crestCount.doubleDelegateX(default = crestCountX)
	var crestCountY by crestCount.doubleDelegateY(default = crestCountY)

	var cyclesPerSecondX by cyclesPerSecond.doubleDelegateX(default = cyclesPerSecondX)
	var cyclesPerSecondY by cyclesPerSecond.doubleDelegateY(default = cyclesPerSecondY)

	var time by uniforms.storageFor(u_Time).doubleDelegateX(default = time)

	override val border: Int get() = max(amplitudeX, amplitudeY)

	init {
		fragment = FragmentShader {
			apply {
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
