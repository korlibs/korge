package com.soywiz.korge.view.filter

import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.view.property.*
import com.soywiz.korma.geom.*
import kotlin.math.*

/**
 * A Flag [Filter] that distorts the texture using increasing waves to the right, keeping the left-most vertical static,
 * as if connected to a flag pole.
 *
 * [amplitude] is the maximum y amplitude of the waves.
 * [crestCount] is the number of crests of waves on the x axis.
 * [cyclesPerSecond] is the number of times the animation would repeat over a second
 * [time] is the elapsed time of the animation
 */
class FlagFilter(
    amplitude: Double = 80.0,
    crestCount: Double = 5.0,
    cyclesPerSecond: Double = 2.0,
    time: TimeSpan = 0.seconds
) : ShaderFilter() {
    companion object : BaseProgramProvider() {
        val u_amplitude = Uniform("amplitude", VarType.Float1)
        val u_crestCount = Uniform("crestCount", VarType.Float1)
        val u_cyclesPerSecond = Uniform("cyclesPerSecond", VarType.Float1)
        val u_Time = Uniform("time", VarType.Float1)

        override val fragment: FragmentShader = FragmentShaderDefault {
            //val x01 = fragmentCoords01.x - (ceil(abs(u_amplitude)) / u_TextureSize.x)
            val x01 = createTemp(Float1)
            SET(x01, v_Tex01.x)
            val offsetY = sin((x01 * u_crestCount - u_Time * u_cyclesPerSecond) * PI.lit) * u_amplitude * x01
            SET(out, tex(vec2(fragmentCoords.x, fragmentCoords.y - offsetY)))
            //BatchBuilder2D.DO_INPUT_OUTPUT(this, out)
        }
    }

    /** Maximum amplitude of the wave on the Y axis */
    @ViewProperty
    var amplitude: Double by scaledUniforms.storageFor(u_amplitude).doubleDelegateX(amplitude)

    /** Number of wave crests in the X axis */
    @ViewProperty
    var crestCount: Double by uniforms.storageFor(u_crestCount).doubleDelegateX(crestCount)

    /** Number of repetitions of the animation per second */
    @ViewProperty
    var cyclesPerSecond: Double by uniforms.storageFor(u_cyclesPerSecond).doubleDelegateX(cyclesPerSecond)

    /** The elapsed time for the animation in seconds */
    var timeSeconds: Double by uniforms.storageFor(u_Time).doubleDelegateX(default = time.seconds)

    /** The elapsed time for the animation */
    @ViewProperty
    var time: TimeSpan
        get() = timeSeconds.seconds
        set(value) {
            timeSeconds = value.seconds
        }

    override val programProvider: ProgramProvider get() = FlagFilter

    override fun computeBorder(out: MutableMarginInt, texWidth: Int, texHeight: Int) {
        out.setTo(amplitude.absoluteValue.toIntCeil())
    }
}
