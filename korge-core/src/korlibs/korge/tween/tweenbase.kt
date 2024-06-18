package korlibs.korge.tween

import korlibs.image.color.*
import korlibs.korge.internal.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.geom.bezier.*
import korlibs.math.geom.vector.*
import korlibs.math.interpolation.*
import korlibs.time.*
import kotlin.jvm.*
import kotlin.reflect.*
import kotlin.time.*

@Suppress("UNCHECKED_CAST")
data class V2<V>(
    val key: KMutableProperty0<V>,
    var initial: V,
    val end: V,
    val interpolator: (Ratio, V, V) -> V,
    val includeStart: Boolean,
    val fastStartTime: FastDuration = FastDuration.ZERO,
    val fastDuration: FastDuration = FastDuration.NaN,
    private val initialization: (() -> Unit)? = null,
) {
    constructor(
        key: KMutableProperty0<V>,
        initial: V,
        end: V,
        interpolator: (Ratio, V, V) -> V,
        includeStart: Boolean,
        startTime: Duration,
        duration: Duration,
        initialization: (() -> Unit)? = null,
    ) : this(key, initial, end, interpolator, includeStart, startTime.fast, duration.fast, initialization)

    val startTime: Duration get() = fastStartTime.toDuration()
    val duration: Duration get() = fastDuration.toDuration()

    val fastEndTime = fastStartTime + if (fastDuration == FastDuration.NaN) 0.fastNanoseconds else fastDuration
    val endTime get() = fastEndTime.slow

    @Deprecated("", ReplaceWith("getDuration(duration)"), level = DeprecationLevel.HIDDEN)
    fun duration(default: Duration): Duration = getDuration(default)

    fun getDuration(default: Duration): Duration = if (duration.isNil) default else duration
    fun endTime(default: Duration): Duration = if (duration.isNil) default else endTime

    fun getDuration(default: FastDuration): FastDuration = if (fastDuration == FastDuration.NaN) default else fastDuration
    fun endTime(default: FastDuration): FastDuration = if (fastDuration == FastDuration.NaN) default else fastEndTime

    fun init(): Unit {
        if (!includeStart) {
            initial = key.get()
            //includeStart = true
        }
        initialization?.invoke()
    }

    //private fun ensureInit() {
    //    if (!includeStart) {
    //        initial = key.get()
    //        includeStart = true
    //    }
    //}
    fun set(ratio: Ratio): Unit {
        //ensureInit()
        key.set(interpolator(ratio, initial, end))
    }

    fun get(): V {
        return key.get()
    }

    override fun toString(): String =
        "V2(key=${key.name}, range=[$initial-$end], startTime=$startTime, duration=$duration)"
}

private object V2CallbackSupport {
    var dummy: Unit = Unit
}

private class V2CallbackTSupport<T>(var dummy: T)

fun V2Callback(init: () -> Unit = {}, callback: (Ratio) -> Unit): V2<Unit> =
    V2(V2CallbackSupport::dummy, Unit, Unit, { ratio, _, _ -> callback(ratio) }, true, initialization = init)

fun <T> V2CallbackT(initial: T, init: () -> Unit = {}, callback: (Ratio) -> T): V2<T> {
    return V2(V2CallbackTSupport<T>(initial)::dummy, initial, initial, { ratio, _, _ -> callback(ratio) }, true, initialization = init)
}

fun V2Lazy(callback: () -> V2<*>): V2<Unit> {
    var value: V2<*>? = null
    return V2CallbackT(Unit) {
        if (value == null) value = callback()
        value!!.set(it)
    }
}

fun KMutableProperty0<Point>.incr(delta: Vector2D): V2<Point> {
    var start: Point = Point.ZERO
    val value: Point = Point.ZERO
    return V2(this, start, value, { it, _, _ ->
        Point(start.x + delta.x, start.y + delta.y)
    }, includeStart = false, initialization = {
        start = this.get()
    })
}

fun KMutableProperty0<MPoint>.incr(dx: Double, dy: Double): V2<MPoint> {
    val start: MPoint = MPoint(0, 0)
    val value: MPoint = MPoint(0, 0)
    return V2(this, start, value, { it, _, _ ->
        value.setTo(start.x + dx, start.y + dy)
        value
    }, includeStart = false, initialization = {
        start.copyFrom(this.get())
    })
}

