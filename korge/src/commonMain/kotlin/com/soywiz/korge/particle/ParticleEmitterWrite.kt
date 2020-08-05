package com.soywiz.korge.particle

import com.soywiz.korim.color.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korma.geom.*

suspend fun VfsFile.writeParticleEmitter(particle: ParticleEmitter) {
    val file = this
    file.writeString(buildXml("particleEmitterConfig") {
        fun nodeValue(name: String, value: Any?) {
            node(name, "value" to value)
        }
        fun nodePoint(name: String, point: IPoint) {
            node(name, "x" to point.x, "y" to point.y)
        }
        fun nodeColor(name: String, color: RGBAf) {
            node(name, "red" to color.rd, "green" to color.gd, "blue" to color.bd, "alpha" to color.ad)
        }

        node("texture", "name" to (particle.textureName ?: "texture.png"))
        nodePoint("sourcePosition", particle.sourcePosition)
        nodePoint("sourcePositionVariance", particle.sourcePositionVariance)
        nodeValue("speed", particle.speed)
        nodeValue("speedVariance", particle.speedVariance)
        nodeValue("particleLifeSpan", particle.lifeSpan)
        nodeValue("particleLifespanVariance", particle.lifespanVariance)
        nodeValue("angle", particle.angle.radians.degrees)
        nodeValue("angleVariance", particle.angleVariance.radians.degrees)
        nodePoint("gravity", particle.gravity)
        nodeValue("radialAcceleration", particle.radialAcceleration)
        nodeValue("radialAccelVariance", particle.radialAccelVariance)
        nodeValue("tangentialAcceleration", particle.tangentialAcceleration)
        nodeValue("tangentialAccelVariance", particle.tangentialAccelVariance)
        nodeColor("startColor", particle.startColor)
        nodeColor("startColorVariance", particle.startColorVariance)
        nodeColor("finishColor", particle.endColor)
        nodeColor("finishColorVariance", particle.endColorVariance)
        nodeValue("maxParticles", particle.maxParticles)
        nodeValue("startParticleSize", particle.startSize)
        nodeValue("startParticleSizeVariance", particle.startSizeVariance)
        nodeValue("finishParticleSize", particle.endSize)
        nodeValue("FinishParticleSizeVariance", particle.endSizeVariance)
        nodeValue("duration", particle.duration)
        nodeValue("emitterType", particle.emitterType.index)
        nodeValue("maxRadius", particle.maxRadius)
        nodeValue("maxRadiusVariance", particle.maxRadiusVariance)
        nodeValue("minRadius", particle.minRadius)
        nodeValue("minRadiusVariance", particle.minRadiusVariance)
        nodeValue("rotatePerSecond", particle.rotatePerSecond)
        nodeValue("rotatePerSecondVariance", particle.rotatePerSecondVariance)
        nodeValue("blendFuncSource", ParticleEmitter.blendFactorMapReversed[particle.blendFuncSource] ?: 1)
        nodeValue("blendFuncDestination", ParticleEmitter.blendFactorMapReversed[particle.blendFuncDestination] ?: 1)
        nodeValue("rotationStart", particle.rotationStart)
        nodeValue("rotationStartVariance", particle.rotationStartVariance)
        nodeValue("rotationEnd", particle.rotationEnd)
        nodeValue("rotationEndVariance", particle.rotationEndVariance)
    }.toOuterXmlIndented().toString())
}
