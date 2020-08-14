package com.soywiz.korge.view.effect

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.*

@KorgeDeprecated
@Deprecated("Use View.filter instead")
open class EffectView : Container() {
	var filtering = true
	private val oldViewMatrix = Matrix3D()
	open var borderEffect = 0
	private val tempMat2d = Matrix()
	var vertex: VertexShader = BatchBuilder2D.VERTEX
		set(value) {
			field = value
			program = null
		}
	var fragment: FragmentShader = DEFAULT_FRAGMENT
		set(value) {
			field = value
			program = null
		}

	var program: Program? = null
	private val timeHolder = FloatArray(1)
	private val textureSizeHolder = FloatArray(2)
	val uniforms = AG.UniformValues(
		u_Time to timeHolder,
		u_TextureSize to textureSizeHolder
	)

	companion object {
		val u_Time = Uniform("time", VarType.Float1)
		val u_TextureSize = Uniform("effectTextureSize", VarType.Float2)
	    val DEFAULT_FRAGMENT = BatchBuilder2D.buildTextureLookupFragment(premultiplied = false)

		val Program.Builder.fragmentCoords01 get() = DefaultShaders.v_Tex["xy"]
		val Program.Builder.fragmentCoords get() = fragmentCoords01 * u_TextureSize
		fun Program.Builder.tex(coords: Operand) = texture2D(DefaultShaders.u_Tex, coords / u_TextureSize)
	}

	private var currentTimeMs = 0
		set(value) {
			field = value
			timeHolder[0] = (currentTimeMs.toDouble() / 1000.0).toFloat()
		}

	init {
		addUpdatable { ms ->
			currentTimeMs += ms
		}
	}

	override fun renderInternal(ctx: RenderContext) {
		val bounds = getLocalBounds()

		//println("$this: [0] $bounds")

		val texWidth = bounds.width.toInt() + borderEffect * 2
		val texHeight = bounds.height.toInt() + borderEffect * 2

		ctx.renderToTexture(texWidth, texHeight, render = {
			tempMat2d.copyFrom(this.globalMatrixInv)
			tempMat2d.translate(-bounds.x + borderEffect, -bounds.y + borderEffect)
			//println("$this: [1] $tempMat2d")
			ctx.batch.setViewMatrixTemp(tempMat2d) {
				super.renderInternal(ctx)
			}
		}) { texture ->
			// @TODO: Precompute vertices
			textureSizeHolder[0] = texture.base.width.toFloat()
			textureSizeHolder[1] = texture.base.height.toFloat()
			updateUniforms()

			//println(textureSizeHolder.toList())
			tempMat2d.copyFrom(this.globalMatrix)
			tempMat2d.pretranslate(-borderEffect + bounds.x, -borderEffect + bounds.y)
			if (program == null) program = Program(vertex, fragment)
			//println("EffectUniforms: ${this.uniforms}")
			//println("$this: [2] $tempMat2d")
			renderFilter(ctx, tempMat2d, texture, texWidth, texHeight)
		}
	}

	open fun renderFilter(ctx: RenderContext, matrix: Matrix, texture: Texture, texWidth: Int, texHeight: Int) {
		ctx.batch.setTemporalUniforms(this.uniforms) {
			ctx.batch.drawQuad(
				texture,
				m = matrix,
				filtering = filtering,
				colorAdd = renderColorAdd,
				colorMul = renderColorMul,
				blendFactors = blendMode.factors,
				program = program
			)
		}
	}

	override fun getLocalBoundsInternal(out: Rectangle) {
		super.getLocalBoundsInternal(out)
		out.x -= borderEffect
		out.y -= borderEffect
		out.width += borderEffect * 2
		out.height += borderEffect * 2
	}

	protected open fun updateUniforms() {
	}
}

@KorgeDeprecated
@Deprecated("Use View.filter instead")
inline fun Container.effectView(callback: EffectView.() -> Unit = {}) =
	EffectView().addTo(this, callback)
