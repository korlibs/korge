package com.soywiz.korge.view.filter

import com.soywiz.klock.TimeSpan
import com.soywiz.klock.seconds
import com.soywiz.kmem.toIntCeil
import com.soywiz.korag.shader.FragmentShader
import com.soywiz.korag.shader.Uniform
import com.soywiz.korag.shader.VarType
import com.soywiz.korag.shader.storageFor
import com.soywiz.korge.debug.uiEditableValue
import com.soywiz.korge.view.Views
import com.soywiz.korui.UiContainer
import kotlin.math.PI
import kotlin.math.absoluteValue


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
    companion object {
        val u_amplitude = Uniform("amplitude", VarType.Float1)
        val u_crestCount = Uniform("crestCount", VarType.Float1)
        val u_cyclesPerSecond = Uniform("cyclesPerSecond", VarType.Float1)
        val u_Time = Uniform("time", VarType.Float1)

        private val FRAGMENT_SHADER = FragmentShader {
            apply {
                val x01 = fragmentCoords01.x - (ceil(abs(u_amplitude)) / u_TextureSize.x)
                val offsetY = sin((x01 * u_crestCount - u_Time * u_cyclesPerSecond) * PI.lit) * u_amplitude * x01
                out setTo tex(vec2(fragmentCoords.x, fragmentCoords.y - offsetY))
            }
        }
    }

    /** Maximum amplitude of the wave on the Y axis */
    var amplitude by uniforms.storageFor(u_amplitude).doubleDelegateX(amplitude)

    /** Number of wave crests in the X axis */
    var crestCount by uniforms.storageFor(u_crestCount).doubleDelegateX(crestCount)

    /** Number of repetitions of the animation per second */
    var cyclesPerSecond by uniforms.storageFor(u_cyclesPerSecond).doubleDelegateX(cyclesPerSecond)

    /** The elapsed time for the animation in seconds */
    var timeSeconds by uniforms.storageFor(u_Time).doubleDelegateX(default = time.seconds)

    /** The elapsed time for the animation */
    var time: TimeSpan
        get() = timeSeconds.seconds
        set(value) {
            timeSeconds = value.seconds
        }

    override val border: Int get() = amplitude.absoluteValue.toIntCeil()
    override val fragment = FRAGMENT_SHADER

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiEditableValue(::amplitude)
        container.uiEditableValue(::crestCount)
        container.uiEditableValue(::cyclesPerSecond)
        container.uiEditableValue(::timeSeconds)
    }
}
