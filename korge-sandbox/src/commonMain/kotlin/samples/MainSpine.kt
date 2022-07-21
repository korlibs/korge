package samples

import com.esotericsoftware.spine.AnimationState
import com.esotericsoftware.spine.AnimationStateData
import com.esotericsoftware.spine.Skeleton
import com.esotericsoftware.spine.korge.skeletonView
import com.esotericsoftware.spine.readSkeletonBinary
import com.soywiz.korge.scene.ScaledScene
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.centered
import com.soywiz.korge.view.container
import com.soywiz.korge.view.filter.DropshadowFilter
import com.soywiz.korge.view.filter.filter
import com.soywiz.korge.view.filter.filters
import com.soywiz.korge.view.position
import com.soywiz.korge.view.scale
import com.soywiz.korge.view.solidRect
import com.soywiz.korim.atlas.readAtlas
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.asumePremultiplied
import com.soywiz.korim.bitmap.computePsnr
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.ImageDecodingProps
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.format.writeTo
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.std.resourcesVfs

class MainSpine : ScaledScene(1280, 720) {
    override suspend fun SContainer.sceneMain() {
        val atlas = resourcesVfs["spineboy/spineboy-pma.atlas"].readAtlas(ImageDecodingProps(asumePremultiplied = true))
        //val atlas = resourcesVfs["spineboy/spineboy-straight.atlas"].readAtlas(asumePremultiplied = true)
        //val skeletonData = resourcesVfs["spineboy/spineboy-pro.json"].readSkeletonJson(atlas, 0.6f)
        val skeletonData = resourcesVfs["spineboy/spineboy-pro.skel"].readSkeletonBinary(atlas, 0.6f)

        /*
        val pma = resourcesVfs["spineboy/spineboy-pma.png"].readBitmap().asumePremultiplied().toBMP32()
        //val sta = pma.depremultiplied()
        //sta.writeTo(localVfs("/tmp/spineboy-straight.png"), PNG)
        pma.writeTo(localVfs("/tmp/spineboy-straight.png"), PNG)
        val pma2 = localVfs("/tmp/spineboy-straight.png").readBitmap().asumePremultiplied().toBMP32()
        pma2.writeTo(localVfs("/tmp/spineboy-straight2.png"), PNG)
        val pma3 = localVfs("/tmp/spineboy-straight2.png").readBitmap().asumePremultiplied().toBMP32()
        val result = Bitmap32.Companion.computePsnr(pma, pma2)
        println("result=$result")
        */


        fun createSkel(): Pair<Skeleton, AnimationState> {
            val skeleton =
                Skeleton(skeletonData) // Skeleton holds skeleton state (bone positions, slot attachments, etc).
            val stateData = AnimationStateData(skeletonData) // Defines mixing (crossfading) between animations.
            stateData.setMix("run", "jump", 0.2f)
            stateData.setMix("jump", "run", 0.2f)

            val state =
                AnimationState(stateData) // Holds the animation state for a skeleton (current animation, time, etc).
            state.timeScale = 0.5f // Slow all animations down to 50% speed.

            // Queue animations on track 0.
            state.setAnimation(0, "run", true)
            state.addAnimation(0, "jump", false, 2f) // Jump after 2 seconds.
            state.addAnimation(0, "run", true, 0f) // Run after the jump.
            state.update(1f / 60f) // Update the animation time.
            state.apply(skeleton) // Poses skeleton using current animations. This sets the bones' local SRT.

            skeleton.updateWorldTransform() // Uses the bones' local SRT to compute their world SRT.
            return skeleton to state
        }

        // Add view
        container {
            val (skeleton, state) = createSkel()
            //speed = 2.0
            speed = 0.5
            scale(2.0)
            position(200, 700)
            skeletonView(skeleton, state).also { it.debugAnnotate = true }
            solidRect(10.0, 10.0, Colors.RED).centered
            filters(DropshadowFilter(shadowColor = Colors.RED))
        }

        container {
            val (skeleton, state) = createSkel()
            //speed = 2.0
            speed = 1.0
            scale(2.0)
            position(900, 700)
            skeletonView(skeleton, state).also { it.debugAnnotate = true }
            solidRect(10.0, 10.0, Colors.RED).centered
            //filters(DropshadowFilter(shadowColor = Colors.RED))
        }
    }
}
