@file:Suppress("ClassName", "unused")

package com.soywiz.korgw.sdl2.jna.enums

enum class SDL_WindowEventID {
    NONE,
    SHOWN,
    HIDDEN,
    EXPOSED,
    MOVED,
    RESIZED,
    SIZE_CHANGED,
    MINIMIZED,
    MAXIMIZED,
    RESTORED,
    ENTER,
    LEAVE,
    FOCUS_GAINED,
    FOCUS_LOST,
    CLOSE,
    TAKE_FOCUS,
    HIT_TEST;

    companion object {
        val lookup = mutableMapOf<Int, SDL_WindowEventID>()

        init {
            values().forEach { lookup[it.ordinal] = it }
        }

        fun fromInt(value: Int) = lookup[value] ?: NONE
    }
}
