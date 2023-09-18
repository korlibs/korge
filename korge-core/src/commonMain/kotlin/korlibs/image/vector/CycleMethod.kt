package korlibs.image.vector

import korlibs.math.*
import korlibs.memory.*
import korlibs.math.geom.*

inline class CycleMethodPair(val data: Int) {
    constructor(x: CycleMethod, y: CycleMethod) : this(0.insert8(x.ordinal, 0).insert8(y.ordinal, 8))
    val x: CycleMethod get() = CycleMethod.VALUES_BY_ORDINAL[data.extract8(0)]
    val y: CycleMethod get() = CycleMethod.VALUES_BY_ORDINAL[data.extract8(8)]

    fun apply(ratio: Point): Point = Point(this.x.apply(ratio.x), this.y.apply(ratio.y))
    fun apply(ratio: Anchor): Anchor = Anchor(this.x.apply(ratio.sx), this.y.apply(ratio.sy))
}

enum class CycleMethod {
    NO_CYCLE, NO_CYCLE_CLAMP, REPEAT, REFLECT;

//enum class CycleMethod(val value: Int) {
    companion object {
        val VALUES_BY_ORDINAL = values()

        //val NO_CYCLE = CycleMethod(0)
        //val NO_CYCLE_CLAMP = CycleMethod(1)
        //val REPEAT = CycleMethod(2)
        //val REFLECT = CycleMethod(3)
        //val ALL = arrayOf(NO_CYCLE, NO_CYCLE_CLAMP, REPEAT, REFLECT)
        //fun values() = ALL

        fun fromRepeat(repeat: Boolean) = if (repeat) REPEAT else NO_CYCLE
    }

    //override fun toString(): String = when (this) {
    //    NO_CYCLE -> "NO_CYCLE"
    //    NO_CYCLE_CLAMP -> "NO_CYCLE_CLAMP"
    //    REPEAT -> "REPEAT"
    //    REFLECT -> "REFLECT"
    //    else -> "UNKNOWN($value)"
    //}

    val repeating: Boolean get() = this != NO_CYCLE && this != NO_CYCLE_CLAMP

    fun apply(ratio: Float): Float = when (this) {
        NO_CYCLE -> ratio
        NO_CYCLE_CLAMP -> ratio.clamp01()
        REPEAT -> fract(ratio)
        REFLECT -> {
            val part = ratio umod 2f
            if (part > 1f) 2f - part else part
        }
    }

    fun apply(ratio: Double): Double = apply(ratio.toFloat()).toDouble()

    fun apply(value: Double, size: Double): Double = apply(value / size) * size
    fun apply(value: Double, min: Double, max: Double): Double = apply(value - min, max - min) + min

    fun apply(value: Float, size: Float): Float = apply(value / size) * size
    fun apply(value: Float, min: Float, max: Float): Float = apply(value - min, max - min) + min
}
