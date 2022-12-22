package com.soywiz.korge.view.filter

import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.ShaderFilter.Companion.fragmentCoords
import com.soywiz.korge.view.filter.ShaderFilter.Companion.fragmentCoords01
import com.soywiz.korge.view.filter.ShaderFilter.Companion.tex
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*

/**
 * Abstract class for [View] [Filter]s that paints the [Texture] using a [FragmentShader] ([fragment]).
 *
 * Inherited versions of this filter, usually inherit [computeBorder] and [fragment] properties.
 *
 * When building shaders by calling FragmentShader { ... }, you will have additionally access to:
 * [fragmentCoords], [fragmentCoords01] properties and [tex] method to be used inside the shader.
 */
abstract class ShaderFilter : Filter {
    companion object {
        //val u_Time = Uniform("time", VarType.Float1)
        val u_TextureSize = Uniform("effectTextureSize", VarType.Float2)
        val u_MaxTexCoords = Uniform("u_MaxTexCoords", VarType.Float2)
        val u_StdTexDerivates = Uniform("u_StdTexDerivates", VarType.Float2)
        val u_filterScale = Uniform("u_filterScale", VarType.Float1)

        val Program.ExpressionBuilder.v_Tex01: Operand get() = (DefaultShaders.v_Tex["xy"] / u_MaxTexCoords)

        val Program.ExpressionBuilder.fragmentCoords01: Operand get() = DefaultShaders.v_Tex["xy"]
        val Program.ExpressionBuilder.fragmentCoords: Operand get() = fragmentCoords01 * u_TextureSize
        fun Program.ExpressionBuilder.texture2DZeroOutside(sampler: Operand, coords: Operand, check: Boolean = true): Operand = if (check) {
            TERNARY(
                //(step(vec2(0f.lit, 0f.lit), coords) - step(vec2(1f.lit, 1f.lit), coords)) eq vec2(0f.lit, 0f.lit),
                coords.x.inRange(0f.lit, 1f.lit) and coords.y.inRange(0f.lit, 1f.lit),
                texture2D(sampler, coords),
                vec4(0f.lit)
            )
        } else{
            texture2D(sampler, coords)
        }
        //return texture2D(sampler, coords)
        // @TODO: Here it should premultiply if required
        fun Program.ExpressionBuilder.tex(coords: Operand): Operand = texture2D(DefaultShaders.u_Tex, coords / u_TextureSize)
    }

    interface ProgramProvider {
        fun getProgram(): Program
    }

    open class BaseProgramProvider : ProgramProvider {
        companion object : BaseProgramProvider()

        protected fun createProgram(vertex: VertexShader, fragment: FragmentShader): Program {
            return Program(vertex, fragment.appending {
                // Required for shape masks:
                IF(out["a"] le 0f.lit) { DISCARD() }
            })
        }

        /** The [VertexShader] used this [Filter] */
        protected open val vertex: VertexShader = BatchBuilder2D.VERTEX

        /** The [FragmentShader] used this [Filter]. This is usually overriden. */
        protected open val fragment: FragmentShader = Filter.DEFAULT_FRAGMENT

        private val _program: Program by lazy { createProgram(vertex, fragment) }

        override fun getProgram(): Program = _program
    }

    var filtering = true

    private val textureSizeHolder = FloatArray(2)
    private val textureMaxTexCoords = FloatArray(2)
    private val textureStdTexDerivates = FloatArray(2)

    val scaledUniforms = AGUniformValues()

    val uniforms = AGUniformValues {
        //Filter.u_Time to timeHolder,
        it[u_TextureSize] = textureSizeHolder
        it[u_MaxTexCoords] = textureMaxTexCoords
        it[u_StdTexDerivates] = textureStdTexDerivates
    }

    override fun computeBorder(out: MutableMarginInt, texWidth: Int, texHeight: Int) {
        out.setTo(0)
    }

