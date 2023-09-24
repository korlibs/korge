package korlibs.korge.view.filter

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.property.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.memory.*
import korlibs.time.*
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
    amplitude: Vector2D = Vector2D(10, 10),
    crestDistance: Vector2D = Vector2D(16, 16),
    cyclesPerSecond: Vector2D = Vector2D(1, 1),
    time: TimeSpan = 0.seconds
) : ShaderFilter() {
    /** Maximum amplitude of the wave on the X,Y axis */
    @ViewProperty
	var amplitude: Vector2D = amplitude

    /** Distance between crests in the X,Y axis */
    @ViewProperty
	var crestDistance: Vector2D = crestDistance

    /** Number of repetitions of the animation on the X,Y axis per second */
    @ViewProperty
	var cyclesPerSecond: Vector2D = cyclesPerSecond

    /** The elapsed time for the animation */
    @ViewProperty
	var time = time

    override val programProvider: ProgramProvider get() = WaveFilter

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        super.updateUniforms(ctx, filterScale)
        ctx[WaveUB].push {
            it[u_Time] = time.seconds.toFloat()
            it[u_Amplitude] = amplitude
            it[u_crestDistance] = crestDistance
            it[u_cyclesPerSecond] = cyclesPerSecond
        }
    }

    override fun computeBorder(texWidth: Int, texHeight: Int): MarginInt {
        return MarginInt(amplitude.y.absoluteValue.toIntCeil(), amplitude.x.absoluteValue.toIntCeil())
    }

    object WaveUB : UniformBlock(fixedLocation = 5) {
        val u_Time by float()
        val u_Amplitude by vec2()
        val u_crestDistance by vec2()
        val u_cyclesPerSecond by vec2()
    }

    companion object : BaseProgramProvider() {
        override val fragment = FragmentShaderDefault {
            SET(t_Temp0["xy"], sin((PI.toFloat() * 2f).lit * ((fragmentCoords / WaveUB.u_crestDistance) + WaveUB.u_Time * WaveUB.u_cyclesPerSecond)))
            SET(out, tex(fragmentCoords - (t_Temp0["yx"] * WaveUB.u_Amplitude)))
        }
    }
}
