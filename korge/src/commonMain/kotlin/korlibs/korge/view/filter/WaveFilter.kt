package korlibs.korge.view.filter

import korlibs.time.*
import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.property.*
import korlibs.math.geom.*
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
    object WaveUB : UniformBlock(fixedLocation = 5) {
        val u_Time by float()
        val u_Amplitude by vec2()
        val u_crestCount by vec2()
        val u_cyclesPerSecond by vec2()
    }

	companion object : BaseProgramProvider() {
        override val fragment = FragmentShaderDefault {
            val tmpx = t_Temp0.x
            val tmpy = t_Temp0.y
            val tmpxy = t_Temp0["zw"]
            SET(tmpxy, v_Tex01)
            SET(tmpx, sin(PI.lit * ((tmpxy.x * WaveUB.u_crestCount.x) + WaveUB.u_Time * WaveUB.u_cyclesPerSecond.x)))
            SET(tmpy, sin(PI.lit * ((tmpxy.y * WaveUB.u_crestCount.y) + WaveUB.u_Time * WaveUB.u_cyclesPerSecond.y)))
            SET(out, tex(fragmentCoords - vec2(tmpy * WaveUB.u_Amplitude.x, tmpx * WaveUB.u_Amplitude.y)))
            //out["b"] setTo ((sin(u_Time * PI) + 1.0) / 2.0)
            //BatchBuilder2D.DO_INPUT_OUTPUT(this, out)
        }
	}

    /** Maximum amplitude of the wave on the X axis */
    @ViewProperty
	var amplitudeX: Int = amplitudeX
    /** Maximum amplitude of the wave on the Y axis */
    @ViewProperty
	var amplitudeY: Int = amplitudeY

    /** Number of wave crests in the X axis */
    @ViewProperty
	var crestCountX: Double = crestCountX
    /** Number of wave crests in the Y axis */
    @ViewProperty
    var crestCountY: Double = crestCountY

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

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        super.updateUniforms(ctx, filterScale)
        ctx[WaveUB].push {
            it[u_Time] = time.seconds
            it[u_Amplitude] = Point(amplitudeX, amplitudeY)
            it[u_crestCount] = Point(crestCountX, crestCountY)
            it[u_cyclesPerSecond] = Point(cyclesPerSecondX, cyclesPerSecondY)
        }
    }

    override fun computeBorder(texWidth: Int, texHeight: Int): MarginInt {
        return MarginInt(amplitudeY.absoluteValue, amplitudeX.absoluteValue)
    }
}