fun KMutableProperty0<MPoint>.incr(dx: Number, dy: Number): V2<MPoint> = incr(dx.toDouble(), dy.toDouble())
fun KMutableProperty0<MPoint>.incr(incr: MPoint): V2<MPoint> = incr(incr.x, incr.y)

inline fun KMutableProperty0<Double>.incr(incr: Number): V2<Double> = incr(incr.toDouble())
fun KMutableProperty0<Double>.incr(incr: Double): V2<Double> = V2(this, 0.0, 0.0, interpolator = { it, start, _ ->
    //println("INTERPOLATE: it=$it, start=$start, incr=$incr")
    it.interpolate(start, start + incr)
}, includeStart = false)

fun KMutableProperty0<Angle>.incr(incr: Angle): V2<Angle> = V2(this, Angle.ZERO, Angle.ZERO, interpolator = { it, start, _ ->
    it.interpolateAngleDenormalized(start, start + incr)
}, includeStart = false)

@JvmName("getInt")
operator fun KMutableProperty0<Int>.get(end: Int) = V2(this, this.get(), end, ::_interpolateInt, includeStart = false)

@JvmName("getInt")
operator fun KMutableProperty0<Int>.get(initial: Int, end: Int) = V2(this, initial, end, ::_interpolateInt, includeStart = true)

@JvmName("getMutableProperty")
operator fun <V : Interpolable<V>> KMutableProperty0<V>.get(end: V) = V2(this, this.get(), end, ::_interpolateInterpolable, includeStart = false)

@JvmName("getMutableProperty")
operator fun <V : Interpolable<V>> KMutableProperty0<V>.get(initial: V, end: V) = V2(this, initial, end, ::_interpolateInterpolable, includeStart = true)

@JvmName("getMutablePropertyPoint")
operator fun KMutableProperty0<Matrix>.get(end: Matrix) = V2(this, this.get(), end, ::_interpolateMatrix, includeStart = false)
@JvmName("getMutablePropertyPoint")
operator fun KMutableProperty0<Matrix>.get(initial: Matrix, end: Matrix) = V2(this, initial, end, ::_interpolateMatrix, includeStart = true)

@JvmName("getMutablePropertyPoint")
operator fun KMutableProperty0<Point>.get(end: Point) = V2(this, this.get(), end, ::_interpolatePoint, includeStart = false)
@JvmName("getMutablePropertyPoint")
operator fun KMutableProperty0<Point>.get(initial: Point, end: Point) = V2(this, initial, end, ::_interpolatePoint, includeStart = true)

@JvmName("getMutablePropertySize")
operator fun KMutableProperty0<Size>.get(end: Size) = V2(this, this.get(), end, ::_interpolateSize, includeStart = false)
@JvmName("getMutablePropertySize")
operator fun KMutableProperty0<Size>.get(initial: Size, end: Size) = V2(this, initial, end, ::_interpolateSize, includeStart = true)

@JvmName("getMutablePropertyScale")
operator fun KMutableProperty0<Scale>.get(end: Scale) = V2(this, this.get(), end, ::_interpolateScale, includeStart = false)
@JvmName("getMutablePropertyScale")
operator fun KMutableProperty0<Scale>.get(initial: Scale, end: Scale) = V2(this, initial, end, ::_interpolateScale, includeStart = true)

