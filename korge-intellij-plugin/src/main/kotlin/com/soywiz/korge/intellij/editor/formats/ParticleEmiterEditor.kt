package com.soywiz.korge.intellij.editor.formats

import com.soywiz.korge.component.ResizeComponent
import com.soywiz.korge.component.docking.dockedTo
import com.soywiz.korge.input.onClick
import com.soywiz.korge.intellij.components.*
import com.soywiz.korge.intellij.editor.*
import com.soywiz.korge.particle.readParticle
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.ui.textButton
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korim.color.*
import com.soywiz.korio.file.VfsFile
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*

fun particleEmiterEditor(file: VfsFile): KorgeBaseKorgeFileEditor.EditorModule {
    val particle = runBlocking { file.readParticle() }

    return createModule(EditableNodeList {
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
            particle::lifespanVariance.toEditableProperty(0.0, 10.0),
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
        add(EditableSection("Start Size", particle::startSize.toEditableProperty(1.0, 1000.0), particle::startSizeVariance.toEditableProperty(1.0, 1000.0)))
        add(EditableSection("End Size", particle::endSize.toEditableProperty(1.0, 1000.0), particle::endSizeVariance.toEditableProperty(1.0, 1000.0)))
        add(EditableSection("Radius",
            particle::minRadius.toEditableProperty(0.0, 1000.0),
            particle::minRadiusVariance.toEditableProperty(0.0, 1000.0),
            particle::maxRadius.toEditableProperty(0.0, 1000.0),
            particle::maxRadiusVariance.toEditableProperty(0.0, 1000.0),
        ))
        add(EditableSection("Rotate",
            particle::rotatePerSecond.toEditableProperty(0.0, 1000.0),
            particle::rotatePerSecondVariance.toEditableProperty(0.0, 1000.0),
            particle::rotationStart.toEditableProperty(0.0, 1000.0),
            particle::rotationStartVariance.toEditableProperty(0.0, 1000.0),
            particle::rotationEnd.toEditableProperty(0.0, 1000.0),
            particle::rotationEndVariance.toEditableProperty(0.0, 1000.0),
        ))
        //add(EditableSection("Emitter Type", particle::emitterType.) // @TODO
        //add(EditableSection("Blend Factors", particle::blendFactors.) // @TODO
    }) {
        sceneView.textButton(text = "Particle").apply {
            width = 80.0
            height = 24.0
            x = 0.0
            y = 0.0
            onClick {
            }
        }
        sceneView += particle.create(0.0, 0.0).dockedTo(Anchor.MIDDLE_CENTER)
    }
}
