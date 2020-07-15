package com.soywiz.korge.image

import com.soywiz.korim.vector.*

fun Shape.toExtString(): String = when (this) {
    is CompoundShape -> "CompoundShape(${this.components.joinToString(", ") { it.toExtString() }})"
    is FillShape -> "$this"
    is EmptyShape -> "$this"
    is PolylineShape -> "$this"
    is TextShape -> "$this"
    is StyledShape -> "$this"
    else -> TODO("$this")
}