@PublishedApi
internal fun _interpolate(ratio: Ratio, l: Double, r: Double): Double = ratio.interpolate(l, r)
@PublishedApi
internal fun _interpolateInt(ratio: Ratio, l: Int, r: Int): Int = ratio.interpolate(l, r)
@PublishedApi
internal fun <V : Interpolable<V>> _interpolateInterpolable(ratio: Ratio, l: V, r: V): V = ratio.interpolate(l, r)
@PublishedApi
internal fun _interpolateRatio(ratio: Ratio, l: Ratio, r: Ratio): Ratio = ratio.interpolate(l, r)
@PublishedApi
internal fun _interpolateMatrix(ratio: Ratio, l: Matrix, r: Matrix): Matrix = ratio.interpolate(l, r)
@PublishedApi
internal fun _interpolatePoint(ratio: Ratio, l: Point, r: Point): Point = ratio.interpolate(l, r)
@PublishedApi
internal fun _interpolateSize(ratio: Ratio, l: Size, r: Size): Size = ratio.interpolate(l, r)
@PublishedApi
internal fun _interpolateScale(ratio: Ratio, l: Scale, r: Scale): Scale = ratio.interpolate(l, r)
@PublishedApi
internal fun _interpolateFloat(ratio: Ratio, l: Float, r: Float): Float = ratio.interpolate(l, r)
@PublishedApi
internal fun _interpolateColor(ratio: Ratio, l: RGBA, r: RGBA): RGBA = RGBA.mixRgba(l, r, ratio)
@KorgeInternal
fun _interpolateAngle(ratio: Ratio, l: Angle, r: Angle): Angle = ratio.interpolateAngleNormalized(l, r)
@PublishedApi
internal fun _interpolateAngleDenormalized(ratio: Ratio, l: Angle, r: Angle): Angle = ratio.interpolateAngleDenormalized(l, r)
@PublishedApi
internal fun _interpolateDuration(ratio: Ratio, l: Duration, r: Duration): Duration = _interpolate(ratio, l.milliseconds, r.milliseconds).milliseconds
@PublishedApi
internal fun _interpolateFastDuration(ratio: Ratio, l: FastDuration, r: FastDuration): FastDuration = _interpolate(ratio, l.milliseconds, r.milliseconds).fastMilliseconds
@PublishedApi
internal fun _interpolateColorAdd(ratio: Ratio, l: ColorAdd, r: ColorAdd): ColorAdd = ColorAdd(
    ratio.interpolate(l.r, r.r),
    ratio.interpolate(l.g, r.g),
    ratio.interpolate(l.b, r.b),
    ratio.interpolate(l.a, r.a)
)

//inline operator fun KMutableProperty0<Float>.get(end: Number) = V2(this, this.get(), end.toFloat(), ::_interpolateFloat)
//inline operator fun KMutableProperty0<Float>.get(initial: Number, end: Number) =
//	V2(this, initial.toFloat(), end.toFloat(), ::_interpolateFloat)

@JvmName("getPoint")
inline operator fun KMutableProperty0<Point>.get(path: VectorPath, includeLastPoint: Boolean = path.isLastCommandClose, reversed: Boolean = false): V2<Point> =
    this[path.getCurves().getEquidistantPoints().also {
        //println("points.lastX=${points.lastX}, points.firstX=${points.firstX}")
        //println("points.lastY=${points.lastY}, points.firstY=${points.firstY}")
        if (!includeLastPoint && it.last.isAlmostEquals(it.first)) {
            (it as PointArrayList).removeAt(it.size - 1)
            //println("REMOVED LAST POINT!")
        }
        if (reversed) {
            (it as PointArrayList).reverse()
        }
    }]

@JvmName("getIPoint")
@Deprecated("")
inline operator fun KMutableProperty0<MPoint>.get(
    path: VectorPath,
    includeLastPoint: Boolean = path.isLastCommandClose,
    reversed: Boolean = false
): V2<MPoint> = this[path.getCurves().getEquidistantPoints().also {
    //println("points.lastX=${points.lastX}, points.firstX=${points.firstX}")
    //println("points.lastY=${points.lastY}, points.firstY=${points.firstY}")
    if (!includeLastPoint && it.last.isAlmostEquals(it.first)) {
        (it as PointArrayList).removeAt(it.size - 1)
        //println("REMOVED LAST POINT!")
    }
    if (reversed) {
        (it as PointArrayList).reverse()
    }
}]

@JvmName("getIPoint")
@Deprecated("")
inline operator fun KMutableProperty0<MPoint>.get(range: PointList): V2<MPoint> {
    val temp = MPoint()
    return V2(
        this, temp, temp, { ratio, _, _ ->
            val ratioIndex = ratio.toFloat() * (range.size - 1)
            val index = ratioIndex.toIntFloor()
            val index1 = (index + 1).coerceAtMost(range.size)
            val sratio = fract(ratioIndex)
            temp.setTo(sratio.toRatio().interpolate(range[index], range[index1]))
        }, includeStart = false
    )
}

@JvmName("getPoint")
inline operator fun KMutableProperty0<Point>.get(range: PointList): V2<Point> {
    val p = Point()
    return V2(
        this, p, p, { ratio, _, _ ->
            val ratioIndex = ratio.toFloat() * (range.size - 1)
            val index = ratioIndex.toIntFloor()
            val index1 = (index + 1).coerceAtMost(range.size)
            val sratio = fract(ratioIndex).toRatio()
            sratio.interpolate(range[index], range[index1])
        }, includeStart = false
    )
}

