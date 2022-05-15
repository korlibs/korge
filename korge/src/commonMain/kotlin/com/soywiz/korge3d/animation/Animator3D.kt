package com.soywiz.korge3d.animation

import com.soywiz.kds.getCyclic
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.korge3d.Korge3DExperimental
import com.soywiz.korge3d.Library3D
import com.soywiz.korge3d.Transform3D
import com.soywiz.korge3d.View3D
import com.soywiz.korge3d.get
import com.soywiz.korma.geom.Matrix3D
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.interpolation.interpolate

/**
 * @param playbackPattern: A function that takes normalized time (from 0 to 1) as argument and returns normalized
 *                         animation progress (from 0 to 1). Allows defining complex behaviors of an object which range
 *                         of movement is defined by an animation, and concrete position is defined by game's logic.
 *                         Normalized time means that as the time flows, it's counted in the number of this animation's
 *                         periods. For example, if animation is 5 seconds long, after 5 seconds this argument will be
 *                         equal to 1, and after 7.5 seconds - 1.5.
 *                         Normalized progress means that the function specifies what frame of the animation should be
 *                         shown in terms of its defined timeframe. For example, if the function returns 0.25 and the
 *                         animation is 10 seconds long, it means that the engine should present the animation as if it
 *                         was at 0.25 * 10 = 2.5 seconds of its regular playback.
 *                         Thanks to this, it's possible to define normalized, reusable playback patterns, not tied to
 *                         any specific animation's length.
 */
@Korge3DExperimental
class Animator3D(val animation: Animation3D, val rootView: View3D, var playbackPattern: (Double) -> Double = repeatInfinitely) {
    companion object {
        val repeatInfinitely: (Double) -> Double = { it % 1.0 }
    }

	var currentTime = 0.milliseconds

    val elapsedTimeInAnimation: TimeSpan
    get() = animation.totalTime * playbackPattern(currentTime/animation.totalTime)

	fun update(dt: TimeSpan) {
		//currentTime += ms.ms * 0.1
		currentTime += dt
		val keyFrames = animation.keyFrames
		val fseconds = keyFrames.seconds
		val ftransforms = keyFrames.transforms
		val ffloats = keyFrames.floats
		val aproperty = animation.property
		//genericBinarySearch(0, animation.keys.size) { animation.keys[it] }

		val n = keyFrames.findIndex(elapsedTimeInAnimation)
		if (n < 0) return

		val startTime = fseconds[n].toDouble().seconds
		val endTime = fseconds.getOrNull(n + 1)?.let { it.toDouble().seconds } ?: startTime
		val fragmentTime = (endTime - startTime)
		if (fragmentTime <= 0.milliseconds) return

		val ratio = (elapsedTimeInAnimation - startTime) / fragmentTime
		val aview = rootView[animation.target]
		//println("ratio: $ratio, startTime=$startTime, endTime=$endTime, elapsedTimeInAnimation=$elapsedTimeInAnimation")
		if (aview != null) {
			when (aproperty) {
				"transform" -> {
					if (ftransforms != null) {
						if (n >= ftransforms.size) {
							error("Unexpected")
						}
						aview.transform.setToInterpolated(
							ftransforms[n],
							ftransforms.getCyclic(n + 1),
							ratio.toDouble()
						)
					}
				}
				"location.X", "location.Y", "location.Z", "scale.X", "scale.Y", "scale.Z", "rotationX.ANGLE", "rotationY.ANGLE", "rotationZ.ANGLE" -> {
					if (ffloats != null) {
						val value = ratio.interpolate(ffloats[n], ffloats[n % ffloats.size]).toDouble()
						when (aproperty) {
							"location.X" -> aview.x = value
							"location.Y" -> aview.y = value
							"location.Z" -> aview.z = value
							"scale.X" -> aview.scaleX = value
							"scale.Y" -> aview.scaleY = value
							"scale.Z" -> aview.scaleZ = value
							"rotationX.ANGLE" -> aview.rotationX = value.degrees
							"rotationY.ANGLE" -> aview.rotationY = value.degrees
							"rotationZ.ANGLE" -> aview.rotationZ = value.degrees
						}
					}
				}
				else -> {
					println("WARNING: animation.property=${animation.property} not implemented")
				}
			}
		}

		//animation.keyFrames.binarySearch { it.time.millisecondsInt }
		//println(animation)
	}
}

@Korge3DExperimental
data class Animation3D(val id: String, val target: String, val property: String, val keyFrames: Frames) :
	Library3D.Def() {
	val totalTime = keyFrames.totalTime

	class Frames(
		var seconds: FloatArray = floatArrayOf(),
		var interpolations: Array<String> = arrayOf(),
		var floats: FloatArray? = null,
		var matrices: Array<Matrix3D>? = null
	) {
		val transforms = matrices?.map { Transform3D().setMatrix(it) }?.toTypedArray()
		val totalFrames = seconds.size
		val totalTime = seconds.maxOrNull()?.let { it.toDouble().seconds } ?: 0.seconds

		// @TODO: Binary Search
		fun findIndex(time: TimeSpan): Int {
			val elapsedSeconds = time.seconds
			for (n in 0 until totalFrames - 1) {
				if (elapsedSeconds >= seconds[n] && elapsedSeconds < seconds[n + 1]) {
					return n
				}
			}
			return totalFrames - 1
		}
	}

}
