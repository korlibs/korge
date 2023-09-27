package korlibs.audio.internal

import korlibs.math.clamp

object SampleConvert {
    // (value.clamp(-1f, +1f) * Short.MAX_VALUE).toShort()
    fun floatToShort(v: Float): Short = (v * Short.MAX_VALUE.toDouble()).toInt().clamp(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    fun shortToFloat(v: Short): Float = (v.toDouble() / Short.MAX_VALUE).toFloat()
}
