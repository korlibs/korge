package com.soywiz.korge.particle

import com.soywiz.kmem.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*

internal const val PARTICLE_STRIDE = 27

inline class Particle(val index: Int) {
    val offset get() = index * PARTICLE_STRIDE
}

open class ParticleContainer(val max: Int) {
    val data = FBuffer(max * PARTICLE_STRIDE * Float.SIZE_BYTES)
    val f32 = data.f32

    var Particle.rotation: Angle get() = rotationRadians.radians; set(value) { rotationRadians = value.radians.toFloat() }

    var Particle.x: Float get() = f32[offset + 0]; set(value) { f32[offset + 0] = value }
    var Particle.y: Float get() = f32[offset + 1]; set(value) { f32[offset + 1] = value }
    var Particle.scale: Float get() = f32[offset + 2]; set(value) { f32[offset + 2] = value }
    var Particle.rotationRadians: Float get() = f32[offset + 3]; set(value) { f32[offset + 3] = value }
    var Particle.currentTime: Float get() = f32[offset + 4]; set(value) { f32[offset + 4] = value }
    var Particle.totalTime: Float get() = f32[offset + 5]; set(value) { f32[offset + 5] = value }

    //val colorArgb: RGBAf = RGBAf(),
    //val colorArgbDelta: RGBAf = RGBAf(),

    var Particle.colorR: Float get() = f32[offset + 6]; set(value) { f32[offset + 6] = value }
    var Particle.colorG: Float get() = f32[offset + 7]; set(value) { f32[offset + 7] = value }
    var Particle.colorB: Float get() = f32[offset + 8]; set(value) { f32[offset + 8] = value }
    var Particle.colorA: Float get() = f32[offset + 9]; set(value) { f32[offset + 9] = value }

    var Particle.colorRdelta: Float get() = f32[offset + 10]; set(value) { f32[offset + 10] = value }
    var Particle.colorGdelta: Float get() = f32[offset + 11]; set(value) { f32[offset + 11] = value }
    var Particle.colorBdelta: Float get() = f32[offset + 12]; set(value) { f32[offset + 12] = value }
    var Particle.colorAdelta: Float get() = f32[offset + 13]; set(value) { f32[offset + 13] = value }

    var Particle.startX: Float get() = f32[offset + 14]; set(value) { f32[offset + 14] = value }
    var Particle.startY: Float get() = f32[offset + 15]; set(value) { f32[offset + 15] = value }
    var Particle.velocityX: Float get() = f32[offset + 16]; set(value) { f32[offset + 16] = value }
    var Particle.velocityY: Float get() = f32[offset + 17]; set(value) { f32[offset + 17] = value }
    var Particle.radialAcceleration: Float get() = f32[offset + 18]; set(value) { f32[offset + 18] = value }
    var Particle.tangentialAcceleration: Float get() = f32[offset + 19]; set(value) { f32[offset + 19] = value }
    var Particle.emitRadius: Float get() = f32[offset + 20]; set(value) { f32[offset + 20] = value }
    var Particle.emitRadiusDelta: Float get() = f32[offset + 21]; set(value) { f32[offset + 21] = value }
    var Particle.scaleDelta: Float get() = f32[offset + 22]; set(value) { f32[offset + 22] = value }

    var Particle.emitRotationRadians: Float get() = f32[offset + 23]; set(value) { f32[offset + 23] = value }
    var Particle.emitRotationDeltaRadians: Float get() = f32[offset + 24]; set(value) { f32[offset + 24] = value }
    var Particle.rotationDeltaRadians: Float get() = f32[offset + 25]; set(value) { f32[offset + 25] = value }

    var Particle.initializedFloat: Float get() = f32[offset + 26]; set(value) { f32[offset + 26] = value }

    var Particle.emitRotation: Angle get() = emitRotationRadians.radians; set(value) { emitRotationRadians = value.radians.toFloat() }
    var Particle.emitRotationDelta: Angle get() = emitRotationDeltaRadians.radians; set(value) { emitRotationDeltaRadians = value.radians.toFloat() }
    var Particle.rotationDelta: Angle get() = rotationDeltaRadians.radians; set(value) { rotationDeltaRadians = value.radians.toFloat() }

    val Particle.color: RGBA get() = RGBA.float(colorR, colorG, colorB, colorA)
    val Particle.alive: Boolean get() = this.currentTime >= 0.0 && this.currentTime < this.totalTime
    var Particle.initialized: Boolean
        set(value) { initializedFloat = if (value) 1f else 0f }
        get() = initializedFloat != 0f

    fun Particle.toStringDefault() = "Particle(initialized=$initialized,pos=($x,$y),start=($startX,$startY),velocity=($velocityX,$velocityY),scale=$scale,rotation=$rotation,time=$currentTime/$totalTime,color=$color,colorDelta=$colorRdelta,$colorGdelta,$colorBdelta,$colorAdelta),radialAcceleration=$radialAcceleration,tangentialAcceleration=$tangentialAcceleration,emitRadius=$emitRadius,emitRadiusDelta=$emitRadiusDelta,scaleDelta=$scaleDelta,emitRotation=$emitRotation,emitRotationDelta=$emitRotationDelta"

    init {
        for (n in 0 until max) {
            val particle = Particle(n)
            particle.scale = 1f
            particle.colorR = 1f
            particle.colorG = 1f
            particle.colorB = 1f
            particle.colorA = 1f
        }
    }

    override fun toString(): String = "ParticleContainer[$max](\n${map { it.toStringDefault() }.joinToString("\n")}\n)"
}

inline fun <T : ParticleContainer> T.fastForEach(max: Int = this.max, block: T.(Particle) -> Unit): T {
    for (n in 0 until max) {
        block(Particle(n))
    }
    return this
}

inline fun <T : ParticleContainer, R> T.map(max: Int = this.max, block: T.(Particle) -> R): List<R> =
    (0 until max).map { block(Particle(it)) }
