package com.soywiz.korge.intellij.editor.formats

import com.esotericsoftware.spine.AnimationState
import com.esotericsoftware.spine.AnimationStateData
import com.esotericsoftware.spine.Skeleton
import com.esotericsoftware.spine.korge.skeletonView
import com.esotericsoftware.spine.readSkeletonBinary
import com.soywiz.korge.input.onClick
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.ui.textButton
import com.soywiz.korge.view.*
import com.soywiz.korim.atlas.readAtlas
import com.soywiz.korim.color.Colors
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName

suspend fun Scene.spineEditor(file: VfsFile) {
    sceneView.textButton(text = "Spine").apply {
        width = 80.0
        height = 24.0
        x = 0.0
        y = 0.0
        onClick {
        }
    }
    val atlas = file.parent.listSimple().firstOrNull { it.baseName.endsWith(".atlas") } ?: error("Can't find atlas in ${file.parent}")
    val skeletonData = file.readSkeletonBinary(atlas.readAtlas(asumePremultiplied = true))
    val skeleton = Skeleton(skeletonData)
    val stateData = AnimationStateData(skeletonData)
    val state = AnimationState(stateData)

    state.setAnimation(0, skeleton.data.animations.last().name, true)
    //skeleton.setPosition(250f, 20f)
    state.apply(skeleton) // Poses skeleton using current animations. This sets the bones' local SRT.
    //skeleton.updateWorldTransform() // Uses the bones' local SRT to compute their world SRT.
    val skeletonView = sceneView.skeletonView(skeleton, state)

    skeletonView.repositionOnResize(views)

    /*
    sceneView.container {
        //speed = 2.0
        speed = 0.5
        //speed = 1.0
        scale(1.0)
        position(400, 800)
        //solidRect(10.0, 10.0, Colors.RED).centered
    }
    */
}
