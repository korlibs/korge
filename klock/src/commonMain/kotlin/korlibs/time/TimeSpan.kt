package korlibs.time

import korlibs.time.internal.*
import kotlin.jvm.*
import kotlin.math.*
import kotlin.time.*

/** [TimeSpan] representing this number as [nanoseconds] or 1 / 1_000_000_000 [seconds]. */
inline val Long.nanoseconds get() = Duration.fromNanoseconds(this.toDouble())
/** [TimeSpan] representing this number as [microseconds] or 1 / 1_000_000 [seconds]. */
inline val Long.microseconds get() = Duration.fromMicroseconds(this.toDouble())
/** [TimeSpan] representing this number as [milliseconds] or 1 / 1_000 [seconds]. */
inline val Long.milliseconds get() = Duration.fromMilliseconds(this.toDouble())
/** [TimeSpan] representing this number as [seconds]. */
inline val Long.seconds get() = Duration.fromSeconds((this.toDouble()))
/** [TimeSpan] representing this number as [minutes] or 60 [seconds]. */
inline val Long.minutes get() = Duration.fromMinutes(this.toDouble())
/** [TimeSpan] representing this number as [hours] or 3_600 [seconds]. */
inline val Long.hours get() = Duration.fromHours(this.toDouble())
/** [TimeSpan] representing this number as [days] or 86_400 [seconds]. */
inline val Long.days get() = Duration.fromDays(this.toDouble())
/** [TimeSpan] representing this number as [weeks] or 604_800 [seconds]. */
inline val Long.weeks get() = Duration.fromWeeks(this.toDouble())

/** [TimeSpan] representing this number as [nanoseconds] or 1 / 1_000_000_000 [seconds]. */
inline val Float.nanoseconds get() = Duration.fromNanoseconds(this.toDouble())
/** [TimeSpan] representing this number as [microseconds] or 1 / 1_000_000 [seconds]. */
inline val Float.microseconds get() = Duration.fromMicroseconds(this.toDouble())
/** [TimeSpan] representing this number as [milliseconds] or 1 / 1_000 [seconds]. */
inline val Float.milliseconds get() = Duration.fromMilliseconds(this.toDouble())
/** [TimeSpan] representing this number as [seconds]. */
inline val Float.seconds get() = Duration.fromSeconds((this.toDouble()))
/** [TimeSpan] representing this number as [minutes] or 60 [seconds]. */
inline val Float.minutes get() = Duration.fromMinutes(this.toDouble())
/** [TimeSpan] representing this number as [hours] or 3_600 [seconds]. */
inline val Float.hours get() = Duration.fromHours(this.toDouble())
/** [TimeSpan] representing this number as [days] or 86_400 [seconds]. */
inline val Float.days get() = Duration.fromDays(this.toDouble())
/** [TimeSpan] representing this number as [weeks] or 604_800 [seconds]. */
inline val Float.weeks get() = Duration.fromWeeks(this.toDouble())

/** [TimeSpan] representing this number as [nanoseconds] or 1 / 1_000_000_000 [seconds]. */
inline val Int.nanoseconds get() = Duration.fromNanoseconds(this.toDouble())
/** [TimeSpan] representing this number as [microseconds] or 1 / 1_000_000 [seconds]. */
inline val Int.microseconds get() = Duration.fromMicroseconds(this.toDouble())
/** [TimeSpan] representing this number as [milliseconds] or 1 / 1_000 [seconds]. */
inline val Int.milliseconds get() = Duration.fromMilliseconds(this.toDouble())
/** [TimeSpan] representing this number as [seconds]. */
inline val Int.seconds get() = Duration.fromSeconds((this.toDouble()))
/** [TimeSpan] representing this number as [minutes] or 60 [seconds]. */
inline val Int.minutes get() = Duration.fromMinutes(this.toDouble())
/** [TimeSpan] representing this number as [hours] or 3_600 [seconds]. */
inline val Int.hours get() = Duration.fromHours(this.toDouble())
/** [TimeSpan] representing this number as [days] or 86_400 [seconds]. */
inline val Int.days get() = Duration.fromDays(this.toDouble())
/** [TimeSpan] representing this number as [weeks] or 604_800 [seconds]. */
inline val Int.weeks get() = Duration.fromWeeks(this.toDouble())

