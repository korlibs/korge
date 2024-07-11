package korlibs.korge.view.filter

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.property.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.time.*
import kotlin.math.*
import kotlin.time.*

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
    fastTime: FastDuration = 0.fastSeconds
) : ShaderFilter() {
    constructor(
        amplitude: Double = 80.0,
        crestCount: Double = 5.0,
        cyclesPerSecond: Double = 2.0,
        time: Duration
    ) : this(amplitude, crestCount, cyclesPerSecond, time.fast)

    object FlagUB : UniformBlock(fixedLocation = 5) {
        val u_amplitude by float()
        val u_crestCount by float()
        val u_cyclesPerSecond by float()
        val u_Time by float()
    }

    companion object : BaseProgramProvider() {
        inline operator fun invoke(
            amplitude: Number = 80.0,
            crestCount: Number = 5.0,
            cyclesPerSecond: Number = 2.0,
            time: Duration = 0.seconds
        ): FlagFilter = FlagFilter(amplitude.toDouble(), crestCount.toDouble(), cyclesPerSecond.toDouble(), time)

        override val fragment: FragmentShader = FragmentShaderDefault {
            //val x01 = fragmentCoords01.x - (ceil(abs(u_amplitude)) / u_TextureSize.x)
            val x01 = createTemp(Float1)
            SET(x01, v_Tex01.x)
            val offsetY = sin((x01 * FlagUB.u_crestCount - FlagUB.u_Time * FlagUB.u_cyclesPerSecond) * PI.toFloat().lit) * FlagUB.u_amplitude * x01
            SET(out, tex(vec2(fragmentCoords.x, fragmentCoords.y - offsetY)))
            //BatchBuilder2D.DO_INPUT_OUTPUT(this, out)
        }
    }

    /** Maximum amplitude of the wave on the Y axis */
    @ViewProperty
    var amplitude: Double = amplitude.toDouble()

    /** Number of wave crests in the X axis */
    @ViewProperty
    var crestCount: Double = crestCount.toDouble()

    /** Number of repetitions of the animation per second */
    @ViewProperty
    var cyclesPerSecond: Double = cyclesPerSecond.toDouble()

    /** The elapsed time for the animation */
    var time: Duration
        set(value) { fastTime = value.fast }
        get() = fastTime.toDuration()

    var fastTime: FastDuration = fastTime

    override val programProvider: ProgramProvider get() = FlagFilter

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        super.updateUniforms(ctx, filterScale)
        ctx[FlagUB].push {
            it[u_amplitude] = amplitude
            it[u_crestCount] = crestCount
            it[u_cyclesPerSecond] = cyclesPerSecond
            it[u_Time] = fastTime.seconds.toFloat()
        }
    }

    override fun computeBorder(texWidth: Int, texHeight: Int): MarginInt {
        return MarginInt(amplitude.absoluteValue.toIntCeil())
    }
}
