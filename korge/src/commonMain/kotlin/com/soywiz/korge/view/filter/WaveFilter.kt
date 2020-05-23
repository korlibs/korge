package com.soywiz.korge.view.filter

import com.soywiz.klock.*
import com.soywiz.korag.DefaultShaders.t_Temp0
import com.soywiz.korag.shader.*
import com.soywiz.korge.internal.*
import kotlin.math.*

/**
 * A Wave [Filter] that distorts the texture using waves.
 *
 * [amplitudeX], [amplitudeY] is the maximum x and y amplitudes of the waves.
 * [crestCountX] and [crestCountY] is the number of crests of waves per axis
 * [cyclesPerSecondX] and [cyclesPerSecondY] is the number of times the animation would repeat over a second
 * [time] is the elapsed time of the animation
 */
class WaveFilter(
	amplitudeX: Int = 10,
	amplitudeY: Int = 10,
	crestCountX: Double = 2.0,
	crestCountY: Double = 2.0,
	cyclesPerSecondX: Double = 1.0,
	cyclesPerSecondY: Double = 1.0,
	time: TimeSpan = 0.secs
) : ShaderFilter() {
	companion object {
		val u_Time = Uniform("time", VarType.Float1)
		val u_Amplitude = Uniform("amplitude", VarType.Float2)
		val u_crestCount = Uniform("crestCount", VarType.Float2)
		val u_cyclesPerSecond = Uniform("cyclesPerSecond", VarType.Float2)
        private val FRAGMENT_SHADER = FragmentShader {
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

	private val amplitude = uniforms.storageFor(u_Amplitude)
	private val crestCount = uniforms.storageFor(u_crestCount)
	private val cyclesPerSecond = uniforms.storageFor(u_cyclesPerSecond)

    /** Maximum amplitude of the wave on the X axis */
	var amplitudeX by amplitude.intDelegateX(default = amplitudeX)
    /** Maximum amplitude of the wave on the Y axis */
	var amplitudeY by amplitude.intDelegateY(default = amplitudeY)

    /** Number of wave crests in the X axis */
	var crestCountX by crestCount.doubleDelegateX(default = crestCountX)
    /** Number of wave crests in the Y axis */
	var crestCountY by crestCount.doubleDelegateY(default = crestCountY)

    /** Number of repetitions of the animation on the X axis per second */
	var cyclesPerSecondX by cyclesPerSecond.doubleDelegateX(default = cyclesPerSecondX)
    /** Number of repetitions of the animation on the Y axis per second */
	var cyclesPerSecondY by cyclesPerSecond.doubleDelegateY(default = cyclesPerSecondY)

    /** The elapsed time for the animation in seconds */
	var timeSeconds by uniforms.storageFor(u_Time).doubleDelegateX(default = time.seconds)

    /** The elapsed time for the animation */
    var time: TimeSpan
        get() = timeSeconds.secs
        set(value) { timeSeconds = value.seconds }

	override val border: Int get() = max(amplitudeX, amplitudeY)
    override val fragment = FRAGMENT_SHADER
}
