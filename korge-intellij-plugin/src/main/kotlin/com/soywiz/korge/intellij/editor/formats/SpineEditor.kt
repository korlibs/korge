package com.soywiz.korge.intellij.editor.formats

import com.esotericsoftware.spine.*
import com.esotericsoftware.spine.ext.*
import com.esotericsoftware.spine.korge.*
import com.soywiz.korge.awt.*
import com.soywiz.korge.intellij.editor.*
import com.soywiz.korim.atlas.readAtlas
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
                vSkeletonView?.play()
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
    val animationSpeed = EditableNumericProperty("animationSpeed", Double::class, 1.0, 0.01, 10.0).apply {
        this.onChange {
            vSkeletonView?.play()
            vSkeletonView?.speed = it
            //state.setAnimation(0, it, true)
        }
    }

    val animationRatio = EditableNumericProperty("animationRatio", Double::class, 1.0, 0.00, 1.0).apply {
        this.onChange {
            vSkeletonView?.ratio = it
            //state.setAnimation(0, it, true)
        }
    }

    val playButton = EditableButtonProperty("play") { vSkeletonView?.play() }
    val stopButton = EditableButtonProperty("stop") { vSkeletonView?.stop() }

    return createModule(EditableNodeList {
        //add(EditableSection("Animation", animation1Property, animation2Property, blendingFactor, animationSpeed))
        add(EditableSection("Animation", animation1Property, animationSpeed, animationRatio, playButton, stopButton))
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
