package korlibs.korge.view.filter

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.property.*
import korlibs.math.geom.*
import korlibs.memory.*
import kotlin.math.*

/**
 * A filter that simulates a page of a book.
 */
class PageFilter(
    hratio: Float = 0f,
    hamplitude0: Float = 0f,
    hamplitude1: Float = 10f,
    hamplitude2: Float = 0f,

    vratio: Float = 0.5f,
    vamplitude0: Float = 0f,
    vamplitude1: Float = 0f,
    vamplitude2: Float = 0f
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
    var hratio: Float = hratio
    @ViewProperty
    var hamplitude0: Float = hamplitude0
    @ViewProperty
    var hamplitude1: Float = hamplitude1
    @ViewProperty
    var hamplitude2: Float = hamplitude2

    @ViewProperty
    var vratio: Float = vratio
    @ViewProperty
    var vamplitude0: Float = vamplitude0
    @ViewProperty
    var vamplitude1: Float = vamplitude1
    @ViewProperty
    var vamplitude2: Float = vamplitude2

    override val programProvider: ProgramProvider get() = PageFilter

    override fun computeBorder(texWidth: Int, texHeight: Int): MarginInt {
        return MarginInt(max(max(abs(hamplitude0), abs(hamplitude1)), abs(hamplitude2)).toIntCeil())
    }

    override fun updateUniforms(ctx: RenderContext, filterScale: Float) {
        super.updateUniforms(ctx, filterScale)

        ctx[PageUB].push {
            it[u_Offset] = Point(hratio, vratio)
            it.set(u_HAmplitude, hamplitude0.toFloat(), hamplitude1.toFloat(), hamplitude2.toFloat(), 0f)
            it.set(u_VAmplitude, vamplitude0.toFloat(), vamplitude1.toFloat(), vamplitude2.toFloat(), 0f)
        }
    }
}
