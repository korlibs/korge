package korlibs.korge.view.filter

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.image.color.*
import korlibs.korge.render.*
import korlibs.korge.view.*
import korlibs.korge.view.filter.ShaderFilter.Companion.fragmentCoords
import korlibs.korge.view.filter.ShaderFilter.Companion.fragmentCoords01
import korlibs.korge.view.filter.ShaderFilter.Companion.tex
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.memory.*

/**
 * Abstract class for [View] [Filter]s that paints the [Texture] using a [FragmentShader] ([fragment]).
 *
 * Inherited versions of this filter, usually inherit [computeBorder] and [fragment] properties.
 *
 * When building shaders by calling FragmentShader { ... }, you will have additionally access to:
 * [fragmentCoords], [fragmentCoords01] properties and [tex] method to be used inside the shader.
 */
abstract class ShaderFilter : FilterWithFiltering {
    object TexInfoUB : UniformBlock(fixedLocation = 4) {
        val u_TextureSize by vec2()
        val u_MaxTexCoords by vec2()
        val u_StdTexDerivates by vec2()
        val u_filterScale by float()
    }

    companion object {
        val DEFAULT_FRAGMENT = BatchBuilder2D.PROGRAM.fragment

        val Program.ExpressionBuilder.fragmentCoords01 get() = DefaultShaders.v_Tex["xy"]
        val Program.ExpressionBuilder.fragmentCoords get() = fragmentCoords01 * TexInfoUB.u_TextureSize
        fun Program.ExpressionBuilder.tex(coords: Operand): Operand = texture2D(DefaultShaders.u_Tex, coords / TexInfoUB.u_TextureSize)

        val Program.ExpressionBuilder.v_Tex01: Operand get() = (DefaultShaders.v_Tex["xy"] / TexInfoUB.u_MaxTexCoords)

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
        protected open val vertex: VertexShader = BatchBuilder2D.PROGRAM.vertex

        /** The [FragmentShader] used this [Filter]. This is usually overriden. */
        protected open val fragment: FragmentShader = ShaderFilter.DEFAULT_FRAGMENT

        private val _program: Program by lazy { createProgram(vertex, fragment) }

        override fun getProgram(): Program = _program
    }

    override var filtering = true

    private var textureSizeHolder = Point()
    private var textureMaxTexCoords = Point()
    private var textureStdTexDerivates = Point()

    override fun computeBorder(texWidth: Int, texHeight: Int): MarginInt {
        return MarginInt.ZERO
    }

    ///** The [VertexShader] used this this [Filter] */
    //open val vertex: VertexShader = BatchBuilder2D.VERTEX
    ///** The [FragmentShader] used this [Filter]. This is usually overriden. */
    //open val fragment: FragmentShader = Filter.DEFAULT_FRAGMENT
    //private val programPremult: Program by lazy { createProgram(vertex, fragment, true) }
    //private val programNormal: Program by lazy { createProgram(vertex, fragment, false) }

    open val programProvider: ProgramProvider = BaseProgramProvider

    private val oldTextureUnits = AGTextureUnits()
    private var resetTex: Int = 0

    protected fun setTex(ctx: RenderContext, sampler: Sampler, texture: AGTexture?, info: AGTextureUnitInfo = AGTextureUnitInfo.DEFAULT) {
        oldTextureUnits.copyFrom(ctx.textureUnits, sampler)
        ctx.textureUnits.set(sampler, texture, info)
        resetTex = resetTex or (1 shl sampler.index)
    }

    //@CallSuper
    protected open fun updateUniforms(ctx: RenderContext, filterScale: Double) {
    }

    private fun _restoreUniforms(ctx: RenderContext, filterScale: Double) {
        resetTex.fastForEachOneBits {
            ctx.textureUnits.copyFrom(oldTextureUnits, it)
            oldTextureUnits.set(it, null)
        }
        resetTex = 0
    }

    private fun _updateUniforms(
        ctx: RenderContext, filterScale: Double, texture: Texture,
        texWidth: Int,
        texHeight: Int,
    ) {
        textureSizeHolder = Point(texture.base.width.toFloat(), texture.base.height.toFloat())
        textureStdTexDerivates = Point(1f / texture.base.width.toFloat(), 1f / texture.base.height.toFloat())
        textureMaxTexCoords = Point(texWidth.toFloat() / texture.base.width.toFloat(), texHeight.toFloat() / texture.base.height.toFloat())

        ctx[TexInfoUB].push {
            it[u_filterScale] = filterScale
            it[u_TextureSize] = textureSizeHolder
            it[u_MaxTexCoords] = textureMaxTexCoords
            it[u_StdTexDerivates] = textureStdTexDerivates
        }

        //scaledUniforms.fastForEach { value ->
        //    when (value.type.kind) {
        //        VarKind.TFLOAT -> {
        //            val out = uniforms[value.uniform].f32
        //            uniforms[value.uniform].setFloatArray(out.size) {
        //                (value.f32[it] * filterScale).toFloat()
        //            }
        //        }
        //        else -> TODO()
        //    }
        //}
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
        renderColorMul: RGBA,
        blendMode: BlendMode,
        filterScale: Double,
    ) {
        if (isIdentity) return IdentityFilter.render(
            ctx,
            matrix,
            texture,
            texWidth,
            texHeight,
            renderColorMul,
            blendMode,
            filterScale
        )

        val _margin = getBorder(texWidth, texHeight)
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

        ctx.useBatcher { batch ->
            //println("renderColorMulInt=" + RGBA(renderColorMulInt))
            //println("blendMode:$blendMode")

            val slice = texture.sliceWithBounds(-marginLeft, -marginTop, texture.width + marginRight, texture.height + marginBottom, clamped = false)

            //println("matrix=$matrix, slice=$slice, marginLeft=$marginLeft")
            //ctx.keepTextureUnit(DefaultShaders.u_Tex, flush = true) {
            batch.temporalTextureUnit(DefaultShaders.u_Tex, slice.base.base) {
                _updateUniforms(ctx, filterScale, texture, texWidth, texHeight)
                batch.drawQuad(
                    slice,
                    x = -marginLeft.toFloat(),
                    y = -marginTop.toFloat(),
                    m = matrix,
                    filtering = filtering,
                    colorMul = renderColorMul,
                    blendMode = blendMode,
                    //program = if (texture.premultiplied) programPremult else programNormal
                    program = programProvider.getProgram(),
                )
            }
            _restoreUniforms(ctx, filterScale)
        }

    }
}
