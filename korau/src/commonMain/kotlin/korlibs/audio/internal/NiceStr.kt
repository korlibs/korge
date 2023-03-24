package korlibs.audio.internal

import kotlin.math.floor

internal val Double.niceStr: String get() = if (floor(this) == this) "${this.toInt()}" else "$this"