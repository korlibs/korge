package com.soywiz.korge.view.effect

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korma.*

class BlurEffectView(initialRadius: Double = 10.0) : EffectView() {
	companion object {
		private val u_Weights = Uniform("weights", VarType.Mat3)

		val KERNEL_GAUSSIAN_BLUR: Matrix3
			get() = Matrix3(
				1f, 2f, 1f,
				2f, 4f, 2f,
				1f, 2f, 1f
			) * (1f / 16f)
	}

	val weights by uniforms.storageForMatrix3(u_Weights, KERNEL_GAUSSIAN_BLUR)

	var radius = initialRadius
		set(value) {
			field = value
			borderEffect = value.toInt()
		}

	init {
		radius = initialRadius

		fragment = FragmentShader {
			DefaultShaders {
				out setTo vec4(0f.lit, 0f.lit, 0f.lit, 0f.lit)

				for (y in 0 until 3) {
					for (x in 0 until 3) {
						out setTo out + (tex(
							fragmentCoords + vec2(
								(x - 1).toFloat().lit,
								(y - 1).toFloat().lit
							)
						)) * u_Weights[x][y]
					}
				}
			}
		}
	}

	private val identity = Matrix2d()

	fun renderFilterLevel(ctx: RenderContext, matrix: Matrix2d, texture: Texture, texWidth: Int, texHeight: Int, level: Int) {
		// @TODO: We only need two render textures
		ctx.renderToTexture(texWidth, texHeight, {
			ctx.batch.setTemporalUniforms(this.uniforms) {
				ctx.batch.drawQuad(
					texture,
					m = identity,
					filtering = filtering,
					colorAdd = renderColorAdd,
					colorMulInt = renderColorMulInt,
					blendFactors = blendMode.factors,
					program = program
				)
			}
		}, { newtex ->
			if (level > 0) {
				renderFilterLevel(ctx, matrix, newtex, texWidth, texHeight, level - 1)
			} else {
				super.renderFilter(ctx, matrix, newtex, texWidth, texHeight)
			}
		})
	}

	override fun renderFilter(ctx: RenderContext, matrix: Matrix2d, texture: Texture, texWidth: Int, texHeight: Int) {
		renderFilterLevel(ctx, matrix, texture, texWidth, texHeight, level = borderEffect)
	}
}

inline fun Container.blurEffectView(
	radius: Double = 10.0,
	callback: @ViewsDslMarker BlurEffectView.() -> Unit = {}
) =
	BlurEffectView(radius).addTo(this).apply(callback)
