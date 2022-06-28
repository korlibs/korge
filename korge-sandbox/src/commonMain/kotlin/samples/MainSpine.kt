package samples

import com.esotericsoftware.spine.AnimationState
import com.esotericsoftware.spine.AnimationStateData
import com.esotericsoftware.spine.Skeleton
import com.esotericsoftware.spine.korge.skeletonView
import com.esotericsoftware.spine.readSkeletonBinary
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.centered
import com.soywiz.korge.view.container
import com.soywiz.korge.view.position
import com.soywiz.korge.view.scale
import com.soywiz.korge.view.solidRect
import com.soywiz.korim.atlas.readAtlas
import com.soywiz.korim.color.Colors
import com.soywiz.korio.file.std.resourcesVfs

class MainSpine : Scene() {
    override suspend fun SContainer.sceneMain() {
        val atlas = resourcesVfs["spineboy/spineboy-pma.atlas"].readAtlas(asumePremultiplied = true)
        //val skeletonData = resourcesVfs["spineboy/spineboy-pro.json"].readSkeletonJson(atlas, 0.6f)
        val skeletonData = resourcesVfs["spineboy/spineboy-pro.skel"].readSkeletonBinary(atlas, 0.6f)

        val skeleton = Skeleton(skeletonData) // Skeleton holds skeleton state (bone positions, slot attachments, etc).
        //skeleton.setPosition(250f, 20f)

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

        // Add view
        container {
            //speed = 2.0
            speed = 0.5
            scale(2.0)
            position(400, 800)
            skeletonView(skeleton, state)
            solidRect(10.0, 10.0, Colors.RED).centered
        }
    }
}
