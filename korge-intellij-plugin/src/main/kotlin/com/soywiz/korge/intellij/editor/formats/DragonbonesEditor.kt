package com.soywiz.korge.intellij.editor.formats

import com.soywiz.korge.awt.*
import com.soywiz.korge.dragonbones.*
import com.soywiz.korge.intellij.editor.*
import com.soywiz.korio.file.*

suspend fun dragonBonesEditor(file: VfsFile) : KorgeBaseKorgeFileEditor.EditorModule {
    val factory = KorgeDbFactory()
    val skeleton = file.readDbSkeletonAndAtlas(factory)
    val armature = skeleton.armatureNames.first()
    val armatureDisplay = factory.buildArmatureDisplay(armature)!!
    val animationNames = armatureDisplay.animation.animationNames.toSet()
    val defaultAnimationName = when {
        "idle" in animationNames -> "idle"
        else -> animationNames.first()
    }

    val animation1Property = EditableEnumerableProperty("animation1", String::class, defaultAnimationName, animationNames.toSet()).apply {
        this.onChange { animationName ->
            armatureDisplay.animation.play(animationName)
        }
    }

    return createModule(EditableNodeList {
        add(EditableSection("Animation", animation1Property, armatureDisplay::speed.toEditableProperty(0.01, 10.0)))
    }) {
        //println(armatureDisplay.animation.animationNames)

        armatureDisplay.animation.play(defaultAnimationName)

        armatureDisplay.repositionOnResize(views)

        sceneView += armatureDisplay
    }
}
