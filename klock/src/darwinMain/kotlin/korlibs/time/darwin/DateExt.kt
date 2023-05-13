package korlibs.time.darwin

import korlibs.time.*
import platform.Foundation.*

fun DateTime.cfAbsoluteTime(): Double = (this - DateTime(2001, Month.January, 1, 0, 0, 0, 0)).seconds

fun DateTime.Companion.fromCFAbsoluteTime(cfAbsoluteTime: Double): DateTime =
    DateTime(2001, Month.January, 1, 0, 0, 0, 0) + cfAbsoluteTime.seconds

fun NSDate.toDateTime(): DateTime = DateTime.fromCFAbsoluteTime(this.timeIntervalSinceReferenceDate)
fun DateTime.toNSDate(): NSDate = NSDate(cfAbsoluteTime())
