package korlibs.korge.time

import korlibs.time.TimeSpan
import korlibs.time.milliseconds
import korlibs.math.interpolation.*
import kotlin.time.*

fun Ratio.interpolate(a: Duration, b: Duration): Duration = this.interpolate(a.milliseconds, b.milliseconds).milliseconds
