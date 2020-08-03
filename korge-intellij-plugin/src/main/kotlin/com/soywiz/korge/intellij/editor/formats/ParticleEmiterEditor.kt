package com.soywiz.korge.intellij.editor.formats

import com.soywiz.korge.awt.*
import com.soywiz.korge.component.docking.*
import com.soywiz.korge.intellij.editor.*
import com.soywiz.korge.particle.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korma.geom.*
import kotlin.math.*

suspend fun particleEmiterEditor(file: VfsFile): KorgeBaseKorgeFileEditor.EditorModule {
    val particle = file.readParticleEmitter()

    val nodeTree = EditableNodeList {
        add(EditableSection("Emitter Type", particle::emitterType.toEditableProperty()))
        add(EditableSection("Blend Factors", particle::blendFuncSource.toEditableProperty(), particle::blendFuncDestination.toEditableProperty()))
        add(EditableSection("Angle",
            particle::angle.toEditableProperty(0.0, 360.0, 0.0, PI * 2),
            particle::angleVariance.toEditableProperty(0.0, 360.0, 0.0, PI * 2)
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
                    file.writeParticleEmitter(particle)
                }
            }
        }

        sceneView += particle.create(0.0, 0.0).dockedTo(Anchor.MIDDLE_CENTER)
    }
}
