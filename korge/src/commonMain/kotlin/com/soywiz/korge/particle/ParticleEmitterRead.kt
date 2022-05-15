package com.soywiz.korge.particle

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korag.AG
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.bitmap.slice
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBAf
import com.soywiz.korim.format.readBitmapSlice
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.lang.FileNotFoundException
import com.soywiz.korio.serialization.xml.readXml
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.vector.circle

suspend fun VfsFile.readParticleEmitter(): ParticleEmitter {
    val file = this
    val emitter = ParticleEmitter()
    val particleXml = file.readXml()

    //var blendFuncSource = AG.BlendFactor.ONE
    //var blendFuncDestination = AG.BlendFactor.ONE

    particleXml.allChildrenNoComments.fastForEach { item ->
        fun point() = Point(item.double("x"), item.double("y"))
        fun scalar() = item.double("value")
        fun blendFactor() = ParticleEmitter.blendFactorMap[scalar().toInt()] ?: AG.BlendFactor.ONE
        fun type() = ParticleEmitter.typeMap[scalar().toInt()] ?: ParticleEmitter.Type.GRAVITY

        fun angle() = item.double("value").degrees
        fun color(): RGBAf = RGBAf(item.double("red"), item.double("green"), item.double("blue"), item.double("alpha"))

        when (item.name.toLowerCase()) {
            "texture" -> emitter.textureName = item.str("name")
            "sourceposition" -> emitter.sourcePosition = point()
            "sourcepositionvariance" -> emitter.sourcePositionVariance = point()
            "speed" -> emitter.speed = scalar()
            "speedvariance" -> emitter.speedVariance = scalar()
            "particlelifespan" -> emitter.lifeSpan = scalar()
            "particlelifespanvariance" -> emitter.lifespanVariance = scalar()
            "angle" -> emitter.angle = angle()
            "anglevariance" -> emitter.angleVariance = angle()
            "gravity" -> emitter.gravity = point()
            "radialacceleration" -> emitter.radialAcceleration = scalar()
            "tangentialacceleration" -> emitter.tangentialAcceleration = scalar()
            "radialaccelvariance" -> emitter.radialAccelVariance = scalar()
            "tangentialaccelvariance" -> emitter.tangentialAccelVariance = scalar()
            "startcolor" -> emitter.startColor = color()
            "startcolorvariance" -> emitter.startColorVariance = color()
            "finishcolor" -> emitter.endColor = color()
            "finishcolorvariance" -> emitter.endColorVariance = color()
            "maxparticles" -> emitter.maxParticles = scalar().toInt()
            "startparticlesize" -> emitter.startSize = scalar()
            "startparticlesizevariance" -> emitter.startSizeVariance = scalar()
            "finishparticlesize" -> emitter.endSize = scalar()
            "finishparticlesizevariance" -> emitter.endSizeVariance = scalar()
            "duration" -> emitter.duration = scalar()
            "emittertype" -> emitter.emitterType = type()
            "maxradius" -> emitter.maxRadius = scalar()
            "maxradiusvariance" -> emitter.maxRadiusVariance = scalar()
            "minradius" -> emitter.minRadius = scalar()
            "minradiusvariance" -> emitter.minRadiusVariance = scalar()
            "rotatepersecond" -> emitter.rotatePerSecond = angle()
            "rotatepersecondvariance" -> emitter.rotatePerSecondVariance = angle()
            "blendfuncsource" -> emitter.blendFuncSource = blendFactor()
            "blendfuncdestination" -> emitter.blendFuncDestination = blendFactor()
            "rotationstart" -> emitter.rotationStart = angle()
            "rotationstartvariance" -> emitter.rotationStartVariance = angle()
            "rotationend" -> emitter.rotationEnd = angle()
            "rotationendvariance" -> emitter.rotationEndVariance = angle()
        }
    }

    emitter.texture = try {
        file.parent[emitter.textureName?.takeIf { it.isNotBlank() } ?: "texture.png"].readBitmapSlice()
    } catch (e: FileNotFoundException) {
        Bitmap32(64, 64).context2d {
            fill(createRadialGradient(32.0, 32.0, 0.0, 32.0, 32.0, 32.0)
                .addColorStop(0.0, Colors.WHITE)
                .addColorStop(0.4, Colors.WHITE)
                .addColorStop(1.0, Colors.TRANSPARENT_WHITE)) { circle(32.0, 32.0, 30.0) }
        }.slice()
    }
    // After we load the texture, we set textureName to null, so it is not loaded again
    emitter.textureName = null
    return emitter
}
