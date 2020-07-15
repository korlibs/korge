package com.soywiz.korma.geom.range

class OpenRange<T : Comparable<T>>(val start: T, val endExclusive: T)

// @TODO: Would cause conflicts with Int until Int for example
//infix fun <T : Comparable<T>> T.until(other: T) = OpenRange(this, other)

operator fun <T : Comparable<T>> OpenRange<T>.contains(item: T) = item >= this.start && item < this.endExclusive
