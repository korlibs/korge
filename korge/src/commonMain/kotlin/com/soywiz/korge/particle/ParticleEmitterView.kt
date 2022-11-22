package com.soywiz.korge.particle

import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.korag.*
import com.soywiz.korge.render.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.fast.*
import com.soywiz.korge.view.property.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.Rectangle
import kotlinx.coroutines.*
import kotlin.random.*

inline fun Container.particleEmitter(
    emitter: ParticleEmitter,
    emitterPos: IPoint = IPoint(),
    time: TimeSpan = TimeSpan.NIL,
    localCoords: Boolean = false,
    random: Random = Random,
    callback: ParticleEmitterView.() -> Unit = {}
) = ParticleEmitterView(emitter, emitterPos, localCoords, random).apply { this.timeUntilStop = time }
    .addTo(this, callback)

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
    @ViewProperty
    @ViewPropertySubTree
    private var emitter: ParticleEmitter,
    emitterPos: IPoint = IPoint(),
    localCoords: Boolean = false,
    random: Random = Random,
) : View(), ViewFileRef by ViewFileRef.Mixin() {
    var simulator = ParticleEmitterSimulator(emitter, Point(emitterPos), random)

    var timeUntilStop by simulator::timeUntilStop
    var emitting by simulator::emitting
    val aliveCount by simulator::aliveCount
    val anyAlive by simulator::anyAlive

    @ViewProperty(
        min = -1000.0,
        max = +1000.0,
    )
    var emitterPos: Point
        get() = simulator.emitterPos
        set(value) {
            simulator.emitterPos.copyFrom(value)
        }
    var emitterXY: Point
        get() = emitterPos
        set(value) {
            emitterPos = value
        }
    var emitterX: Double
        get() = emitterPos.x
        set(value) {
            emitterPos.x = value
        }
    var emitterY: Double
        get() = emitterPos.y
        set(value) {
            emitterPos.y = value
        }

    @ViewProperty
    var localCoords: Boolean = localCoords

    private val lastPosition = Point(globalX, globalY)

    //override fun setXY(x: Double, y: Double) {
    //    if (localCoords) {
    //        super.setXY(x, y)
    //    } else {
    //        super.setXY(0.0, 0.0)
    //        emitterX = x
    //        emitterY = y
    //    }
    //}

    var autoInvalidateRenderer: Boolean = true

    init {
        addUpdater { step(it) }
    }

    fun step(dt: TimeSpan) {
        //            if (!this.localCoords) {
//                simulator.emitterPos.setTo(x, y)
//            }
        if (dt > 0.milliseconds) {
            val gx = globalX / stage!!.scaleX
            val gy = globalY / stage!!.scaleY

            val dx = if (this.localCoords) 0.0 else lastPosition.x - gx
            val dy = if (this.localCoords) 0.0 else lastPosition.y - gy
//                val dx = 0.0
//                val dy = 0.0

            simulator.simulate(dt, dx, dy)

            lastPosition.setTo(gx, gy)
            if (autoInvalidateRenderer) invalidateRender()
        }
    }

    fun restart() {
        simulator.restart()
    }

    suspend fun waitComplete() {
        while (anyAlive) delayFrame()
    }

    private var cachedBlending = BlendMode.NORMAL

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

        if (cachedBlending.factors.srcRGB != emitter.blendFuncSource || cachedBlending.factors.dstRGB != emitter.blendFuncDestination) {
            cachedBlending = BlendMode(AG.Blending(emitter.blendFuncSource, emitter.blendFuncDestination))
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
//            globalMatrix = if (localCoords) globalMatrix else ctx.views!!.stage.localMatrix,
            globalMatrix = globalMatrix,
            blending = cachedBlending
        )
    }

    private var fsprites: FSprites? = null
    private val fviewInfo = FSprites.FViewInfo(1)

    override fun getLocalBoundsInternal(out: Rectangle) {
        out.setBounds(-30, -30, +30, +30)
    }

    private var textureLoaded: Boolean = false
    @ViewProperty
    @ViewPropertyFileRef(["png", "jpg"])
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
            if (e is CancellationException) throw e
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

    @Suppress("unused")
    @ViewProperty
    @ViewPropertyFileRef(["pex"])
    private var pexSourceFile: String? by this::sourceFile
}
