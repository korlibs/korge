package korlibs.math

////////////////////
////////////////////

/** Converts this value considering it was in the range [srcMin]..[srcMax] into [dstMin]..[dstMax], if the value is not inside the range the output value will be outside the destination range */
fun Float.convertRange(srcMin: Float, srcMax: Float, dstMin: Float, dstMax: Float): Float = (dstMin + (dstMax - dstMin) * ((this - srcMin) / (srcMax - srcMin)))
/** Converts this value considering it was in the range [srcMin]..[srcMax] into [dstMin]..[dstMax], if the value is not inside the range the output value will be outside the destination range */
fun Double.convertRange(srcMin: Double, srcMax: Double, dstMin: Double, dstMax: Double): Double = (dstMin + (dstMax - dstMin) * ((this - srcMin) / (srcMax - srcMin)))
//fun Double.convertRange(minSrc: Double, maxSrc: Double, minDst: Double, maxDst: Double): Double = (((this - minSrc) / (maxSrc - minSrc)) * (maxDst - minDst)) + minDst
/** Converts this value considering it was in the range [srcMin]..[srcMax] into [dstMin]..[dstMax], if the value is not inside the range the output value will be outside the destination range */
fun Int.convertRange(srcMin: Int, srcMax: Int, dstMin: Int, dstMax: Int): Int = (dstMin + (dstMax - dstMin) * ((this - srcMin).toDouble() / (srcMax - srcMin).toDouble())).toInt()
/** Converts this value considering it was in the range [srcMin]..[srcMax] into [dstMin]..[dstMax], if the value is not inside the range the output value will be outside the destination range */
fun Long.convertRange(srcMin: Long, srcMax: Long, dstMin: Long, dstMax: Long): Long = (dstMin + (dstMax - dstMin) * ((this - srcMin).toDouble() / (srcMax - srcMin).toDouble())).toLong()

/** Converts this value considering it was in the range [srcMin]..[srcMax] into [dstMin]..[dstMax], if the value is not inside the range the output value will be clamped to the nearest bound */
fun Float.convertRangeClamped(srcMin: Float, srcMax: Float, dstMin: Float, dstMax: Float): Float = convertRange(srcMin, srcMax, dstMin, dstMax).clamp(dstMin, dstMax)
/** Converts this value considering it was in the range [srcMin]..[srcMax] into [dstMin]..[dstMax], if the value is not inside the range the output value will be clamped to the nearest bound */
fun Double.convertRangeClamped(srcMin: Double, srcMax: Double, dstMin: Double, dstMax: Double): Double = convertRange(srcMin, srcMax, dstMin, dstMax).clamp(dstMin, dstMax)
/** Converts this value considering it was in the range [srcMin]..[srcMax] into [dstMin]..[dstMax], if the value is not inside the range the output value will be clamped to the nearest bound */
fun Int.convertRangeClamped(srcMin: Int, srcMax: Int, dstMin: Int, dstMax: Int): Int = convertRange(srcMin, srcMax, dstMin, dstMax).clamp(dstMin, dstMax)
/** Converts this value considering it was in the range [srcMin]..[srcMax] into [dstMin]..[dstMax], if the value is not inside the range the output value will be clamped to the nearest bound */
fun Long.convertRangeClamped(srcMin: Long, srcMax: Long, dstMin: Long, dstMax: Long): Long = convertRange(srcMin, srcMax, dstMin, dstMax).clamp(dstMin, dstMax)

