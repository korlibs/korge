package korlibs.korge.view.filter

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.property.*
import korlibs.math.*
import korlibs.math.geom.*
import kotlin.math.*

/**
 * A filter that simulates a page of a book.
 */
class PageFilter(
    hratio: Double = 0.0,
    hamplitude0: Double = 0.0,
    hamplitude1: Double = 10.0,
    hamplitude2: Double = 0.0,

    vratio: Double = 0.5,
    vamplitude0: Double = 0.0,
    vamplitude1: Double = 0.0,
    vamplitude2: Double = 0.0,
) : ShaderFilter() {
    object PageUB : UniformBlock(fixedLocation = 5) {
        val u_Offset by vec2()
        val u_HAmplitude by vec4()
        val u_VAmplitude by vec4()
    }

    companion object : BaseProgramProvider() {

        private fun Program.Builder.sin01(arg: Operand) = sin(arg * (PI.toFloat().lit * 0.5f.lit))
        override val fragment = FragmentShaderDefault {
            val x01 = DefaultShaders.t_Temp0["zw"]
            SET(x01, v_Tex01)
            for (n in 0..1) {
                val vr = x01[n]
                val offset = PageUB.u_Offset[n]
                val amplitudes = if (n == 0) PageUB.u_HAmplitude["xyz"] else PageUB.u_VAmplitude["xyz"]
                val tmp = DefaultShaders.t_Temp0[n]
                IF(vr lt offset) {
                    val ratio = ((vr - 0f.lit) / offset)
                    SET(tmp, mix(amplitudes[0], amplitudes[1], sin01(ratio)))
                } ELSE {
                    val ratio = 1f.lit + ((vr - offset) / (1f.lit - offset))
                    SET(tmp, mix(amplitudes[2], amplitudes[1], sin01(ratio)))
                }
            }
            SET(out, tex(fragmentCoords + DefaultShaders.t_Temp0["yx"]))
            //BatchBuilder2D.DO_INPUT_OUTPUT(this, out)
        }
    }

    @ViewProperty
    var hratio: Double = hratio
    @ViewProperty
    var hamplitude0: Double = hamplitude0
    @ViewProperty
    var hamplitude1: Double = hamplitude1
    @ViewProperty
    var hamplitude2: Double = hamplitude2

    @ViewProperty
    var vratio: Double = vratio
    @ViewProperty
    var vamplitude0: Double = vamplitude0
    @ViewProperty
    var vamplitude1: Double = vamplitude1
    @ViewProperty
    var vamplitude2: Double = vamplitude2

    override val programProvider: ProgramProvider get() = PageFilter

    override fun computeBorder(texWidth: Int, texHeight: Int): MarginInt {
        return MarginInt(max(max(abs(hamplitude0), abs(hamplitude1)), abs(hamplitude2)).toIntCeil())
    }

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        super.updateUniforms(ctx, filterScale)

        ctx[PageUB].push {
            it[u_Offset] = Point(hratio, vratio)
            it.set(u_HAmplitude, hamplitude0.toFloat(), hamplitude1.toFloat(), hamplitude2.toFloat(), 0f)
            it.set(u_VAmplitude, vamplitude0.toFloat(), vamplitude1.toFloat(), vamplitude2.toFloat(), 0f)
        }
    }
}
