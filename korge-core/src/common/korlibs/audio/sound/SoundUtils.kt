package korlibs.audio.sound

object SoundUtils {
	fun convertS16ToF32(channels: Int, input: ShortArray, leftVolume: Float, rightVolume: Float): FloatArray {
		val output = FloatArray(input.size * 2 / channels)
		val optimized = leftVolume == 1f && rightVolume == 1f
		when (channels) {
			2 ->
				if (optimized) {
					for (n in 0 until output.size) output[n] = (input[n] / 32767f)
				} else {
					for (n in 0 until output.size step 2) {
						output[n + 0] = ((input[n + 0] / 32767f) * leftVolume)
						output[n + 1] = ((input[n + 1] / 32767f) * rightVolume)
					}
				}
			1 ->
				if (optimized) {
					var m = 0
					for (n in 0 until input.size) {
						val v = (input[n] / 32767f)
						output[m++] = v
						output[m++] = v
					}
				} else {
					var m = 0
					for (n in 0 until input.size) {
						val sample = (input[n] / 32767f)
						output[m++] = sample * leftVolume
						output[m++] = sample * rightVolume
					}
				}
		}
		return output
	}
}
