package com.soywiz.korge.intellij.editor.formats

import com.soywiz.korge.dragonbones.KorgeDbFactory
import com.soywiz.korge.dragonbones.readDbSkeletonAndAtlas
import com.soywiz.korge.input.onClick
import com.soywiz.korge.ui.textButton
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.centerOnStage
import com.soywiz.korge.view.position
import com.soywiz.korge.view.scale
import com.soywiz.korio.file.VfsFile

suspend fun Container.dragonBonesEditor(file: VfsFile) {
    textButton(text = "Dragonbones").apply {
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

    armatureDisplay.position(armatureDisplay.getLocalBounds().size.p)
    //armatureDisplay.centerOnStage()

    this += armatureDisplay
}