    ///** The [VertexShader] used this this [Filter] */
    //open val vertex: VertexShader = BatchBuilder2D.VERTEX
    ///** The [FragmentShader] used this [Filter]. This is usually overriden. */
    //open val fragment: FragmentShader = Filter.DEFAULT_FRAGMENT
    //private val programPremult: Program by lazy { createProgram(vertex, fragment, true) }
    //private val programNormal: Program by lazy { createProgram(vertex, fragment, false) }

    open val programProvider: ProgramProvider = BaseProgramProvider

    //@CallSuper
    protected open fun updateUniforms(ctx: RenderContext, filterScale: Double) {
    }

    private fun _updateUniforms(ctx: RenderContext, filterScale: Double) {
        uniforms[u_filterScale] = filterScale
        uniforms[u_TextureSize] = textureSizeHolder
        uniforms[u_MaxTexCoords] = textureMaxTexCoords
        uniforms[u_StdTexDerivates] = textureStdTexDerivates

        scaledUniforms.fastForEach { value ->
            when (value.type.kind) {
                VarKind.TFLOAT -> {
                    val out = uniforms[value.uniform].f32
                    for (n in 0 until out.size) out[n] = (value.f32[n] * filterScale).toFloat()
                }
                else -> TODO()
            }
        }
        updateUniforms(ctx, filterScale)
    }

    //private var slice: MutableBmpCoordsWithInstanceBase<TextureBase>? = null

    open val isIdentity: Boolean get() = false

    override fun render(
        ctx: RenderContext,
        matrix: Matrix,
        texture: Texture,
        texWidth: Int,
        texHeight: Int,
        renderColorAdd: ColorAdd,
        renderColorMul: RGBA,
        blendMode: BlendMode,
        filterScale: Double,
    ) {
        if (isIdentity) return IdentityFilter.render(ctx, matrix, texture, texWidth, texHeight, renderColorAdd, renderColorMul, blendMode, filterScale)

        val _margin = getBorder(texWidth, texHeight, ctx.tempMargin)
        val marginLeft = (_margin.left * filterScale).toIntCeil()
        val marginRight = (_margin.right * filterScale).toIntCeil()
        val marginTop = (_margin.top * filterScale).toIntCeil()
        val marginBottom = (_margin.bottom * filterScale).toIntCeil()
        //if (slice == null) {
        //    slice = MutableBmpCoordsWithInstanceBase(texture.base, texture)
        //}
        //slice!!.setBasicCoords(
        //    texture.xcoord(-marginLeft),
        //    texture.ycoord(-marginTop),
        //    texture.xcoord(texture.width + marginRight),
        //    texture.ycoord(texture.height + marginBottom),
        //)

        //println("$this.render()")
        // @TODO: Precompute vertices
        textureSizeHolder[0] = texture.base.width.toFloat()
        textureSizeHolder[1] = texture.base.height.toFloat()
        textureStdTexDerivates[0] = 1f / texture.base.width.toFloat()
        textureStdTexDerivates[1] = 1f / texture.base.height.toFloat()
        textureMaxTexCoords[0] = texWidth.toFloat() / texture.base.width.toFloat()
        textureMaxTexCoords[1] = texHeight.toFloat() / texture.base.height.toFloat()
        _updateUniforms(ctx, filterScale)

        ctx.useBatcher { batch ->
            batch.setTemporalUniforms(this.uniforms) {
                //println("renderColorMulInt=" + RGBA(renderColorMulInt))
                //println("blendMode:$blendMode")

                val slice = texture.sliceBoundsUnclamped(-marginLeft, -marginTop, texture.width + marginRight, texture.height + marginBottom)

                //println("matrix=$matrix, slice=$slice, marginLeft=$marginLeft")
                batch.drawQuad(
                    slice,
                    x = -marginLeft.toFloat(),
                    y = -marginTop.toFloat(),
                    m = matrix,
                    filtering = filtering,
                    colorAdd = renderColorAdd,
                    colorMul = renderColorMul,
                    blendMode = blendMode,
                    //program = if (texture.premultiplied) programPremult else programNormal
                    program = programProvider.getProgram(),
                )
                //ctx.batch.flush()
            }
        }
    }
}
