package com.soywiz.korio.util

fun Char.isWhitespaceFast(): Boolean = this == ' ' || this == '\t' || this == '\r' || this == '\n'
fun Char.isDigit(): Boolean = this in '0'..'9'
fun Char.isLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'
fun Char.isLetterOrDigit(): Boolean = isLetter() || isDigit()
fun Char.isLetterOrUnderscore(): Boolean = this.isLetter() || this == '_' || this == '$'
fun Char.isLetterDigitOrUnderscore(): Boolean = this.isLetterOrDigit() || this == '_' || this == '$'
fun Char.isLetterOrDigitOrDollar(): Boolean = this.isLetterOrDigit() || this == '$'
val Char.isNumeric: Boolean get() = this.isDigit() || this == '.' || this == 'e' || this == '-'
fun Char.isPrintable(): Boolean = this in '\u0020'..'\u007e' || this in '\u00a1'..'\u00ff'
val Char.isPossibleFloatChar get() = (this >= '0' && this <= '9') || (this == '+') || (this == '-') || (this == 'e') || (this == 'E') || (this == '.')
