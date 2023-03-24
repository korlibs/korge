package korlibs.korge.particle

import korlibs.datastructure.iterators.*
import korlibs.graphics.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.file.*
import korlibs.io.lang.*
import korlibs.io.serialization.xml.*
import korlibs.math.geom.*

suspend fun VfsFile.readParticleEmitter(): ParticleEmitter {
    val file = this
    val emitter = ParticleEmitter()
    val particleXml = file.readXml()

    //var blendFuncSource = AGBlendFactor.ONE
    //var blendFuncDestination = AGBlendFactor.ONE

    particleXml.allChildrenNoComments.fastForEach { item ->
        fun point() = MPoint(item.double("x"), item.double("y"))
        fun scalar() = item.double("value")
        fun blendFactor() = ParticleEmitter.blendFactorMap[scalar().toInt()] ?: AGBlendFactor.ONE
        fun type() = ParticleEmitter.typeMap[scalar().toInt()] ?: ParticleEmitter.Type.GRAVITY

        fun angle() = item.double("value").degrees
        fun color(): RGBAf = RGBAf(item.double("red"), item.double("green"), item.double("blue"), item.double("alpha"))

        when (item.name.lowercase()) {
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
        Bitmap32(64, 64, premultiplied = true).context2d {
            fill(createRadialGradient(32.0, 32.0, 0.0, 32.0, 32.0, 32.0)
                .addColorStop(0.0, Colors.WHITE)
                .addColorStop(0.4, Colors.WHITE)
                .addColorStop(1.0, Colors.TRANSPARENT_WHITE)) { circle(Point(32, 32), 30f) }
        }.slice()
    }
    // After we load the texture, we set textureName to null, so it is not loaded again
    emitter.textureName = null
    return emitter
}