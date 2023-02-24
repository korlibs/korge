package com.soywiz.korge.view.filter

import com.soywiz.klock.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.view.property.*
import com.soywiz.korma.geom.*
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
	time: TimeSpan = 0.seconds
) : ShaderFilter() {
	companion object : BaseProgramProvider() {
		val u_Time = Uniform("time", VarType.Float1)
		val u_Amplitude = Uniform("amplitude", VarType.Float2)
		val u_crestCount = Uniform("crestCount", VarType.Float2)
		val u_cyclesPerSecond = Uniform("cyclesPerSecond", VarType.Float2)
        override val fragment = FragmentShaderDefault {
            val tmpx = t_Temp0.x
            val tmpy = t_Temp0.y
            val tmpxy = t_Temp0["zw"]
            SET(tmpxy, v_Tex01)
            SET(tmpx, sin(PI.lit * ((tmpxy.x * u_crestCount.x) + u_Time * u_cyclesPerSecond.x)))
            SET(tmpy, sin(PI.lit * ((tmpxy.y * u_crestCount.y) + u_Time * u_cyclesPerSecond.y)))
            SET(out, tex(fragmentCoords - vec2(tmpy * u_Amplitude.x, tmpx * u_Amplitude.y)))
            //out["b"] setTo ((sin(u_Time * PI) + 1.0) / 2.0)
            //BatchBuilder2D.DO_INPUT_OUTPUT(this, out)
        }
	}

	private val amplitude = scaledUniforms.storageFor(u_Amplitude)
	private val crestCount = uniforms.storageFor(u_crestCount)
	private val cyclesPerSecond = uniforms.storageFor(u_cyclesPerSecond)

    /** Maximum amplitude of the wave on the X axis */
    @ViewProperty
	var amplitudeX: Int by amplitude.intDelegateX(default = amplitudeX)
    /** Maximum amplitude of the wave on the Y axis */
    @ViewProperty
	var amplitudeY: Int by amplitude.intDelegateY(default = amplitudeY)

    /** Number of wave crests in the X axis */
    @ViewProperty
	var crestCountX: Double by crestCount.doubleDelegateX(default = crestCountX)
    /** Number of wave crests in the Y axis */
    @ViewProperty
    var crestCountY: Double by crestCount.doubleDelegateY(default = crestCountY)

    /** Number of repetitions of the animation on the X axis per second */
    @ViewProperty
	var cyclesPerSecondX: Double by cyclesPerSecond.doubleDelegateX(default = cyclesPerSecondX)
    /** Number of repetitions of the animation on the Y axis per second */
    @ViewProperty
	var cyclesPerSecondY: Double by cyclesPerSecond.doubleDelegateY(default = cyclesPerSecondY)

    /** The elapsed time for the animation in seconds */
	var timeSeconds by uniforms.storageFor(u_Time).doubleDelegateX(default = time.seconds)

    /** The elapsed time for the animation */
    @ViewProperty
    var time: TimeSpan
        get() = timeSeconds.seconds
        set(value) { timeSeconds = value.seconds }

    override val programProvider: ProgramProvider get() = WaveFilter

    override fun computeBorder(out: MMarginInt, texWidth: Int, texHeight: Int) {
        out.setTo(amplitudeY.absoluteValue, amplitudeX.absoluteValue)
    }
}
