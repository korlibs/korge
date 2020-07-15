package com.soywiz.korau.sound

object SoundUtils {
	fun convertS16ToF32(channels: Int, input: ShortArray, leftVolume: Int, rightVolume: Int): FloatArray {
		val output = FloatArray(input.size * 2 / channels)
		val optimized = leftVolume == 1 && rightVolume == 1
		when (channels) {
			2 ->
				if (optimized) {
					for (n in 0 until output.size) output[n] = (input[n] / 32767.0).toFloat()
				} else {
					for (n in 0 until output.size step 2) {
						output[n + 0] = ((input[n + 0] / 32767.0) * leftVolume).toFloat()
						output[n + 1] = ((input[n + 1] / 32767.0) * rightVolume).toFloat()
					}
				}
			1 ->
				if (optimized) {
					var m = 0
					for (n in 0 until input.size) {
						val v = (input[n] / 32767.0).toFloat()
						output[m++] = v
						output[m++] = v
					}
				} else {
					var m = 0
					for (n in 0 until input.size) {
						val sample = (input[n] / 32767.0).toFloat()
						output[m++] = sample * leftVolume
						output[m++] = sample * rightVolume
					}
				}
		}
		return output
	}
}