/** [TimeSpan] representing this number as [nanoseconds] or 1 / 1_000_000_000 [seconds]. */
inline val Double.nanoseconds get() = Duration.fromNanoseconds(this)
/** [TimeSpan] representing this number as [microseconds] or 1 / 1_000_000 [seconds]. */
inline val Double.microseconds get() = Duration.fromMicroseconds(this)
/** [TimeSpan] representing this number as [milliseconds] or 1 / 1_000 [seconds]. */
inline val Double.milliseconds get() = Duration.fromMilliseconds(this)
/** [TimeSpan] representing this number as [seconds]. */
inline val Double.seconds get() = Duration.fromSeconds((this))
/** [TimeSpan] representing this number as [minutes] or 60 [seconds]. */
inline val Double.minutes get() = Duration.fromMinutes(this)
/** [TimeSpan] representing this number as [hours] or 3_600 [seconds]. */
inline val Double.hours get() = Duration.fromHours(this)
/** [TimeSpan] representing this number as [days] or 86_400 [seconds]. */
inline val Double.days get() = Duration.fromDays(this)
/** [TimeSpan] representing this number as [weeks] or 604_800 [seconds]. */
inline val Double.weeks get() = Duration.fromWeeks(this)

typealias TimeSpan = kotlin.time.Duration

fun TimeSpan(milliseconds: Double): TimeSpan = milliseconds.toDuration(DurationUnit.MILLISECONDS)

val Duration.milliseconds: Double get() = this.toDouble(DurationUnit.MILLISECONDS)

/** Returns the total number of [nanoseconds] for this [TimeSpan] (1 / 1_000_000_000 [seconds]) */
val Duration.nanoseconds: Double get() = this.milliseconds / MILLIS_PER_NANOSECOND
/** Returns the total number of [nanoseconds] for this [TimeSpan] (1 / 1_000_000_000 [seconds]) as Integer */
val Duration.nanosecondsInt: Int get() = (this.milliseconds / MILLIS_PER_NANOSECOND).toInt()

/** Returns the total number of [microseconds] for this [TimeSpan] (1 / 1_000_000 [seconds]) */
val Duration.microseconds: Double get() = this.milliseconds / MILLIS_PER_MICROSECOND
/** Returns the total number of [microseconds] for this [TimeSpan] (1 / 1_000_000 [seconds]) as Integer */
val Duration.microsecondsInt: Int get() = (this.milliseconds / MILLIS_PER_MICROSECOND).toInt()

/** Returns the total number of [seconds] for this [TimeSpan] */
val Duration.seconds: Double get() = this.milliseconds / MILLIS_PER_SECOND
/** Returns the total number of [minutes] for this [TimeSpan] (60 [seconds]) */
val Duration.minutes: Double get() = this.milliseconds / MILLIS_PER_MINUTE
/** Returns the total number of [hours] for this [TimeSpan] (3_600 [seconds]) */
val Duration.hours: Double get() = this.milliseconds / MILLIS_PER_HOUR
/** Returns the total number of [days] for this [TimeSpan] (86_400 [seconds]) */
val Duration.days: Double get() = this.milliseconds / MILLIS_PER_DAY
/** Returns the total number of [weeks] for this [TimeSpan] (604_800 [seconds]) */
val Duration.weeks: Double get() = this.milliseconds / MILLIS_PER_WEEK

/** Returns the total number of [milliseconds] as a [Long] */
val Duration.millisecondsLong: Long get() = milliseconds.toLong()
/** Returns the total number of [milliseconds] as an [Int] */
val Duration.millisecondsInt: Int get() = milliseconds.toInt()

//override fun Duration.compareTo(other: TimeSpan): Int = this.milliseconds.compareTo(other.milliseconds)

/** Return true if [Duration.NIL] */
//val Duration.isNil: Boolean get() = milliseconds.isNaN()
val Duration.isNil: Boolean get() = this == Duration.NIL

operator fun Duration.unaryMinus() = TimeSpan(-this.milliseconds)
operator fun Duration.unaryPlus() = TimeSpan(+this.milliseconds)

operator fun Duration.plus(other: TimeSpan): TimeSpan = TimeSpan(this.milliseconds + other.milliseconds)
operator fun Duration.plus(other: MonthSpan): DateTimeSpan = DateTimeSpan(other, this)
operator fun Duration.plus(other: DateTimeSpan): DateTimeSpan = DateTimeSpan(other.monthSpan, other.timeSpan + this)

operator fun Duration.minus(other: TimeSpan): TimeSpan = this + (-other)
operator fun Duration.minus(other: MonthSpan): DateTimeSpan = this + (-other)
operator fun Duration.minus(other: DateTimeSpan): DateTimeSpan = this + (-other)

operator fun Duration.times(scale: Int): TimeSpan = TimeSpan(this.milliseconds * scale)
operator fun Duration.times(scale: Float): TimeSpan = TimeSpan((this.milliseconds * scale))
operator fun Duration.times(scale: Double): TimeSpan = TimeSpan((this.milliseconds * scale))

operator fun Duration.div(scale: Int): TimeSpan = TimeSpan(this.milliseconds / scale)
operator fun Duration.div(scale: Float): TimeSpan = TimeSpan(this.milliseconds / scale)
operator fun Duration.div(scale: Double): TimeSpan = TimeSpan((this.milliseconds / scale))

