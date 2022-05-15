package com.soywiz.korge.particle

import com.soywiz.korim.color.RGBAf
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.serialization.xml.buildXml
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.degrees

suspend fun VfsFile.writeParticleEmitter(particle: ParticleEmitter) {
    val file = this
    file.writeString(buildXml("particleEmitterConfig") {
        fun nodeValue(name: String, value: Any?) {
            node(name, "value" to value)
        }
        fun nodeAngle(name: String, value: Angle) {
            node(name, "value" to value.degrees)
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
        nodeAngle("angle", particle.angle)
        nodeAngle("angleVariance", particle.angleVariance)
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
        nodeAngle("rotatePerSecond", particle.rotatePerSecond)
        nodeAngle("rotatePerSecondVariance", particle.rotatePerSecondVariance)
        nodeValue("blendFuncSource", ParticleEmitter.blendFactorMapReversed[particle.blendFuncSource] ?: 1)
        nodeValue("blendFuncDestination", ParticleEmitter.blendFactorMapReversed[particle.blendFuncDestination] ?: 1)
        nodeAngle("rotationStart", particle.rotationStart)
        nodeAngle("rotationStartVariance", particle.rotationStartVariance)
        nodeAngle("rotationEnd", particle.rotationEnd)
        nodeAngle("rotationEndVariance", particle.rotationEndVariance)
    }.toOuterXmlIndented().toString())
}
