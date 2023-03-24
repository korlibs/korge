package korlibs.korge.time

import korlibs.time.TimeSpan
import korlibs.time.milliseconds
import korlibs.math.interpolation.*

fun Ratio.interpolate(a: TimeSpan, b: TimeSpan): TimeSpan = this.interpolate(a.milliseconds, b.milliseconds).milliseconds