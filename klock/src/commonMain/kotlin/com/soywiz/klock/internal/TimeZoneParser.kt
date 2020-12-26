package com.soywiz.klock.internal

import com.soywiz.klock.TimeSpan
import com.soywiz.klock.TimezoneNames
import com.soywiz.klock.hours
import com.soywiz.klock.minutes

internal fun MicroStrReader.readTimeZoneOffset(tzNames: TimezoneNames = TimezoneNames.DEFAULT): TimeSpan? {
    val reader = this
    for ((name, offset) in tzNames.namesToOffsets) {
        if (name == "GMT" || name == "UTC") continue
        if (reader.tryRead(name)) return offset
    }
    if (reader.tryRead('Z')) return 0.minutes
    var sign = +1
    reader.tryRead("GMT")
    reader.tryRead("UTC")
    if (reader.tryRead("+")) sign = +1
    if (reader.tryRead("-")) sign = -1
    val part = reader.readRemaining().replace(":", "")
    val hours = part.substr(0, 2).padStart(2, '0').toIntOrNull() ?: return null
    val minutes = part.substr(2, 2).padStart(2, '0').toIntOrNull() ?: return null
    val roffset = hours.hours + minutes.minutes
    return if (sign > 0) +roffset else -roffset
}
