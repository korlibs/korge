package com.soywiz.korge.view.filter

import com.soywiz.kmem.toIntCeil
import com.soywiz.korag.AG
import com.soywiz.korag.DefaultShaders
import com.soywiz.korag.shader.FragmentShader
import com.soywiz.korag.shader.Operand
import com.soywiz.korag.shader.Program
import com.soywiz.korag.shader.Uniform
import com.soywiz.korag.shader.VarType
import com.soywiz.korag.shader.VertexShader
import com.soywiz.korag.shader.appending
import com.soywiz.korge.render.BatchBuilder2D
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture
import com.soywiz.korge.view.BlendMode
import com.soywiz.korge.view.View
import com.soywiz.korge.view.filter.ShaderFilter.Companion.fragmentCoords
import com.soywiz.korge.view.filter.ShaderFilter.Companion.fragmentCoords01
import com.soywiz.korge.view.filter.ShaderFilter.Companion.tex
import com.soywiz.korim.color.ColorAdd
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.MutableMarginInt

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

        val Program.ExpressionBuilder.fragmentCoords01 get() = DefaultShaders.v_Tex["xy"]
        val Program.ExpressionBuilder.fragmentCoords get() = fragmentCoords01 * u_TextureSize
        fun Program.ExpressionBuilder.tex(coords: Operand) = texture2D(DefaultShaders.u_Tex, coords / u_TextureSize)

        protected fun createProgram(vertex: VertexShader, fragment: FragmentShader, premultiplied: Boolean): Program {
            return Program(vertex, fragment.appending {
                // Premultiplied
                if (premultiplied) {
                    out["rgb"] setTo out["rgb"] / out["a"]
                }

                // Color multiply and addition
                // @TODO: Kotlin.JS BUG
                //out setTo (out * BatchBuilder2D.v_ColMul) + ((BatchBuilder2D.v_ColAdd - vec4(.5f, .5f, .5f, .5f)) * 2f)
                out setTo (out * BatchBuilder2D.v_ColMul) + ((BatchBuilder2D.v_ColAdd - vec4(.5f.lit, .5f.lit, .5f.lit, .5f.lit)) * 2f.lit)

                // Required for shape masks:
                if (premultiplied) {
                    IF(out["a"] le 0f.lit) { DISCARD() }
                }
            })
        }
    }

    var filtering = true

    private val textureSizeHolder = FloatArray(2)
    private val textureMaxTexCoords = FloatArray(2)
    private val textureStdTexDerivates = FloatArray(2)

    val scaledUniforms = AG.UniformValues()

    val uniforms = AG.UniformValues(
        //Filter.u_Time to timeHolder,
        u_TextureSize to textureSizeHolder,
        u_MaxTexCoords to textureMaxTexCoords,
        u_StdTexDerivates to textureStdTexDerivates,
    )

    override fun computeBorder(out: MutableMarginInt, texWidth: Int, texHeight: Int) {
        out.setTo(0)
    }

    /** The [VertexShader] used this this [Filter] */
    open val vertex: VertexShader = BatchBuilder2D.VERTEX

    /** The [FragmentShader] used this this [Filter]. This is usually overriden. */
    open val fragment: FragmentShader = Filter.DEFAULT_FRAGMENT

    private val programPremult: Program by lazy { createProgram(vertex, fragment, true) }
    private val programNormal: Program by lazy { createProgram(vertex, fragment, false) }

    //@CallSuper
    protected open fun updateUniforms(ctx: RenderContext, filterScale: Double) {
    }

    private fun _updateUniforms(ctx: RenderContext, filterScale: Double) {
        uniforms[u_filterScale] = filterScale
        scaledUniforms.fastForEach { uniform, value ->
            when (value) {
                is FloatArray -> {
                    if (uniform !in uniforms) {
                        uniforms[uniform] = value.copyOf()
                    }
                    val out = (uniforms[uniform] as FloatArray)
                    for (n in out.indices) out[n] = (value[n] * filterScale).toFloat()
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
                    blendFactors = blendMode.factors,
                    program = if (texture.premultiplied) programPremult else programNormal
                )
                //ctx.batch.flush()
            }
        }
    }
}
