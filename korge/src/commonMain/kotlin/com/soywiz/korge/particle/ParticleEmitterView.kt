package com.soywiz.korge.particle

import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.korag.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.render.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.ktree.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import kotlin.math.*

inline fun Container.particleEmitter(
	emitter: ParticleEmitter, emitterPos: IPoint = IPoint(),
    time: TimeSpan = TimeSpan.NIL,
	callback: ParticleEmitterView.() -> Unit = {}
) = ParticleEmitterView(emitter, emitterPos).apply { this.timeUntilStop = time }.addTo(this, callback)

suspend fun Container.attachParticleAndWait(
    particle: ParticleEmitter,
    x: Double,
    y: Double,
    time: TimeSpan = TimeSpan.NIL,
    speed: Double = 1.0
) {
    val p = particle.create(x, y, time)
    p.speed = speed
    this += p
    p.waitComplete()
    this -= p
}

class ParticleEmitterView(emitter: ParticleEmitter, emitterPos: IPoint = IPoint()) : View(), ViewFileRef by ViewFileRef.Mixin() {
    var emitter: ParticleEmitter = emitter
	var simulator = ParticleEmitterSimulator(emitter, emitterPos)

	var timeUntilStop by simulator::timeUntilStop.redirected()
	val emitterPos by simulator::emitterPos.redirected()
	var emitting by simulator::emitting.redirected()
	val aliveCount by simulator::aliveCount.redirected()
	val anyAlive by simulator::anyAlive.redirected()

	init {
		addUpdater { dt ->
			simulator.simulate(dt)
		}
	}

    fun restart() {
        simulator.restart()
    }

	suspend fun waitComplete() {
		while (anyAlive) delayFrame()
	}

    private var cachedBlending = AG.Blending.NORMAL

	// @TODO: Make ultra-fast rendering flushing ctx and using a custom shader + vertices + indices
	override fun renderInternal(ctx: RenderContext) {
		if (!visible) return
        if (!textureLoaded && texture != null) {
            textureLoaded = true
            launchImmediately(ctx.coroutineContext) {
                forceLoadTexture(ctx.views!!, sourceFile = texture)
            }
        }
        lazyLoadRenderInternal(ctx, this)
		//ctx.flush()

        if (cachedBlending.srcRGB != emitter.blendFuncSource || cachedBlending.dstRGB != emitter.blendFuncDestination) {
            cachedBlending = AG.Blending(emitter.blendFuncSource, emitter.blendFuncDestination)
        }

		val context = ctx.ctx2d
		val texture = emitter.texture ?: return
		val cx = texture.width * 0.5
		val cy = texture.height * 0.5
		context.keep {
			context.blendFactors = cachedBlending
			context.setMatrix(globalMatrix)

			simulator.particles.fastForEach { p ->
                if (p.alive) {
                    val scale = p.scale
                    context.multiplyColor = p.color * this@ParticleEmitterView.colorMul
                    context.imageScale(ctx.getTex(texture), p.x - cx * scale, p.y - cy * scale, scale)
                }
			}
		}
	}

    override fun getLocalBoundsInternal(out: Rectangle) {
        out.setBounds(-30, -30, +30, +30)
    }

    private var textureLoaded: Boolean = false
    var texture: String?
        get() = emitter.textureName
        set(value) {
            textureLoaded = false
            emitter.textureName = value
        }

    suspend fun forceLoadTexture(views: Views, currentVfs: VfsFile = views.currentVfs, sourceFile: String? = null) {
        //println("### Trying to load sourceImage=$sourceImage")
        this.texture = sourceFile
        textureLoaded = true
        try {
            emitter.texture = currentVfs["$sourceFile"].readBitmapSlice()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override suspend fun forceLoadSourceFile(views: Views, currentVfs: VfsFile, sourceFile: String?) {
        //println("### Trying to load sourceImage=$sourceImage")
        baseForceLoadSourceFile(views, currentVfs, sourceFile)
        emitter = currentVfs["$sourceFile"].readParticleEmitter()
        simulator = ParticleEmitterSimulator(emitter, emitterPos)
        scale = 1.0
    }

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        if (views.name == "ktree") {
            container.uiCollapsableSection("Particle Emitter Reference") {
                uiEditableValue(::sourceFile, UiTextEditableValue.Kind.FILE(views.currentVfs) {
                    it.extensionLC == "pex"
                })
            }
            return
        }
        val particle = this@ParticleEmitterView.emitter
        container.uiCollapsableSection("Particle Emitter") {
            uiEditableValue(::texture, UiTextEditableValue.Kind.FILE(views.currentVfs) {
                it.extensionLC == "png" || it.extensionLC == "jpg"
            })
            uiEditableValue(particle::emitterType, values = { ParticleEmitter.Type.values().toList() })
            uiEditableValue(particle::blendFuncSource, values = { AG.BlendFactor.values().toList() })
            uiEditableValue(particle::blendFuncDestination, values = { AG.BlendFactor.values().toList() })
            uiCollapsableSection("Angle") {
                uiEditableValue(listOf(particle::angle, particle::angleVariance))
            }
            uiCollapsableSection("Speed") {
                uiEditableValue(listOf(particle::speed, particle::speedVariance), 0.0, 1000.0)
            }
            uiCollapsableSection("Lifespan") {
                uiEditableValue(listOf(particle::lifeSpan, particle::lifespanVariance), -10.0, 10.0)
                uiEditableValue(particle::duration, -10.0, 10.0)
            }
            uiEditableValue("Gravity", particle.gravity)
            uiEditableValue("Source Position", particle.sourcePosition)
            uiEditableValue("Source Position Variance", particle.sourcePositionVariance)
            uiCollapsableSection("Acceleration") {
                uiEditableValue(listOf(particle::radialAcceleration, particle::radialAccelVariance), -1000.0, +1000.0)
                uiEditableValue(listOf(particle::tangentialAcceleration, particle::tangentialAccelVariance), -1000.0, +1000.0)
            }
            uiEditableValue("Start Color", particle.startColor)
            uiEditableValue("Start Color Variance", particle.startColorVariance)
            uiEditableValue("End Color", particle.endColor)
            uiEditableValue("End Color Variance", particle.endColor)
            uiEditableValue(particle::maxParticles)
            uiEditableValue(listOf(particle::startSize, particle::startSizeVariance), -1000.0, +1000.0)
            uiEditableValue(listOf(particle::endSize, particle::endSizeVariance), -1000.0, 1000.0)

            uiCollapsableSection("Radius") {
                uiEditableValue(listOf(particle::minRadius, particle::minRadiusVariance), min = -1000.0, max = 1000.0)
                uiEditableValue(listOf(particle::maxRadius, particle::maxRadiusVariance), min = -1000.0, max = 1000.0)
            }
            uiCollapsableSection("Rotate") {
                uiEditableValue(listOf(particle::rotatePerSecond, particle::rotatePerSecondVariance))
                uiEditableValue(listOf(particle::rotationStart, particle::rotationStartVariance))
                uiEditableValue(listOf(particle::rotationEnd, particle::rotationEndVariance))
            }
        }
        super.buildDebugComponent(views, container)
    }
}
