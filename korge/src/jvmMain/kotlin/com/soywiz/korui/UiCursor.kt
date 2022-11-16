package com.soywiz.korui

internal interface UiCursor {
}

internal enum class UiStandardCursor : UiCursor {
    DEFAULT, CROSSHAIR, TEXT, HAND, MOVE, WAIT,
    RESIZE_EAST, RESIZE_WEST, RESIZE_SOUTH, RESIZE_NORTH,
    RESIZE_NORTH_EAST, RESIZE_NORTH_WEST, RESIZE_SOUTH_EAST, RESIZE_SOUTH_WEST;
}
