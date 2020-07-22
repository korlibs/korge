package common

import com.esotericsoftware.spine.*
import com.esotericsoftware.spine.assets.*
import com.esotericsoftware.spine.korge.*
import com.soywiz.korge.*
import com.soywiz.korge.view.*
import com.soywiz.korim.atlas.*
import com.soywiz.korim.color.*
import com.soywiz.korio.file.std.*

suspend fun main() = Korge(width = 800, height = 800, bgcolor = Colors["#2b2b2b"]) {
    //val json = SkeletonBinary(TextureAtlas(resourcesVfs["spineboy/spineboy-pma.atlas"].readAtlas())) // This loads skeleton JSON data, which is stateless.
    val json = SkeletonJson(resourcesVfs["spineboy/spineboy-pma.atlas"].readAtlas()) // This loads skeleton JSON data, which is stateless.
    json.scale = 0.6f // Load the skeleton at 60% the size it was in Spine.
    //val skeletonData = json.readSkeletonData(resourcesVfs["spineboy/spineboy-pro.skel"].toFileHandle())
    val skeletonData = json.readSkeletonData(resourcesVfs["spineboy/spineboy-pro.json"].toFileHandle())

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
        position(400, 500)
        skeletonView(skeleton, state)
        solidRect(10.0, 10.0, Colors.RED).centered
    }
}
