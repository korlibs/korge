package korlibs.time.internal

import korlibs.time.DateTime
import korlibs.time.TimeSpan
import korlibs.time.hr.HRTimeSpan

internal expect object KlockInternal {
    val currentTime: Double
    val hrNow: HRTimeSpan
    fun localTimezoneOffsetMinutes(time: DateTime): TimeSpan
    fun sleep(time: HRTimeSpan)
}

expect interface Serializable