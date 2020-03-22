package com.soywiz.korge.view.filter

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
 * Inherited versions of this filter, usually inherit [border] and [fragment] properties.
 *
 * When building shaders by calling FragmentShader { ... }, you will have additionally access to:
 * [fragmentCoords], [fragmentCoords01] properties and [tex] method to be used inside the shader.
 */
abstract class ShaderFilter : Filter {
    companion object {
        //val u_Time = Uniform("time", VarType.Float1)
        val u_TextureSize = Uniform("effectTextureSize", VarType.Float2)
        val DEFAULT_FRAGMENT = BatchBuilder2D.buildTextureLookupFragment(premultiplied = false)

        val Program.Builder.fragmentCoords01 get() = DefaultShaders.v_Tex["xy"]
        val Program.Builder.fragmentCoords get() = fragmentCoords01 * u_TextureSize
        fun Program.Builder.tex(coords: Operand) = texture2D(DefaultShaders.u_Tex, coords / u_TextureSize)
    }

    var filtering = true

    private val textureSizeHolder = FloatArray(2)

    val uniforms = AG.UniformValues(
        //Filter.u_Time to timeHolder,
        Filter.u_TextureSize to textureSizeHolder
    )

    override val border: Int = 0

    /** The [VertexShader] used this this [Filter] */
    open val vertex: VertexShader = BatchBuilder2D.VERTEX

    /** The [FragmentShader] used this this [Filter]. This is usually overriden. */
    open val fragment: FragmentShader = Filter.DEFAULT_FRAGMENT

    protected fun createProgram(premultiplied: Boolean): Program {
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

    private val programPremult: Program by lazy { createProgram(true) }
    private val programNormal: Program by lazy { createProgram(false) }

    protected open fun updateUniforms() {
    }

    override fun render(
        ctx: RenderContext,
        matrix: Matrix,
        texture: Texture,
        texWidth: Int,
        texHeight: Int,
        renderColorAdd: Int,
        renderColorMul: RGBA,
        blendMode: BlendMode
    ) {
        //println("$this.render()")
        // @TODO: Precompute vertices
        textureSizeHolder[0] = texture.base.width.toFloat()
        textureSizeHolder[1] = texture.base.height.toFloat()
        updateUniforms()

        ctx.batch.setTemporalUniforms(this.uniforms) {
            //println("renderColorMulInt=" + RGBA(renderColorMulInt))
            //println("blendMode:$blendMode")
            ctx.batch.drawQuad(
                texture,
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
