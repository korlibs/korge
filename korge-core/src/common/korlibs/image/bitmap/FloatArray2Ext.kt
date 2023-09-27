package korlibs.image.bitmap

import korlibs.datastructure.*
import korlibs.image.color.*
import korlibs.math.*
import kotlin.math.*

inline fun FloatArray2.forEachValue(block: (x: Int, y: Int, v: Float) -> Unit) {
    for (y in 0 until height) {
        for (x in 0 until width) {
            block(x, y, this[x, y])
        }
    }
}

fun FloatArray2.getMinMax(out: FloatArray = FloatArray(2)): FloatArray {
    var min = Float.POSITIVE_INFINITY
    var max = Float.NEGATIVE_INFINITY
    forEachValue { _, _, c ->
        min = min(min, c)
        max = max(max, c)
    }
    out[0] = min
    out[1] = max
    return out
}

fun FloatArray2.convertRange(minSrc: Float, maxSrc: Float, minDst: Float = 0f, maxDst: Float = 1f): Unit {
    updateValues { x, y, value ->
        value.convertRange(minSrc, maxSrc, minDst, maxDst)
    }
}

fun FloatArray2.normalize(): Unit {
    val (min, max) = getMinMax()
    convertRange(min, max, 0f, 1f)
}

fun FloatArray2.normalizeUniform() {
    val (min, max) = getMinMax()
    val range = max(min.absoluteValue, max.absoluteValue)
    convertRange(-range, +range)
}

fun FloatArray2.scale(scale: Float = 1f): Unit {
    forEachValue { x, y, c ->
        set(x, y, c * scale)
    }
}

fun FloatArray2.clamp(min: Float, max: Float): Unit {
    updateValues { _, _, value ->
        value.clamp(min, max)
    }
}

inline fun FloatArray2.updateValues(block: (x: Int, y: Int, value: Float) -> Float): Unit {
    forEachValue { x, y, v ->
        set(x, y, block(x, y, v))
    }
}

fun FloatArray2.toBMP32(block: (Float) -> RGBA): Bitmap32 {
    val out = Bitmap32(width, height)
    forEachValue { x, y, v ->
        out[x, y] = block(v)
    }
    return out
}
