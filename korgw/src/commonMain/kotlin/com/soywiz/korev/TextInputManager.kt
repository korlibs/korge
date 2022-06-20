package com.soywiz.korev

interface TextInputPosition : Comparator<TextInputPosition> {
}

interface TextInputRange {
    val start: TextInputPosition
    val end: TextInputPosition
}

interface TextInputManager {
}
