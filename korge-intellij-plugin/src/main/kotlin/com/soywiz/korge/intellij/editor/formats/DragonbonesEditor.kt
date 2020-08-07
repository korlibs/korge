package com.soywiz.korge.intellij.editor.formats

import com.soywiz.korge.awt.*
import com.soywiz.korge.dragonbones.*
import com.soywiz.korge.intellij.editor.*
import com.soywiz.korge.intellij.editor.util.*
import com.soywiz.korge.scene.*
import com.soywiz.korio.file.*

suspend fun dragonBonesEditor(file: VfsFile) : Module {
    val factory = KorgeDbFactory()
    val skeleton = file.readDbSkeletonAndAtlas(factory)
    val armature = skeleton.armatureNames.first()
    val armatureDisplay = factory.buildArmatureDisplay(armature)!!
    val animationNames = armatureDisplay.animation.animationNames.toSet()
    val defaultAnimationName = when {
        "idle" in animationNames -> "idle"
        else -> animationNames.first()
    }

    return createModule {
        armatureDisplay.animation.play(defaultAnimationName)
        armatureDisplay.repositionOnResize(views)
        sceneView += armatureDisplay
        views.debugHightlightView(armatureDisplay)
    }
}
