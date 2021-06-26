package com.soywiz.korge.ui

enum class UIDirection(val index: Int) {
    HORIZONTAL(0), VERTICAL(1);

    val isHorizontal get() = this == HORIZONTAL
    val isVertical get() = this == VERTICAL
}

