package com.soywiz.korge.intellij.editor.formats

import com.esotericsoftware.spine.*
import com.esotericsoftware.spine.ext.*
import com.esotericsoftware.spine.korge.*
import com.soywiz.korge.awt.*
import com.soywiz.korge.intellij.editor.*
import com.soywiz.korge.intellij.editor.util.*
import com.soywiz.korge.scene.*
import com.soywiz.korim.atlas.readAtlas
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName

suspend fun spineEditor(file: VfsFile): Module {
    val atlas = file.parent.listSimple().firstOrNull { it.baseName.endsWith(".atlas") }
        ?: error("Can't find atlas in ${file.parent}")
    val skeletonData = file.readSkeletonBinary(atlas.readAtlas(asumePremultiplied = true))
    val skeleton = Skeleton(skeletonData)
    val stateData = AnimationStateData(skeletonData)
    val state = AnimationState(stateData)

    val animationNames = skeleton.data.animations.map { it.name }

    val defaultAnimationName = when {
        //"portal" in animationNames -> "portal"
        "idle" in animationNames -> "idle"
        "walk" in animationNames -> "walk"
        "run" in animationNames -> "run"
        else -> animationNames.lastOrNull() ?: "unknown"
    }

    return createModule {
        state.setAnimation(0, defaultAnimationName, true)
        //skeleton.setPosition(250f, 20f)
        state.apply(skeleton) // Poses skeleton using current animations. This sets the bones' local SRT.
        //skeleton.updateWorldTransform() // Uses the bones' local SRT to compute their world SRT.
        val skeletonView = sceneView.skeletonView(skeleton, state)
        val reposition = skeletonView.repositionOnResize(views)
        views.debugHighlighters.add {
            val animation = skeletonView.currentMainAnimation
            if (animation != null) {
                reposition.forceBounds(animation.getAnimationMaxBounds(skeleton.data))
            }
        }
        views.debugHightlightView(skeletonView)
    }
}
