package korlibs.korge.particle

import korlibs.datastructure.*
import korlibs.graphics.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.korge.view.property.*
import korlibs.math.geom.*
import korlibs.time.*

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(ParticleEmitter.Factory::class)
class ParticleEmitter() {
	enum class Type(val index: Int) {
        GRAVITY(0), RADIAL(1)
    }

    var textureName: String? = null
	var texture: BmpSlice? = null
    @ViewProperty
	var sourcePosition = Point()
    @ViewProperty
	var sourcePositionVariance = Point()
    @ViewProperty
	var speed = 100.0
    @ViewProperty
	var speedVariance = 30.0
    @ViewProperty
	var lifeSpan = 2.0
    @ViewProperty
	var lifespanVariance = 1.9
    @ViewProperty
	var angle: Angle = 270.0.degrees
    @ViewProperty
	var angleVariance: Angle = 360.0.degrees
    @ViewProperty
	var gravity = Point()
    @ViewProperty
	var radialAcceleration = 0.0
    @ViewProperty
	var tangentialAcceleration = 0.0
    @ViewProperty
	var radialAccelVariance = 0.0
    @ViewProperty
	var tangentialAccelVariance = 0.0
    @ViewProperty
	var startColor = RGBAf(1f, 1f, 1f, 1f)
    @ViewProperty
	var startColorVariance = RGBAf(0f, 0f, 0f, 0f)
    @ViewProperty
	var endColor = RGBAf(1f, 1f, 1f, 0f)
    @ViewProperty
	var endColorVariance = RGBAf(0f, 0f, 0f, 0f)
    @ViewProperty
	var maxParticles = 500
    @ViewProperty
	var startSize = 70.0
    @ViewProperty
	var startSizeVariance = 50.0
    @ViewProperty
	var endSize = 10.0
    @ViewProperty
	var endSizeVariance = 5.0
    @ViewProperty
	var duration = -1.0
    @ViewProperty
	var emitterType = Type.GRAVITY
    @ViewProperty(groupName = "radius")
	var maxRadius = 0.0
    @ViewProperty(groupName = "radius")
	var maxRadiusVariance = 0.0
    @ViewProperty(groupName = "radius")
	var minRadius = 0.0
    @ViewProperty(groupName = "radius")
	var minRadiusVariance = 0.0
    @ViewProperty(groupName = "rotation")
	var rotatePerSecond = 0.0.degrees
    @ViewProperty(groupName = "rotation")
	var rotatePerSecondVariance = 0.0.degrees
    @ViewProperty
    var blendFuncSource = AGBlendFactor.SOURCE_ALPHA
    @ViewProperty
    var blendFuncDestination = AGBlendFactor.ONE
    @ViewProperty(groupName = "rotation")
	var rotationStart = 0.0.degrees
    @ViewProperty(groupName = "rotation")
	var rotationStartVariance = 0.0.degrees
    @ViewProperty(groupName = "rotation")
	var rotationEnd = 0.0.degrees
    @ViewProperty(groupName = "rotation")
	var rotationEndVariance = 0.0.degrees

	fun create(x: Double = 0.0, y: Double = 0.0, time: TimeSpan = TimeSpan.NIL): ParticleEmitterView =
		ParticleEmitterView(this, MPoint(x, y)).apply {
			this.timeUntilStop = time
		}

    companion object {
        val blendFactorMap = mapOf(
            0 to AGBlendFactor.ZERO,
            1 to AGBlendFactor.ONE,
            0x300 to AGBlendFactor.SOURCE_COLOR,
            0x301 to AGBlendFactor.ONE_MINUS_SOURCE_COLOR,
            0x302 to AGBlendFactor.SOURCE_ALPHA,
            0x303 to AGBlendFactor.ONE_MINUS_SOURCE_ALPHA,
            0x304 to AGBlendFactor.DESTINATION_ALPHA,
            0x305 to AGBlendFactor.ONE_MINUS_DESTINATION_ALPHA,
            0x306 to AGBlendFactor.DESTINATION_COLOR,
            0x307 to AGBlendFactor.ONE_MINUS_DESTINATION_COLOR,
        )
        val blendFactorMapReversed = blendFactorMap.flip()
        val typeMap = Type.values().associateBy { it.index }
        val typeMapReversed = typeMap.flip()
    }

}
