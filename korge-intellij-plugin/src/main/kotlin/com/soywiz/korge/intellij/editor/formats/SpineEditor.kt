package com.soywiz.korge.intellij.editor.formats

import com.esotericsoftware.spine.*
import com.esotericsoftware.spine.ext.*
import com.esotericsoftware.spine.korge.*
import com.soywiz.klock.*
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
import com.soywiz.korma.geom.*

suspend fun spineEditor(file: VfsFile): KorgeBaseKorgeFileEditor.EditorModule {
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
    var reposition: RepositionResult? = null


    var vSkeletonView: SkeletonView? = null
    val animation1Property = EditableEnumerableProperty("animation1", String::class, defaultAnimationName, animationNames.toSet()).apply {
        this.onChange { animationName ->
            val animation = skeletonData.findAnimation(animationName)
            if (animation != null) {
                state.setAnimation(0, animation, true)
                reposition?.forceBounds(animation.getAnimationMaxBounds(skeleton.data))
            }
        }
    }
    //val animation2Property = EditableEnumerableProperty("animation2", String::class, defaultAnimationName, animationNames.toSet()).apply {
    //    this.onChange {
    //        state.setAnimation(0, it, true)
    //    }
    //}
    //val blendingFactor = EditableNumericProperty("blending", Double::class, 0.0, 0.0, 1.0).apply {
    //    this.onChange {
    //        stateData.defaultMix
    //        //state.setAnimation(0, it, true)
    //    }
    //}

    return createModule(EditableNodeList {
        //add(EditableSection("Animation", animation1Property, animation2Property, blendingFactor))
        add(EditableSection("Animation", animation1Property))
    }) {
        state.setAnimation(0, defaultAnimationName, true)
        //skeleton.setPosition(250f, 20f)
        state.apply(skeleton) // Poses skeleton using current animations. This sets the bones' local SRT.
        //skeleton.updateWorldTransform() // Uses the bones' local SRT to compute their world SRT.
        val skeletonView = sceneView.skeletonView(skeleton, state)
        vSkeletonView = skeletonView
        reposition = skeletonView.repositionOnResize(views)
    }
}
