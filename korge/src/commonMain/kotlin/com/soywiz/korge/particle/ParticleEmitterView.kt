package com.soywiz.korge.particle

import com.soywiz.klock.*
import com.soywiz.korag.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.render.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.fast.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import kotlin.random.*

inline fun Container.particleEmitter(
    emitter: ParticleEmitter,
    emitterPos: IPoint = IPoint(),
    time: TimeSpan = TimeSpan.NIL,
    localCoords: Boolean = false,
    random: Random = Random,
    callback: ParticleEmitterView.() -> Unit = {}
) = ParticleEmitterView(emitter, emitterPos, localCoords, random).apply { this.timeUntilStop = time }.addTo(this, callback)

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

class ParticleEmitterView(
    emitter: ParticleEmitter,
    emitterPos: IPoint = IPoint(),
    localCoords: Boolean = false,
    random: Random = Random,
) : View(), ViewFileRef by ViewFileRef.Mixin() {
    private var emitter = emitter
	var simulator = ParticleEmitterSimulator(emitter, Point(emitterPos), random)

	var timeUntilStop by simulator::timeUntilStop
	var emitting by simulator::emitting
	val aliveCount by simulator::aliveCount
	val anyAlive by simulator::anyAlive
    var emitterPos: Point
        get() = simulator.emitterPos
        set(value) {
            simulator.emitterPos.copyFrom(value)
        }
    var emitterXY: Point
        get() = emitterPos
        set(value) { emitterPos = value }
    var emitterX: Double
        get() = emitterPos.x
        set(value) { emitterPos.x = value }
    var emitterY: Double
        get() = emitterPos.y
        set(value) { emitterPos.y = value }

    var localCoords: Boolean = localCoords

    //override fun setXY(x: Double, y: Double) {
    //    if (localCoords) {
    //        super.setXY(x, y)
    //    } else {
    //        super.setXY(0.0, 0.0)
    //        emitterX = x
    //        emitterY = y
    //    }
    //}

    init {
		addUpdater { dt ->
            if (!this.localCoords) {
                simulator.emitterPos.setTo(globalX, globalY)
            }
            if (dt > 0.milliseconds) {
                simulator.simulate(dt)
            }
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
        run {
            if (!textureLoaded && texture != null) {
                textureLoaded = true
                launchImmediately(ctx.coroutineContext) {
                    forceLoadTexture(ctx.views!!, sourceFile = texture)
                }
            }
            lazyLoadRenderInternal(ctx, this)
        }

        if (cachedBlending.srcRGB != emitter.blendFuncSource || cachedBlending.dstRGB != emitter.blendFuncDestination) {
            cachedBlending = AG.Blending(emitter.blendFuncSource, emitter.blendFuncDestination)
        }

        if (fsprites == null || fsprites!!.maxSize < simulator.particles.max) {
            fsprites = FSprites(simulator.particles.max)
        }
        val sprites = fsprites!!
        val tex = emitter.texture ?: Bitmaps.white
        fviewInfo.texs[0] = tex.bmp
        sprites.size = 0
        val particles = simulator.particles
        //println("particles.max=${particles.max}")
        simulator.particles.fastForEach { p ->
            if (p.alive) {
                //if (p.x == 0f && p.y == 0f) println("00: ${p.index}")
                sprites.apply {
                    val fsprite = FSpriteFromIndex(sprites.size++)
                    //println("FSPRITE: ${fsprite.index}")
                    fsprite.setTex(tex)
                    fsprite.colorMul = p.color * this@ParticleEmitterView.colorMul
                    fsprite.setAnchor(.5f, .5f)
                    fsprite.xy(p.x, p.y)
                    fsprite.scale(p.scale)
                    fsprite.angle = p.rotation
                }
            }
        }

        FSprites.render(
            ctx = ctx,
            sprites = sprites,
            info = fviewInfo,
            smoothing = true,
            globalMatrix = if (localCoords) globalMatrix else ctx.views!!.stage.localMatrix,
            blending = cachedBlending
        )
    }
    private var fsprites: FSprites? = null
    private val fviewInfo = FSprites.FViewInfo(1)

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
            container.uiCollapsibleSection("Particle Emitter Reference") {
                uiEditableValue(::sourceFile, UiTextEditableValue.Kind.FILE(views.currentVfs) {
                    it.extensionLC == "pex"
                })
            }
            return
        }
        val particle = this@ParticleEmitterView.emitter
        container.uiCollapsibleSection("Particle Emitter") {
            uiEditableValue(::texture, UiTextEditableValue.Kind.FILE(views.currentVfs) {
                it.extensionLC == "png" || it.extensionLC == "jpg"
            })
            uiEditableValue(this@ParticleEmitterView::localCoords)
            uiEditableValue(Pair(this@ParticleEmitterView::emitterX, this@ParticleEmitterView::emitterY), min = -1000.0, max = +1000.0, clamp = false, name = "emitterPos")
            uiEditableValue(particle::emitterType, values = { ParticleEmitter.Type.values().toList() })
            uiEditableValue(particle::blendFuncSource, values = { AG.BlendFactor.values().toList() })
            uiEditableValue(particle::blendFuncDestination, values = { AG.BlendFactor.values().toList() })
            uiCollapsibleSection("Angle") {
                uiEditableValue(listOf(particle::angle, particle::angleVariance))
            }
            uiCollapsibleSection("Speed") {
                uiEditableValue(listOf(particle::speed, particle::speedVariance), 0.0, 1000.0)
            }
            uiCollapsibleSection("Lifespan") {
                uiEditableValue(listOf(particle::lifeSpan, particle::lifespanVariance), -10.0, 10.0)
                uiEditableValue(particle::duration, -10.0, 10.0)
            }
            uiEditableValue("Gravity", particle.gravity)
            uiEditableValue("Source Position", particle.sourcePosition)
            uiEditableValue("Source Position Variance", particle.sourcePositionVariance)
            uiCollapsibleSection("Acceleration") {
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

            uiCollapsibleSection("Radius") {
                uiEditableValue(listOf(particle::minRadius, particle::minRadiusVariance), min = -1000.0, max = 1000.0)
                uiEditableValue(listOf(particle::maxRadius, particle::maxRadiusVariance), min = -1000.0, max = 1000.0)
            }
            uiCollapsibleSection("Rotate") {
                uiEditableValue(listOf(particle::rotatePerSecond, particle::rotatePerSecondVariance))
                uiEditableValue(listOf(particle::rotationStart, particle::rotationStartVariance))
                uiEditableValue(listOf(particle::rotationEnd, particle::rotationEndVariance))
            }
        }
        super.buildDebugComponent(views, container)
    }
}
