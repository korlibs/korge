package com.soywiz.korge.ext.particle

import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.time.waitFrame
import com.soywiz.korge.view.View
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.RGBAf
import com.soywiz.korio.util.redirect
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.Point2d

class ParticleEmitterView(val emitter: ParticleEmitter, emitterPos: Point2d = Point2d()) : View(emitter.views) {
	val simulator = ParticleEmitter.Simulator(emitter, emitterPos)

	var timeUntilStop by simulator::timeUntilStop.redirect()
	val emitterPos by simulator::emitterPos.redirect()
	var emitting by simulator::emitting.redirect()
	val aliveCount by simulator::aliveCount.redirect()
	val anyAlive by simulator::anyAlive.redirect()

	override fun updateInternal(dtMs: Int) {
		simulator.simulate(dtMs.toDouble() / 1000.0)
	}

	suspend fun waitComplete() {
		while (anyAlive) waitFrame()
	}

	// @TODO: Make ultra-fast rendering flushing ctx and using a custom shader + vertices + indices
	override fun render(ctx: RenderContext, m: Matrix2d) {
		if (!visible) return
		//ctx.flush()

		val context = ctx.ctx2d
		val texture = emitter.texture ?: return
		val cx = texture.width * 0.5
		val cy = texture.height * 0.5
		context.keep {
			context.blendFactors = emitter.blendFactors
			context.setMatrix(m)

			for (p in simulator.particles) {
				val scale = p.scale
				context.multiplyColor = p.colorInt
				context.imageScale(texture, p.x - cx * scale, p.y - cy * scale, scale)
			}
		}
	}
}
