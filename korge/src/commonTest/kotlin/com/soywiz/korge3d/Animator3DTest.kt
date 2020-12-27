package com.soywiz.korge3d

import com.soywiz.kds.*
import com.soywiz.klock.seconds
import com.soywiz.korge3d.animation.*
import com.soywiz.korge3d.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.interpolation.Easing
import it.krzeminski.visassert.assertFunctionConformsTo
import kotlin.test.*

@Korge3DExperimental
class Animator3DTest {
    @Test
    fun updateWithDefaultPlaybackPattern() = suspendTestNoBrowser {
        val library = resourcesVfs["skinning.dae"].readColladaLibrary()
        val animation = library.animationDefs["Armature_Bone_pose_matrix"]
            ?: throw RuntimeException("Animation not found")
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

    @Test
    fun updateWithCustomPlaybackPattern() = suspendTestNoBrowser {
        val library = resourcesVfs["skinning.dae"].readColladaLibrary()
        val animation = library.animationDefs["Armature_Bone_pose_matrix"]
            ?: throw RuntimeException("Animation not found")
        val smoothBackAndForth: (Double) -> Double = { t ->
            val timeModulo2Periods = t % 2.0
            if (timeModulo2Periods < 1.0) {
                Easing.SMOOTH(timeModulo2Periods)
            } else {
                Easing.SMOOTH(2.0 - timeModulo2Periods)
            }
        }
        val animator = Animator3D(animation, view3d, smoothBackAndForth)

        val animLengthSecs = animation.totalTime.seconds.toFloat()
        assertFunctionConformsTo(
            functionUnderTest = animator.adaptToPlotAssert(),
            visualisation = {
                row(animLengthSecs, "                  IIIII                                   III")
                row(                "               III     III                             III   ")
                row(                "              I           I                           I      ")
                row(                "            II             II                       II       ")
                row(                "           I                 I                     I         ")
                row(                "         II                   II                 II          ")
                row(                "       II                       II             II            ")
                row(                "      I                           I           I              ")
                row(                "   III                             III     III               ")
                row(0.0f,           "XII                                   IIIII                  ")
                xAxis {
                    markers(        "|                   |                   |                   |")
                    values(         0.0f,         animLengthSecs,   2.0f*animLengthSecs, 3.0f*animLengthSecs)
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

    private val view3d = object : View3D() {
        override fun render(ctx: RenderContext3D) {
            throw RuntimeException("Not relevant - shouldn't be called in this unit test")
        }
    }
}
