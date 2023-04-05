package korlibs.korge.view.filter

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.property.*
import korlibs.math.geom.*
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
	amplitudeX: Int = 10,
	amplitudeY: Int = 10,
	crestDistanceX: Double = 16.0,
	crestDistanceY: Double = 16.0,
	cyclesPerSecondX: Double = 1.0,
	cyclesPerSecondY: Double = 1.0,
	time: TimeSpan = 0.seconds
) : ShaderFilter() {
    /** Maximum amplitude of the wave on the X axis */
    @ViewProperty
	var amplitudeX: Int = amplitudeX
    /** Maximum amplitude of the wave on the Y axis */
    @ViewProperty
	var amplitudeY: Int = amplitudeY

    /** Distance between crests in the X axis */
    @ViewProperty
	var crestDistanceX: Double = crestDistanceX
    /** Distance between crests in the Y axis */
    @ViewProperty
    var crestDistanceY: Double = crestDistanceY

    /** Number of repetitions of the animation on the X axis per second */
    @ViewProperty
	var cyclesPerSecondX: Double = cyclesPerSecondX
    /** Number of repetitions of the animation on the Y axis per second */
    @ViewProperty
	var cyclesPerSecondY: Double = cyclesPerSecondY

    /** The elapsed time for the animation */
    @ViewProperty
	var time = time

    override val programProvider: ProgramProvider get() = WaveFilter

    override fun updateUniforms(ctx: RenderContext, filterScale: Float) {
        super.updateUniforms(ctx, filterScale)
        ctx[WaveUB].push {
            it[u_Time] = time.seconds
            it[u_Amplitude] = Point(amplitudeX, amplitudeY)
            it[u_crestDistance] = Vector2(crestDistanceX, crestDistanceY)
            it[u_cyclesPerSecond] = Point(cyclesPerSecondX, cyclesPerSecondY)
        }
    }

    override fun computeBorder(texWidth: Int, texHeight: Int): MarginInt {
        return MarginInt(amplitudeY.absoluteValue, amplitudeX.absoluteValue)
    }

    object WaveUB : UniformBlock(fixedLocation = 5) {
        val u_Time by float()
        val u_Amplitude by vec2()
        val u_crestDistance by vec2()
        val u_cyclesPerSecond by vec2()
    }

    companion object : BaseProgramProvider() {
        override val fragment = FragmentShaderDefault {
            SET(t_Temp0["xy"], sin((PI * 2f).lit * ((fragmentCoords / WaveUB.u_crestDistance) + WaveUB.u_Time * WaveUB.u_cyclesPerSecond)))
            SET(out, tex(fragmentCoords - (t_Temp0["yx"] * WaveUB.u_Amplitude)))
        }
    }
}
