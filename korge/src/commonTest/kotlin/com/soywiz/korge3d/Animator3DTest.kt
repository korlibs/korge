package com.soywiz.korge3d

import com.soywiz.kds.*
import com.soywiz.klock.seconds
import com.soywiz.korge3d.animation.*
import com.soywiz.korge3d.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import it.krzeminski.visassert.assertFunctionConformsTo
import kotlin.test.*

@Korge3DExperimental
class Animator3DTest {
    @Test
    fun updateWithDefaultPlaybackPattern() = suspendTestNoBrowser {
        val library = resourcesVfs["skinning.dae"].readColladaLibrary()
        val animation = library.animationDefs["Armature_Bone_pose_matrix"]
            ?: throw RuntimeException("Animation not found")
        val view3d = object : View3D() {
            override fun render(ctx: RenderContext3D) {
                throw RuntimeException("Not relevant - shouldn't be called in this unit test")
            }
        }
        val animator = Animator3D(animation, view3d)

        val animLengthSecs = animation.totalTime.seconds.toFloat()
        assertFunctionConformsTo(
            functionUnderTest = animator.adaptToPlotAssert(),
            visualisation = {
                row(animLengthSecs, "                   I                   I ")
                row(                "                 II                  II  ")
                row(                "               II                  II    ")
                row(                "             II                  II      ")
                row(                "          III                 III        ")
                row(                "        II                  II           ")
                row(                "      II                  II             ")
                row(                "    II                  II               ")
                row(                "  II                  II                 ")
                row(0.0f,           "XI                  XI                  I")
                xAxis {
                    markers(        "|                   |                   |")
                    values(         0.0f,         animLengthSecs,   2.0f*animLengthSecs)
                }
        })
    }

    private fun Animator3D.adaptToPlotAssert(): (Float) -> Float {
        var previousTime = 0.0f

        return { time: Float ->
            update((time - previousTime).seconds)
            previousTime = time
            elapsedTimeInAnimation.seconds.toFloat()
        }
    }
}
