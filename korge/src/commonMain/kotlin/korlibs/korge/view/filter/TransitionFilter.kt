package korlibs.korge.view.filter

import korlibs.graphics.*
import korlibs.graphics.DefaultShaders.t_Temp1
import korlibs.graphics.shader.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.paint.*
import korlibs.korge.render.*
import korlibs.korge.view.property.*

class TransitionFilter(
    var transition: Transition = Transition.CIRCULAR,
    reversed: Boolean = false,
    spread: Float = 1f,
    ratio: Float = 1f,
    filtering: Boolean = false,
) : ShaderFilter() {
    class Transition(val bmp: Bitmap) {
        fun inverted() = bmp.toBMP32().also { it.invert() }

        companion object {
            private val BMP_SIZE = 64

            private fun createTransitionBox(paint: GradientPaint): Transition {
                return Transition(Bitmap32Context2d(BMP_SIZE, BMP_SIZE) {
                    fill(paint.add(0.0, Colors.WHITE).add(1.0, Colors.BLACK)) {
                        rect(0, 0, BMP_SIZE, BMP_SIZE)
                    }
                })
            }
            private fun createLinearTransitionBox(x0: Int, y0: Int, x1: Int, y1: Int): Transition =
                createTransitionBox(LinearGradientPaint(x0, y0, x1, y1))

            val VERTICAL by lazy { createLinearTransitionBox(0, 0, 0, BMP_SIZE) }
            val HORIZONTAL by lazy { createLinearTransitionBox(0, 0, BMP_SIZE, 0) }
            val DIAGONAL1 by lazy { createLinearTransitionBox(0, 0, BMP_SIZE, BMP_SIZE) }
            val DIAGONAL2 by lazy { createLinearTransitionBox(BMP_SIZE, 0, 0, BMP_SIZE) }
            val CIRCULAR by lazy { createTransitionBox(RadialGradientPaint(BMP_SIZE / 2, BMP_SIZE / 2, 0, BMP_SIZE / 2, BMP_SIZE / 2, BMP_SIZE / 2)) }
            val SWEEP by lazy { createTransitionBox(SweepGradientPaint(BMP_SIZE / 2, BMP_SIZE / 2)) }
        }
    }

    object TransitionUB : UniformBlock(fixedLocation = 5) {
        val u_Reversed by float()
        val u_Spread by float()
        val u_Ratio by float()
    }

    companion object : BaseProgramProvider() {
        private val u_Mask = DefaultShaders.u_TexEx

        override val fragment = DEFAULT_FRAGMENT.appending {
            val alpha = t_Temp1.x
            val spread = t_Temp1.y

            SET(alpha, texture2D(u_Mask, v_Tex01).r)
            IF(TransitionUB.u_Reversed eq 1f.lit) {
                SET(alpha, 1f.lit - alpha)
            }
            SET(alpha, clamp(alpha + ((TransitionUB.u_Ratio * 2f.lit) - 1f.lit), 0f.lit, 1f.lit))
            SET(spread, clamp(TransitionUB.u_Spread, 0.01f.lit, 1f.lit) * 0.5f.lit)
            SET(alpha, smoothstep(clamp01(TransitionUB.u_Ratio - spread), clamp01(TransitionUB.u_Ratio + spread), alpha))

            SET(out, (out * alpha))
            //BatchBuilder2D.DO_INPUT_OUTPUT(this, out)
            //SET(out, texture2D(u_Mask, v_Tex01))
            //SET(out, vec4(1.lit, 0.lit, 1.lit, 1.lit))
        }
    }

    init {
        this.filtering = filtering
    }

    override val programProvider: ProgramProvider get() = TransitionFilter
    @ViewProperty
    var reversed: Boolean = reversed
    @ViewProperty
    var spread: Float = spread
    @ViewProperty
    var ratio: Float = ratio

    override fun updateUniforms(ctx: RenderContext, filterScale: Float) {
        ctx[TransitionUB].push {
            it[u_Reversed] = reversed
            it[u_Spread] = spread
            it[u_Ratio] = ratio
        }
        setTex(ctx, u_Mask, ctx.getTex(transition.bmp).base, AGTextureUnitInfo.DEFAULT)
        //println("ratio=$ratio, s_ratio=$s_ratio, uniformValue=${uniforms[u_Ratio].f32[0]}")
    }
}
