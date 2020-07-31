package com.soywiz.korge.intellij.editor.formats

import com.esotericsoftware.spine.AnimationState
import com.esotericsoftware.spine.AnimationStateData
import com.esotericsoftware.spine.Skeleton
import com.esotericsoftware.spine.korge.skeletonView
import com.esotericsoftware.spine.readSkeletonBinary
import com.soywiz.korge.input.onClick
import com.soywiz.korge.intellij.components.*
import com.soywiz.korge.intellij.editor.*
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.ui.textButton
import com.soywiz.korge.view.*
import com.soywiz.korim.atlas.readAtlas
import com.soywiz.korim.color.Colors
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName

suspend fun spineEditor(file: VfsFile): KorgeBaseKorgeFileEditor.EditorModule {
    val atlas = file.parent.listSimple().firstOrNull { it.baseName.endsWith(".atlas") }
        ?: error("Can't find atlas in ${file.parent}")
    val skeletonData = file.readSkeletonBinary(atlas.readAtlas(asumePremultiplied = true))
    val skeleton = Skeleton(skeletonData)
    val stateData = AnimationStateData(skeletonData)
    val state = AnimationState(stateData)

    val animationNames = skeleton.data.animations.map { it.name }

    val defaultAnimationName = when {
        "idle" in animationNames -> "idle"
        "walk" in animationNames -> "walk"
        "run" in animationNames -> "run"
        else -> animationNames.lastOrNull() ?: "unknown"
    }

    val animationProperty = EditableEnumerableProperty("animation", String::class, defaultAnimationName, animationNames.toSet()).apply {
        this.onChange {
            state.setAnimation(0, it, true)
        }
    }

    return createModule(EditableNodeList {
        add(EditableSection("Animation", animationProperty))
    }) {
        state.setAnimation(0, defaultAnimationName, true)
        //skeleton.setPosition(250f, 20f)
        state.apply(skeleton) // Poses skeleton using current animations. This sets the bones' local SRT.
        //skeleton.updateWorldTransform() // Uses the bones' local SRT to compute their world SRT.
        val skeletonView = sceneView.skeletonView(skeleton, state)

        skeletonView.repositionOnResize(views)
    }
}
