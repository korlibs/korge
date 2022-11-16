package com.soywiz.korui.layout

internal typealias Length = com.soywiz.korma.length.Length
internal typealias LengthContext = com.soywiz.korma.length.Length.Context
internal typealias LengthExtensions = com.soywiz.korma.length.LengthExtensions
internal typealias Padding = com.soywiz.korma.length.PaddingLength
internal typealias Position = com.soywiz.korma.length.PositionLength
internal typealias Size = com.soywiz.korma.length.SizeLength

object MathEx {
    fun <T : Comparable<T>> min(a: T, b: T): T = if (a.compareTo(b) < 0) a else b
    fun <T : Comparable<T>> max(a: T, b: T): T = if (a.compareTo(b) > 0) a else b
}