@JvmName("getFloat")
inline operator fun KMutableProperty0<Float>.get(end: Float) = V2(this, this.get(), end, ::_interpolateFloat, includeStart = false)
@JvmName("getFloat")
inline operator fun KMutableProperty0<Float>.get(end: Int) = get(end.toFloat())
@JvmName("getFloat")
inline operator fun KMutableProperty0<Float>.get(end: Double) = get(end.toFloat())
@JvmName("getFloat")
inline operator fun KMutableProperty0<Float>.get(end: Long) = get(end.toFloat())
@JvmName("getFloat")
inline operator fun KMutableProperty0<Float>.get(end: Number) = get(end.toFloat())
@JvmName("getFloat")
inline operator fun KMutableProperty0<Float>.get(initial: Float, end: Float) = V2(this, initial, end, ::_interpolateFloat, true)
@JvmName("getFloat")
inline operator fun KMutableProperty0<Float>.get(initial: Int, end: Int) = get(initial.toFloat(), end.toFloat())
@JvmName("getFloat")
inline operator fun KMutableProperty0<Float>.get(initial: Double, end: Double) = get(initial.toFloat(), end.toFloat())
@JvmName("getFloat")
inline operator fun KMutableProperty0<Float>.get(initial: Long, end: Long) = get(initial.toFloat(), end.toFloat())
@JvmName("getFloat")
inline operator fun KMutableProperty0<Float>.get(initial: Number, end: Number) = get(initial.toFloat(), end.toFloat())

inline operator fun KMutableProperty0<Ratio>.get(end: Ratio) = V2(this, this.get(), end, ::_interpolateRatio, includeStart = false)
inline operator fun KMutableProperty0<Ratio>.get(initial: Ratio, end: Ratio) = V2(this, initial, end, ::_interpolateRatio, true)

inline operator fun KMutableProperty0<Double>.get(end: Double) = V2(this, this.get(), end, ::_interpolate, includeStart = false)
inline operator fun KMutableProperty0<Double>.get(end: Int) = get(end.toDouble())
inline operator fun KMutableProperty0<Double>.get(end: Float) = get(end.toDouble())
inline operator fun KMutableProperty0<Double>.get(end: Long) = get(end.toDouble())
inline operator fun KMutableProperty0<Double>.get(end: Number) = get(end.toDouble())
inline operator fun KMutableProperty0<Double>.get(initial: Double, end: Double) = V2(this, initial, end, ::_interpolate, true)
inline operator fun KMutableProperty0<Double>.get(initial: Int, end: Int) = get(initial.toDouble(), end.toDouble())
inline operator fun KMutableProperty0<Double>.get(initial: Float, end: Float) = get(initial.toDouble(), end.toDouble())
inline operator fun KMutableProperty0<Double>.get(initial: Long, end: Long) = get(initial.toDouble(), end.toDouble())
inline operator fun KMutableProperty0<Double>.get(initial: Number, end: Number) = get(initial.toDouble(), end.toDouble())

inline operator fun KMutableProperty0<RGBA>.get(end: RGBA) = V2(this, this.get(), end, ::_interpolateColor, includeStart = false)
inline operator fun KMutableProperty0<RGBA>.get(initial: RGBA, end: RGBA) = V2(this, initial, end, ::_interpolateColor, includeStart = true)

inline operator fun KMutableProperty0<ColorAdd>.get(end: ColorAdd) = V2(this, this.get(), end, ::_interpolateColorAdd, includeStart = false)
inline operator fun KMutableProperty0<ColorAdd>.get(initial: ColorAdd, end: ColorAdd) = V2(this, initial, end, ::_interpolateColorAdd, includeStart = true)

inline operator fun KMutableProperty0<Angle>.get(end: Angle) = V2(this, this.get(), end, ::_interpolateAngle, includeStart = false)
inline operator fun KMutableProperty0<Angle>.get(initial: Angle, end: Angle) = V2(this, initial, end, ::_interpolateAngle, includeStart = true)

fun V2<Angle>.denormalized(): V2<Angle> = this.copy(interpolator = ::_interpolateAngleDenormalized)

inline operator fun KMutableProperty0<Duration>.get(end: Duration) = V2(this, this.get(), end, ::_interpolateDuration, includeStart = false)
inline operator fun KMutableProperty0<Duration>.get(initial: Duration, end: Duration) = V2(this, initial, end, ::_interpolateDuration, includeStart = true)

