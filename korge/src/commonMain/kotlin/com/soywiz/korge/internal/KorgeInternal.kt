package com.soywiz.korge.internal

/** A mechanism to annotate Korge internal properties and methods that were left open in KorGE 1.0 so they can be marked as internal in KorGE 2.0 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class KorgeInternal
