package com.esotericsoftware.spine

import com.esotericsoftware.spine.*
import com.esotericsoftware.spine.korge.*
import com.soywiz.korge.*
import com.soywiz.korge.view.*
import com.soywiz.korim.atlas.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korge.render.*
import com.soywiz.korio.util.*
import kotlin.test.*

class SampleTest {
    @Test
    fun test() = suspendTest({ !OS.isJs && !OS.isAndroid }) {
        val atlas = resourcesVfs["spineboy/spineboy-pma.atlas"].readAtlas()
        //val skeletonData = resourcesVfs["spineboy/spineboy-pro.json"].readSkeletonJson(atlas, 0.6f)
        val skeletonData = resourcesVfs["spineboy/spineboy-pro.skel"].readSkeletonBinary(atlas, 0.6f)

        val skeleton = Skeleton(skeletonData) // Skeleton holds skeleton state (bone positions, slot attachments, etc).
        skeleton.setPosition(250f, 20f)

        val stateData = AnimationStateData(skeletonData) // Defines mixing (crossfading) between animations.
        stateData.setMix("run", "jump", 0.2f)
        stateData.setMix("jump", "run", 0.2f)

        val state = AnimationState(stateData) // Holds the animation state for a skeleton (current animation, time, etc).
        state.timeScale = 0.5f // Slow all animations down to 50% speed.

        // Queue animations on track 0.
        state.setAnimation(0, "run", true)
        state.addAnimation(0, "jump", false, 2f) // Jump after 2 seconds.
        state.addAnimation(0, "run", true, 0f) // Run after the jump.

        state.update(1f / 60f) // Update the animation time.

        state.apply(skeleton) // Poses skeleton using current animations. This sets the bones' local SRT.
        skeleton.updateWorldTransform() // Uses the bones' local SRT to compute their world SRT.
        val view = SkeletonView(skeleton, state)
        val log = testRenderContext {
            view.render(it)
        }
        //println(log)
    }

    @Test
    fun testDukeJson() = suspendTest({ !OS.isJs && !OS.isAndroid }) {
        val atlas = resourcesVfs["duke/3.8/Duke.atlas"].readAtlas()
        val skeletonData = resourcesVfs["duke/3.8/Duke.json"].readSkeletonJson(atlas, 0.6f)
        val skeleton = Skeleton(skeletonData)
        val stateData = AnimationStateData(skeletonData)
        val state = AnimationState(stateData)
    }
}