inline operator fun KMutableProperty0<FastDuration>.get(end: FastDuration) = V2(this, this.get(), end, ::_interpolateFastDuration, includeStart = false)
inline operator fun KMutableProperty0<FastDuration>.get(initial: FastDuration, end: FastDuration) = V2(this, initial, end, ::_interpolateFastDuration, includeStart = true)

fun <V> V2<V>.clamped(): V2<V> = copy(interpolator = { ratio, l, r -> this.interpolator(ratio.clamped, l, r) })
fun <V> V2<V>.easing(easing: Easing): V2<V> = this.copy(interpolator = { ratio, a, b -> this.interpolator(easing(ratio.toDouble()).toRatio(), a, b) })

inline fun <V> V2<V>.delay(startTime: Duration) = delay(startTime.fast)
inline fun <V> V2<V>.duration(duration: Duration) = duration(duration.fast)

inline fun <V> V2<V>.delay(startTime: FastDuration) = this.copy(fastStartTime = startTime)
inline fun <V> V2<V>.duration(duration: FastDuration) = this.copy(fastDuration = duration)

inline fun <V> V2<V>.linear() = this
inline fun <V> V2<V>.smooth() = this.easing(Easing.SMOOTH)

inline fun <V> V2<V>.ease() = this.easing(Easing.EASE)
inline fun <V> V2<V>.easeIn() = this.easing(Easing.EASE_IN)
inline fun <V> V2<V>.easeOut() = this.easing(Easing.EASE_OUT)
inline fun <V> V2<V>.easeInOut() = this.easing(Easing.EASE_IN_OUT)

inline fun <V> V2<V>.easeInOld() = this.easing(Easing.EASE_IN_OLD)
inline fun <V> V2<V>.easeOutOld() = this.easing(Easing.EASE_OUT_OLD)
inline fun <V> V2<V>.easeInOutOld() = this.easing(Easing.EASE_IN_OUT_OLD)
inline fun <V> V2<V>.easeOutInOld() = this.easing(Easing.EASE_OUT_IN_OLD)

inline fun <V> V2<V>.easeInBack() = this.easing(Easing.EASE_IN_BACK)
inline fun <V> V2<V>.easeOutBack() = this.easing(Easing.EASE_OUT_BACK)
inline fun <V> V2<V>.easeInOutBack() = this.easing(Easing.EASE_IN_OUT_BACK)
inline fun <V> V2<V>.easeOutInBack() = this.easing(Easing.EASE_OUT_IN_BACK)

inline fun <V> V2<V>.easeInElastic() = this.easing(Easing.EASE_IN_ELASTIC)
inline fun <V> V2<V>.easeOutElastic() = this.easing(Easing.EASE_OUT_ELASTIC)
inline fun <V> V2<V>.easeInOutElastic() = this.easing(Easing.EASE_IN_OUT_ELASTIC)
inline fun <V> V2<V>.easeOutInElastic() = this.easing(Easing.EASE_OUT_IN_ELASTIC)

inline fun <V> V2<V>.easeInBounce() = this.easing(Easing.EASE_IN_BOUNCE)
inline fun <V> V2<V>.easeOutBounce() = this.easing(Easing.EASE_OUT_BOUNCE)
inline fun <V> V2<V>.easeInOutBounce() = this.easing(Easing.EASE_IN_OUT_BOUNCE)
inline fun <V> V2<V>.easeOutInBounce() = this.easing(Easing.EASE_OUT_IN_BOUNCE)

inline fun <V> V2<V>.easeInQuad() = this.easing(Easing.EASE_IN_QUAD)
inline fun <V> V2<V>.easeOutQuad() = this.easing(Easing.EASE_OUT_QUAD)
inline fun <V> V2<V>.easeInOutQuad() = this.easing(Easing.EASE_IN_OUT_QUAD)

inline fun <V> V2<V>.easeSine() = this.easing(Easing.EASE_SINE)

inline fun <V> V2<V>.easeClampStart() = this.easing(Easing.EASE_CLAMP_START)
inline fun <V> V2<V>.easeClampEnd() = this.easing(Easing.EASE_CLAMP_END)
inline fun <V> V2<V>.easeClampMiddle() = this.easing(Easing.EASE_CLAMP_MIDDLE)
