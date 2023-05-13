package korlibs.time.darwin

import korlibs.time.*

fun DateTime.cfAbsoluteTime(): Double = (this - DateTime(2001, Month.January, 1, 0, 0, 0, 0)).seconds

fun DateTime.Companion.fromCFAbsoluteTime(cfAbsoluteTime: Double): DateTime =
    DateTime(2001, Month.January, 1, 0, 0, 0, 0) + cfAbsoluteTime.seconds
