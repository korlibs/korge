package com.soywiz.korge.particle

import com.soywiz.kds.iterators.*
import com.soywiz.korma.geom.*
import kotlin.math.*
import kotlin.random.*

class ParticleEmitterSimulator(
    private val emitter: ParticleEmitter,
    var emitterPos: IPoint = IPoint(),
    val seed: Long = Random.nextLong()
) {
    val random = Random(seed)
    var totalElapsedTime = 0
    var timeUntilStop = Int.MAX_VALUE
    var emitting = true
    val textureWidth = emitter.texture?.width ?: 16
    val particles = List(emitter.maxParticles) { init(Particle(it), true) }
    val aliveCount: Int get() = particles.count { it.alive }
    val anyAlive: Boolean get() = aliveCount > 0

    private fun randomVariance(base: Double, variance: Double): Double =
        base + variance * (random.nextDouble() * 2.0 - 1.0)

    fun init(particle: Particle, initialization: Boolean): Particle {
        val lifespan = randomVariance(emitter.lifeSpan, emitter.lifespanVariance).coerceAtLeast(0.001)

        particle.totalTime = lifespan.coerceAtLeast(0.0)

        if (initialization) {
            val ratio = particle.index.toDouble() / emitter.maxParticles.toDouble()
            particle.currentTime = -(emitter.lifeSpan + emitter.lifespanVariance.absoluteValue) * ratio
            //println(particle.currentTime)
        } else {
            particle.currentTime = 0.0
        }

        val emitterX = emitterPos.x
        val emitterY = emitterPos.y

        particle.x = randomVariance(emitterX, emitter.sourcePositionVariance.x)
        particle.y = randomVariance(emitterY, emitter.sourcePositionVariance.y)
        particle.startX = emitterX
        particle.startY = emitterY

        val angle = randomVariance(emitter.angle, emitter.angleVariance)
        val speed = randomVariance(emitter.speed, emitter.speedVariance)
        particle.velocityX = speed * cos(angle)
        particle.velocityY = speed * sin(angle)

        val startRadius = randomVariance(emitter.maxRadius, emitter.maxRadiusVariance)
        val endRadius = randomVariance(emitter.minRadius, emitter.minRadiusVariance)
        particle.emitRadius = startRadius
        particle.emitRadiusDelta = (endRadius - startRadius) / lifespan
        particle.emitRotation = randomVariance(emitter.angle, emitter.angleVariance)
        particle.emitRotationDelta = randomVariance(emitter.rotatePerSecond, emitter.rotatePerSecondVariance)
        particle.radialAcceleration = randomVariance(emitter.radialAcceleration, emitter.radialAccelVariance)
        particle.tangentialAcceleration =
            randomVariance(emitter.tangentialAcceleration, emitter.tangentialAccelVariance)

        val startSize = randomVariance(emitter.startSize, emitter.startSizeVariance).coerceAtLeast(0.1)
        val endSize = randomVariance(emitter.endSize, emitter.endSizeVariance).coerceAtLeast(0.1)
        particle.scale = startSize / textureWidth
        particle.scaleDelta = ((endSize - startSize) / lifespan) / textureWidth

        particle.colorR = randomVariance(emitter.startColor.rd, emitter.startColorVariance.rd)
        particle.colorG = randomVariance(emitter.startColor.gd, emitter.startColorVariance.gd)
        particle.colorB = randomVariance(emitter.startColor.bd, emitter.startColorVariance.bd)
        particle.colorA = randomVariance(emitter.startColor.ad, emitter.startColorVariance.ad)

        val endColorR = randomVariance(emitter.endColor.rd, emitter.endColorVariance.rd)
        val endColorG = randomVariance(emitter.endColor.gd, emitter.endColorVariance.gd)
        val endColorB = randomVariance(emitter.endColor.bd, emitter.endColorVariance.bd)
        val endColorA = randomVariance(emitter.endColor.ad, emitter.endColorVariance.ad)

        particle.colorRdelta = ((endColorR - particle.colorR) / lifespan)
        particle.colorGdelta = ((endColorG - particle.colorG) / lifespan)
        particle.colorBdelta = ((endColorB - particle.colorB) / lifespan)
        particle.colorAdelta = ((endColorA - particle.colorA) / lifespan)

        val startRotation = randomVariance(emitter.rotationStart, emitter.rotationStartVariance)
        val endRotation = randomVariance(emitter.rotationEnd, emitter.rotationEndVariance)

        particle.rotation = startRotation
        particle.rotationDelta = (endRotation - startRotation) / lifespan

        return particle
    }

    fun advance(particle: Particle, _elapsedTime: Double) {
        val restTime = particle.totalTime - particle.currentTime
        val elapsedTime = if (restTime > _elapsedTime) _elapsedTime else restTime
        particle.currentTime += elapsedTime

        if (particle.currentTime < 0.0) return

        when (emitter.emitterType) {
            ParticleEmitter.Type.RADIAL -> {
                particle.emitRotation += particle.emitRotationDelta * elapsedTime
                particle.emitRadius += particle.emitRadiusDelta * elapsedTime
                particle.x = emitter.sourcePosition.x - cos(particle.emitRotation) * particle.emitRadius
                particle.y = emitter.sourcePosition.y - sin(particle.emitRotation) * particle.emitRadius
            }
            ParticleEmitter.Type.GRAVITY -> {
                val distanceX = particle.x - particle.startX
                val distanceY = particle.y - particle.startY
                val distanceScalar = sqrt(distanceX * distanceX + distanceY * distanceY).coerceAtLeast(0.01)
                var radialX = distanceX / distanceScalar
                var radialY = distanceY / distanceScalar
                var tangentialX = radialX
                var tangentialY = radialY

                radialX *= particle.radialAcceleration
                radialY *= particle.radialAcceleration

                val newY = tangentialX
                tangentialX = -tangentialY * particle.tangentialAcceleration
                tangentialY = newY * particle.tangentialAcceleration

                particle.velocityX += elapsedTime * (emitter.gravity.x + radialX + tangentialX)
                particle.velocityY += elapsedTime * (emitter.gravity.y + radialY + tangentialY)
                particle.x += particle.velocityX * elapsedTime
                particle.y += particle.velocityY * elapsedTime
            }
        }

        particle.scale += particle.scaleDelta * elapsedTime
        particle.rotation += particle.rotationDelta * elapsedTime

        particle.colorR += (particle.colorRdelta * elapsedTime).toFloat()
        particle.colorG += (particle.colorGdelta * elapsedTime).toFloat()
        particle.colorB += (particle.colorBdelta * elapsedTime).toFloat()
        particle.colorA += (particle.colorAdelta * elapsedTime).toFloat()

        if (!particle.alive && emitting) init(particle, false)
    }

    fun simulate(time: Double) {
        totalElapsedTime += (time * 1000.0).toInt()

        if (totalElapsedTime >= timeUntilStop) {
            emitting = false
        }

        particles.fastForEach { p ->
            advance(p, time)
        }
    }
}
