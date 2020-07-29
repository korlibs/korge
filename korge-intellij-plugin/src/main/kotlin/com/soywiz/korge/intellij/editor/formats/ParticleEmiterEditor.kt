package com.soywiz.korge.intellij.editor.formats

import com.soywiz.korge.component.ResizeComponent
import com.soywiz.korge.component.docking.dockedTo
import com.soywiz.korge.input.onClick
import com.soywiz.korge.particle.readParticle
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.ui.textButton
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korio.file.VfsFile
import com.soywiz.korma.geom.Anchor

suspend fun Scene.particleEmiterEditor(file: VfsFile) {
    sceneView.textButton(text = "Particle").apply {
        width = 80.0
        height = 24.0
        x = 0.0
        y = 0.0
        onClick {
        }
    }
    sceneView += file.readParticle().create(0.0, 0.0).dockedTo(Anchor.MIDDLE_CENTER)
}
