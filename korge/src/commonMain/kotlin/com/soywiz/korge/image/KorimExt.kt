package com.soywiz.korge.image

import com.soywiz.korim.vector.CompoundShape
import com.soywiz.korim.vector.EmptyShape
import com.soywiz.korim.vector.FillShape
import com.soywiz.korim.vector.PolylineShape
import com.soywiz.korim.vector.Shape
import com.soywiz.korim.vector.StyledShape
import com.soywiz.korim.vector.TextShape

fun Shape.toExtString(): String = when (this) {
    is CompoundShape -> "CompoundShape(${this.components.joinToString(", ") { it.toExtString() }})"
    is FillShape -> "$this"
    is EmptyShape -> "$this"
    is PolylineShape -> "$this"
    is TextShape -> "$this"
    is StyledShape -> "$this"
    else -> TODO("$this")
}
