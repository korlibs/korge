package korlibs.time.darwin

import korlibs.time.*
import platform.Foundation.*

private val APPLE_REFERENCE_NSDATE = DateTime(2001, Month.January, 1, 0, 0, 0, 0)

val DateTime.Companion.APPLE_REFERENCE_DATE: DateTime get() = APPLE_REFERENCE_NSDATE

fun DateTime.cfAbsoluteTime(): Double = (this - APPLE_REFERENCE_NSDATE).seconds

fun DateTime.Companion.fromCFAbsoluteTime(cfAbsoluteTime: Double): DateTime =
    APPLE_REFERENCE_NSDATE + cfAbsoluteTime.seconds

fun NSDate.toDateTime(): DateTime = DateTime.fromCFAbsoluteTime(this.timeIntervalSinceReferenceDate)
fun DateTime.toNSDate(): NSDate = NSDate(cfAbsoluteTime())
