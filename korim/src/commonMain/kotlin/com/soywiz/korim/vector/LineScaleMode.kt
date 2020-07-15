package com.soywiz.korim.vector

enum class LineScaleMode(val hScale: Boolean, val vScale: Boolean) {
    NONE(false, false),
    HORIZONTAL(true, false),
    VERTICAL(false, true),
    NORMAL(true, true);
}
