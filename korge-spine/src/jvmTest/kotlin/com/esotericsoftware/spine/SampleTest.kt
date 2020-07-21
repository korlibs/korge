package com.esotericsoftware.spine

import com.badlogic.gdx.files.*
import com.badlogic.gdx.graphics.g2d.*
import kotlin.test.*

class SampleTest {
    @Test
    //@Ignore
    fun test() {
        val atlas = TextureAtlas(FileHandle("spineboy/spineboy-pma.atlas"))
        val json = SkeletonBinary(atlas) // This loads skeleton JSON data, which is stateless.
        json.scale = 0.6f // Load the skeleton at 60% the size it was in Spine.
        val skeletonData = json.readSkeletonData(FileHandle("spineboy/spineboy-pro.skel"))

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

        state.update(1f / 60f); // Update the animation time.

        state.apply(skeleton); // Poses skeleton using current animations. This sets the bones' local SRT.
        skeleton.updateWorldTransform(); // Uses the bones' local SRT to compute their world SRT.
    }
}