operator fun Duration.div(other: TimeSpan): Float = (this.milliseconds / other.milliseconds).toFloat()
operator fun Duration.rem(other: TimeSpan): TimeSpan = (this.milliseconds % other.milliseconds).milliseconds
infix fun Duration.divFloat(other: TimeSpan): Float = (this.milliseconds / other.milliseconds).toFloat()
infix fun Duration.umod(other: TimeSpan): TimeSpan = (this.milliseconds umod other.milliseconds).milliseconds

fun Duration.toStringCompat(): String = "${milliseconds.niceStr}ms"

private const val MILLIS_PER_MICROSECOND = 1.0 / 1000.0
private const val MILLIS_PER_NANOSECOND = MILLIS_PER_MICROSECOND / 1000.0

/**
 * Zero time.
 */
val Duration.Companion.ZERO get() = TimeSpan(0.0)

/**
 * Represents an invalid Duration.
 * Useful to represent an alternative "null" time-lapse
 * avoiding the boxing of a nullable type.
 */
//val Duration.Companion.NIL get() = TimeSpan(Double.NaN)
val Duration.Companion.NIL get() = (Long.MAX_VALUE / 2 - 1).toDuration(DurationUnit.MILLISECONDS)

@PublishedApi
internal fun Duration.Companion.fromMilliseconds(ms: Double) = when (ms) {
    0.0 -> ZERO
    else -> TimeSpan(ms)
}

@PublishedApi internal fun Duration.Companion.fromNanoseconds(s: Double) = fromMilliseconds(s * MILLIS_PER_NANOSECOND)
@PublishedApi internal fun Duration.Companion.fromMicroseconds(s: Double) = fromMilliseconds(s * MILLIS_PER_MICROSECOND)
@PublishedApi internal fun Duration.Companion.fromSeconds(s: Double) = fromMilliseconds(s * MILLIS_PER_SECOND)
@PublishedApi internal fun Duration.Companion.fromMinutes(s: Double) = fromMilliseconds(s * MILLIS_PER_MINUTE)
@PublishedApi internal fun Duration.Companion.fromHours(s: Double) = fromMilliseconds(s * MILLIS_PER_HOUR)
@PublishedApi internal fun Duration.Companion.fromDays(s: Double) = fromMilliseconds(s * MILLIS_PER_DAY)
@PublishedApi internal fun Duration.Companion.fromWeeks(s: Double) = fromMilliseconds(s * MILLIS_PER_WEEK)

private val timeSteps = listOf(60, 60, 24)
private fun Duration.Companion.toTimeStringRaw(totalMilliseconds: Double, components: Int = 3): String {
    var timeUnit = floor(totalMilliseconds / 1000.0).toInt()

    val out = arrayListOf<String>()

    for (n in 0 until components) {
        if (n == components - 1) {
            out += timeUnit.padded(2)
            break
        }
        val step = timeSteps.getOrNull(n) ?: throw RuntimeException("Just supported ${timeSteps.size} steps")
        val cunit = timeUnit % step
        timeUnit /= step
        out += cunit.padded(2)
    }

    return out.reversed().joinToString(":")
}

@PublishedApi
internal fun Duration.Companion.toTimeString(totalMilliseconds: Double, components: Int = 3, addMilliseconds: Boolean = false): String {
    val milliseconds = (totalMilliseconds % 1000).toInt()
    val out = toTimeStringRaw(totalMilliseconds, components)
    return if (addMilliseconds) "$out.$milliseconds" else out
}

/**
 * Formats this [TimeSpan] into something like `12:30:40.100`.
 *
 * For 3 hour, 20 minutes and 15 seconds
 *
 * 1 [components] (seconds): 12015
 * 2 [components] (minutes): 200:15
 * 3 [components] (hours)  : 03:20:15
 * 4 [components] (days)   : 00:03:20:15
 *
 * With milliseconds would add decimals to the seconds part.
 */
fun Duration.toTimeString(components: Int = 3, addMilliseconds: Boolean = false): String =
    Duration.toTimeString(milliseconds, components, addMilliseconds)

fun Duration.roundMilliseconds(): TimeSpan = kotlin.math.round(milliseconds).milliseconds
fun max(a: TimeSpan, b: TimeSpan): TimeSpan = max(a.milliseconds, b.milliseconds).milliseconds
fun min(a: TimeSpan, b: TimeSpan): TimeSpan = min(a.milliseconds, b.milliseconds).milliseconds
fun Duration.clamp(min: TimeSpan, max: TimeSpan): TimeSpan = when {
    this < min -> min
    this > max -> max
    else -> this
}
inline fun Duration.coalesce(block: () -> TimeSpan): TimeSpan = if (this != Duration.NIL) this else block()
