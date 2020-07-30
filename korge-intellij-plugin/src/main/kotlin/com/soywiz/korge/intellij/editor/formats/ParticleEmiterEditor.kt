package com.soywiz.korge.intellij.editor.formats

import com.intellij.openapi.command.undo.*
import com.intellij.openapi.vfs.*
import com.soywiz.korge.component.docking.*
import com.soywiz.korge.input.*
import com.soywiz.korge.intellij.*
import com.soywiz.korge.intellij.components.*
import com.soywiz.korge.intellij.editor.*
import com.soywiz.korge.intellij.util.*
import com.soywiz.korge.particle.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*

fun particleEmiterEditor(file: VfsFile): KorgeBaseKorgeFileEditor.EditorModule {
    val particle = runBlocking { file.readParticle() }

    suspend fun save() {
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

            node("texture", "name" to "texture.png")
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

    val nodeTree = EditableNodeList {
        add(EditableSection("Emitter Type", particle::emitterType.toEditableProperty()))
        add(EditableSection("Blend Factors", particle::blendFuncSource.toEditableProperty(), particle::blendFuncDestination.toEditableProperty()))
        add(EditableSection("Angle",
            particle::angle.toEditableProperty(0.0, 360.0, 0.0, kotlin.math.PI * 2),
            particle::angleVariance.toEditableProperty(0.0, 360.0, 0.0, kotlin.math.PI * 2)
        ))
        add(EditableSection("Speed",
            particle::speed.toEditableProperty(0.0, 1000.0),
            particle::speedVariance.toEditableProperty(0.0, 1000.0),
        ))
        add(EditableSection("Lifespan",
            particle::lifeSpan.toEditableProperty(0.0, 10.0),
            particle::lifespanVariance.toEditableProperty(-10.0, 10.0),
            particle::duration.toEditableProperty(-10.0, 10.0),
        ))
        add(EditableSection("Gravity", particle.gravity.editableNodes()))
        add(EditableSection("Source Position", particle.sourcePosition.editableNodes()))
        add(EditableSection("Source Position Variance", particle.sourcePositionVariance.editableNodes()))
        add(EditableSection("Acceleration",
            particle::radialAcceleration.toEditableProperty(-1000.0, +1000.0),
            particle::radialAccelVariance.toEditableProperty(-1000.0, +1000.0),
            particle::tangentialAcceleration.toEditableProperty(-1000.0, +1000.0),
            particle::tangentialAccelVariance.toEditableProperty(-1000.0, +1000.0)
        ))
        add(EditableSection("Start Color", particle.startColor.editableNodes()))
        add(EditableSection("Start Color Variance", particle.startColorVariance.editableNodes(variance = true)))
        add(EditableSection("End Color", particle.endColor.editableNodes()))
        add(EditableSection("End Color Variance", particle.endColor.editableNodes(variance = true)))
        add(EditableSection("Max particles", particle::maxParticles.toEditableProperty(1, 20000)))
        add(EditableSection("Start Size", particle::startSize.toEditableProperty(1.0, 1000.0), particle::startSizeVariance.toEditableProperty(-1000.0, 1000.0)))
        add(EditableSection("End Size", particle::endSize.toEditableProperty(1.0, 1000.0), particle::endSizeVariance.toEditableProperty(-1000.0, 1000.0)))
        add(EditableSection("Radius",
            particle::minRadius.toEditableProperty(0.0, 1000.0),
            particle::minRadiusVariance.toEditableProperty(-1000.0, 1000.0),
            particle::maxRadius.toEditableProperty(0.0, 1000.0),
            particle::maxRadiusVariance.toEditableProperty(-1000.0, 1000.0),
        ))
        add(EditableSection("Rotate",
            particle::rotatePerSecond.toEditableProperty(0.0, 1000.0),
            particle::rotatePerSecondVariance.toEditableProperty(-1000.0, 1000.0),
            particle::rotationStart.toEditableProperty(0.0, 1000.0),
            particle::rotationStartVariance.toEditableProperty(-1000.0, 1000.0),
            particle::rotationEnd.toEditableProperty(0.0, 1000.0),
            particle::rotationEndVariance.toEditableProperty(-1000.0, 1000.0),
        ))
    }

    var doSave = false

    for (property in nodeTree.getAllBaseEditableProperty()) {
        property.onChange {
            doSave = true
        }
    }

    return createModule(nodeTree) {
        sceneView.addUpdater {
            if (doSave) {
                doSave = false
                launchImmediately {
                    save()
                }
            }
        }

        sceneView += particle.create(0.0, 0.0).dockedTo(Anchor.MIDDLE_CENTER)
    }
}
