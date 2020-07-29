package com.soywiz.korge.intellij.editor.formats

import com.soywiz.korge.component.ResizeComponent
import com.soywiz.korge.component.docking.dockedTo
import com.soywiz.korge.component.onStageResized
import com.soywiz.korge.dragonbones.KorgeDbFactory
import com.soywiz.korge.dragonbones.readDbSkeletonAndAtlas
import com.soywiz.korge.input.onClick
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.ui.textButton
import com.soywiz.korge.view.*
import com.soywiz.korio.file.VfsFile
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.ScaleMode

suspend fun Scene.dragonBonesEditor(file: VfsFile) {
    sceneView.textButton(text = "Dragonbones").apply {
        width = 80.0
        height = 24.0
        x = 0.0
        y = 0.0
        onClick {
        }
    }
    val factory = KorgeDbFactory()
    val skeleton = file.readDbSkeletonAndAtlas(factory)
    val armatureDisplay = factory.buildArmatureDisplay(skeleton.armatureNames.first())!!

    //armatureDisplay.animation.play("walk")
    println(armatureDisplay.animation.animationNames)
    //armatureDisplay.animation.play("jump")
    armatureDisplay.animation.play(armatureDisplay.animation.animationNames.first())

    armatureDisplay.repositionOnResize(views)

    sceneView += armatureDisplay
}
