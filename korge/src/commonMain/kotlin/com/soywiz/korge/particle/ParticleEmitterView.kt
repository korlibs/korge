package com.soywiz.korge.particle

import com.soywiz.kds.iterators.*
import com.soywiz.korge.render.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*

inline fun Container.particleEmitter(
	emitter: ParticleEmitter, emitterPos: IPoint = IPoint(),
	callback: ParticleEmitterView.() -> Unit = {}
) = ParticleEmitterView(emitter, emitterPos).addTo(this, callback)

class ParticleEmitterView(val emitter: ParticleEmitter, emitterPos: IPoint = IPoint()) : View() {
	val simulator = ParticleEmitter.Simulator(emitter, emitterPos)

	var timeUntilStop by simulator::timeUntilStop.redirected()
	val emitterPos by simulator::emitterPos.redirected()
	var emitting by simulator::emitting.redirected()
	val aliveCount by simulator::aliveCount.redirected()
	val anyAlive by simulator::anyAlive.redirected()

	init {
		addUpdatable { dtMs ->
			simulator.simulate(dtMs.toDouble() / 1000.0)
		}
	}

	suspend fun waitComplete() {
		while (anyAlive) waitFrame()
	}

	// @TODO: Make ultra-fast rendering flushing ctx and using a custom shader + vertices + indices
	override fun renderInternal(ctx: RenderContext) {
		if (!visible) return
		//ctx.flush()

		val context = ctx.ctx2d
		val texture = emitter.texture ?: return
		val cx = texture.width * 0.5
		val cy = texture.height * 0.5
		context.keep {
			context.blendFactors = emitter.blendFactors
			context.setMatrix(globalMatrix)

			simulator.particles.fastForEach { p ->
				val scale = p.scale
				context.multiplyColor = p.color
				context.imageScale(ctx.getTex(texture), p.x - cx * scale, p.y - cy * scale, scale)
			}
		}
	}
}
